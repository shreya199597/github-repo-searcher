package com.shreya.githubsearcher.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shreya.githubsearcher.client.dto.GitHubSearchResponse;
import com.shreya.githubsearcher.config.GitHubProperties;
import com.shreya.githubsearcher.domain.SortField;
import com.shreya.githubsearcher.exception.GitHubApiException;
import com.shreya.githubsearcher.exception.GitHubRateLimitException;
import com.shreya.githubsearcher.exception.GitHubUnavailableException;
import com.shreya.githubsearcher.exception.InvalidRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Map;

/**
 * Calls GitHub's repository search endpoint and turns its failure modes into our exceptions:
 * rate limits, rejected queries, server errors, timeouts and unreadable responses.
 */
@Component
public class GitHubClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubClient.class);
    static final String SEARCH_PATH = "/search/repositories";

    private final RestClient restClient;
    private final GitHubProperties properties;
    private final ObjectMapper objectMapper;

    public GitHubClient(RestClient gitHubRestClient, GitHubProperties properties, ObjectMapper objectMapper) {
        this.restClient = gitHubRestClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public GitHubSearchResponse searchRepositories(String query, String language, SortField sort) {
        String q = GitHubQueryBuilder.build(query, language);
        log.info("Searching GitHub: q='{}', sort={}", q, sort.value());

        GitHubSearchResponse response;
        try {
            response = restClient.get()
                    // q goes in as a URI variable so it is fully encoded ("C++" must not become "C  ")
                    .uri(uri -> uri.path(SEARCH_PATH)
                            .queryParam("q", "{q}")
                            .queryParam("sort", sort.value())
                            .queryParam("order", "desc")
                            .queryParam("per_page", properties.perPage())
                            .build(Map.of("q", q)))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, errorResponse) -> {
                        throw translateError(errorResponse);
                    })
                    .body(GitHubSearchResponse.class);
        } catch (ResourceAccessException e) {
            throw new GitHubUnavailableException("GitHub could not be reached or did not respond in time", e);
        } catch (RestClientException e) {
            throw new GitHubApiException("GitHub returned a response that could not be read", e);
        }

        if (response == null) {
            throw new GitHubApiException("GitHub returned an empty response body", HttpStatus.OK.value());
        }
        if (response.incompleteResults()) {
            log.warn("GitHub reported incomplete results for q='{}' (search timed out on GitHub's side)", q);
        }
        return response;
    }

    private RuntimeException translateError(ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        HttpHeaders headers = response.getHeaders();
        String gitHubMessage = readMessage(response);
        log.warn("GitHub search failed with status {}: {}", status, gitHubMessage);

        if (isRateLimited(status, headers)) {
            return new GitHubRateLimitException(
                    "GitHub API rate limit exceeded. Please retry later.", resolveResetTime(headers));
        }
        if (status == HttpStatus.UNPROCESSABLE_ENTITY.value()) {
            return new InvalidRequestException("GitHub rejected the search query: " + gitHubMessage);
        }
        return new GitHubApiException("GitHub API error (status %d): %s".formatted(status, gitHubMessage), status);
    }

    /**
     * GitHub signals rate limiting with 429, or with 403 plus either an exhausted
     * {@code x-ratelimit-remaining} (primary limit) or a {@code retry-after} header (secondary limit).
     */
    static boolean isRateLimited(int status, HttpHeaders headers) {
        if (status == HttpStatus.TOO_MANY_REQUESTS.value()) {
            return true;
        }
        return status == HttpStatus.FORBIDDEN.value()
                && ("0".equals(headers.getFirst("x-ratelimit-remaining"))
                || headers.getFirst(HttpHeaders.RETRY_AFTER) != null);
    }

    static Instant resolveResetTime(HttpHeaders headers) {
        try {
            String retryAfter = headers.getFirst(HttpHeaders.RETRY_AFTER);
            if (retryAfter != null) {
                return Instant.now().plusSeconds(Long.parseLong(retryAfter.trim()));
            }
            String reset = headers.getFirst("x-ratelimit-reset");
            if (reset != null) {
                return Instant.ofEpochSecond(Long.parseLong(reset.trim()));
            }
        } catch (NumberFormatException e) {
            log.debug("Could not parse GitHub rate limit headers", e);
        }
        return null;
    }

    private String readMessage(ClientHttpResponse response) {
        try (InputStream body = response.getBody()) {
            JsonNode json = objectMapper.readTree(body);
            if (json != null && json.hasNonNull("message")) {
                return json.get("message").asText();
            }
        } catch (IOException e) {
            log.debug("GitHub error body was not JSON", e);
        }
        return "no details provided";
    }
}
