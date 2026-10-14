package com.shreya.githubsearcher.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shreya.githubsearcher.client.dto.GitHubRepositoryItem;
import com.shreya.githubsearcher.client.dto.GitHubSearchResponse;
import com.shreya.githubsearcher.config.GitHubProperties;
import com.shreya.githubsearcher.domain.SortField;
import com.shreya.githubsearcher.exception.GitHubApiException;
import com.shreya.githubsearcher.exception.GitHubRateLimitException;
import com.shreya.githubsearcher.exception.GitHubUnavailableException;
import com.shreya.githubsearcher.exception.InvalidRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.startsWith;

class GitHubClientTest {

    private static final String BASE_URL = "https://api.github.com";

    private static final String SEARCH_RESULT = """
            {
              "total_count": 1,
              "incomplete_results": false,
              "items": [
                {
                  "id": 123456,
                  "name": "spring-boot-example",
                  "full_name": "user123/spring-boot-example",
                  "description": "An example repository for Spring Boot",
                  "owner": { "login": "user123", "id": 99 },
                  "language": "Java",
                  "stargazers_count": 450,
                  "forks_count": 120,
                  "watchers_count": 450,
                  "open_issues_count": 7,
                  "html_url": "https://github.com/user123/spring-boot-example",
                  "created_at": "2020-05-01T08:00:00Z",
                  "updated_at": "2024-01-01T12:00:00Z",
                  "pushed_at": "2023-12-31T09:30:00Z",
                  "some_field_we_ignore": true
                }
              ]
            }
            """;

    private MockRestServiceServer server;
    private GitHubClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        GitHubProperties properties = new GitHubProperties(BASE_URL, null, 30, Duration.ofSeconds(1), Duration.ofSeconds(1));
        client = new GitHubClient(builder.build(), properties, new ObjectMapper());
    }

    @Test
    void sendsSearchQueryAndParsesResults() {
        server.expect(requestTo(startsWith(BASE_URL + "/search/repositories")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(decodedQueryParam("q", "spring boot in:name language:\"Java\""))
                .andExpect(queryParam("sort", "stars"))
                .andExpect(queryParam("order", "desc"))
                .andExpect(queryParam("per_page", "30"))
                .andRespond(withSuccess(SEARCH_RESULT, MediaType.APPLICATION_JSON));

        GitHubSearchResponse response = client.searchRepositories("spring boot", "Java", SortField.STARS);

        server.verify();
        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.items()).hasSize(1);
        GitHubRepositoryItem item = response.items().getFirst();
        assertThat(item.id()).isEqualTo(123456L);
        assertThat(item.fullName()).isEqualTo("user123/spring-boot-example");
        assertThat(item.owner().login()).isEqualTo("user123");
        assertThat(item.stargazersCount()).isEqualTo(450);
        assertThat(item.forksCount()).isEqualTo(120);
        assertThat(item.updatedAt()).isEqualTo(Instant.parse("2024-01-01T12:00:00Z"));
    }

    @Test
    void encodesPlusSignsSoGitHubDoesNotReadThemAsSpaces() {
        server.expect(request -> assertThat(request.getURI().getRawQuery()).contains("%2B%2B"))
                .andExpect(decodedQueryParam("q", "engine in:name language:\"C++\""))
                .andRespond(withSuccess(SEARCH_RESULT, MediaType.APPLICATION_JSON));

        client.searchRepositories("engine", "C++", SortField.UPDATED);

        server.verify();
    }

    @Test
    void passesTheChosenSortToGitHub() {
        server.expect(queryParam("sort", "forks"))
                .andRespond(withSuccess(SEARCH_RESULT, MediaType.APPLICATION_JSON));

        client.searchRepositories("cli", null, SortField.FORKS);

        server.verify();
    }

    @Test
    void returnsEmptyResultsWhenNothingMatches() {
        server.expect(requestTo(startsWith(BASE_URL)))
                .andRespond(withSuccess("""
                        {"total_count": 0, "incomplete_results": false, "items": []}""", MediaType.APPLICATION_JSON));

        GitHubSearchResponse response = client.searchRepositories("zzzz-nothing", null, SortField.STARS);

        assertThat(response.itemsOrEmpty()).isEmpty();
    }

    @Test
    void treatsMissingItemsAsEmpty() {
        server.expect(requestTo(startsWith(BASE_URL)))
                .andRespond(withSuccess("{\"total_count\": 0}", MediaType.APPLICATION_JSON));

        assertThat(client.searchRepositories("x", null, SortField.STARS).itemsOrEmpty()).isEmpty();
    }

    @Test
    void mapsPrimaryRateLimitToRateLimitException() {
        long resetEpoch = Instant.now().plusSeconds(60).getEpochSecond();
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-ratelimit-remaining", "0");
        headers.set("x-ratelimit-reset", String.valueOf(resetEpoch));
        server.expect(requestTo(startsWith(BASE_URL)))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).headers(headers)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\": \"API rate limit exceeded\"}"));

        assertThatThrownBy(() -> client.searchRepositories("java", null, SortField.STARS))
                .isInstanceOfSatisfying(GitHubRateLimitException.class, e ->
                        assertThat(e.getResetAt()).contains(Instant.ofEpochSecond(resetEpoch)));
    }

    @Test
    void mapsSecondaryRateLimitToRateLimitException() {
        server.expect(requestTo(startsWith(BASE_URL)))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).header(HttpHeaders.RETRY_AFTER, "30"));

        assertThatThrownBy(() -> client.searchRepositories("java", null, SortField.STARS))
                .isInstanceOfSatisfying(GitHubRateLimitException.class, e ->
                        assertThat(e.getResetAt()).isPresent());
    }

    @Test
    void maps429ToRateLimitException() {
        server.expect(requestTo(startsWith(BASE_URL)))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.searchRepositories("java", null, SortField.STARS))
                .isInstanceOfSatisfying(GitHubRateLimitException.class, e ->
                        assertThat(e.getResetAt()).isEmpty());
    }

    @Test
    void mapsOtherForbiddenResponsesToApiException() {
        server.expect(requestTo(startsWith(BASE_URL)))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).header("x-ratelimit-remaining", "12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\": \"Resource not accessible\"}"));

        assertThatThrownBy(() -> client.searchRepositories("java", null, SortField.STARS))
                .isInstanceOfSatisfying(GitHubApiException.class, e -> {
                    assertThat(e.getUpstreamStatus()).isEqualTo(403);
                    assertThat(e.getMessage()).contains("Resource not accessible");
                });
    }

    @Test
    void mapsRejectedQueryToInvalidRequest() {
        server.expect(requestTo(startsWith(BASE_URL)))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\": \"Validation Failed\"}"));

        assertThatThrownBy(() -> client.searchRepositories("java", null, SortField.STARS))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Validation Failed");
    }

    @Test
    void mapsServerErrorToApiException() {
        server.expect(requestTo(startsWith(BASE_URL))).andRespond(withServerError().body("not json"));

        assertThatThrownBy(() -> client.searchRepositories("java", null, SortField.STARS))
                .isInstanceOfSatisfying(GitHubApiException.class, e -> {
                    assertThat(e.getUpstreamStatus()).isEqualTo(500);
                    assertThat(e.getMessage()).contains("no details provided");
                });
    }

    @Test
    void mapsTimeoutToUnavailable() {
        server.expect(requestTo(startsWith(BASE_URL)))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> client.searchRepositories("java", null, SortField.STARS))
                .isInstanceOf(GitHubUnavailableException.class);
    }

    @Test
    void mapsConnectionFailureToUnavailable() {
        server.expect(requestTo(startsWith(BASE_URL)))
                .andRespond(withException(new IOException("Connection refused")));

        assertThatThrownBy(() -> client.searchRepositories("java", null, SortField.STARS))
                .isInstanceOf(GitHubUnavailableException.class);
    }

    @Test
    void mapsMalformedJsonToApiException() {
        server.expect(requestTo(startsWith(BASE_URL)))
                .andRespond(withSuccess("{ this is not json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.searchRepositories("java", null, SortField.STARS))
                .isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("could not be read");
    }

    @Test
    void mapsEmptyBodyToApiException() {
        server.expect(requestTo(startsWith(BASE_URL)))
                .andRespond(withSuccess());

        assertThatThrownBy(() -> client.searchRepositories("java", null, SortField.STARS))
                .isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("empty response");
    }

    @Test
    void resolveResetTimeIgnoresUnparseableHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-ratelimit-reset", "soon");

        assertThat(GitHubClient.resolveResetTime(headers)).isNull();
    }

    /** Compares a query parameter after URL-decoding it, so expectations read like the real query. */
    private static RequestMatcher decodedQueryParam(String name, String expected) {
        return request -> {
            String raw = UriComponentsBuilder.fromUri(request.getURI()).build(true).getQueryParams().getFirst(name);
            assertThat(raw).as("query parameter '%s'", name).isNotNull();
            assertThat(URLDecoder.decode(raw, StandardCharsets.UTF_8)).isEqualTo(expected);
        };
    }
}
