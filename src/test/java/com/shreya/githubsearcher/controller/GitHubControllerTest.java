package com.shreya.githubsearcher.controller;

import com.shreya.githubsearcher.dto.RepositoryListResponse;
import com.shreya.githubsearcher.dto.RepositoryResponse;
import com.shreya.githubsearcher.dto.SearchRequest;
import com.shreya.githubsearcher.dto.SearchResponse;
import com.shreya.githubsearcher.exception.GitHubApiException;
import com.shreya.githubsearcher.exception.GitHubRateLimitException;
import com.shreya.githubsearcher.exception.GitHubUnavailableException;
import com.shreya.githubsearcher.exception.InvalidRequestException;
import com.shreya.githubsearcher.service.GitHubSearchService;
import com.shreya.githubsearcher.service.RepositoryQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GitHubController.class)
class GitHubControllerTest {

    private static final RepositoryResponse REPO = new RepositoryResponse(123456L, "spring-boot-example",
            "An example repository for Spring Boot", "user123", "Java", 450, 120,
            Instant.parse("2024-01-01T12:00:00Z"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GitHubSearchService searchService;

    @MockitoBean
    private RepositoryQueryService queryService;

    // ---- POST /api/github/search ----

    @Test
    void searchReturnsSavedRepositoriesInSpecifiedShape() throws Exception {
        when(searchService.search(new SearchRequest("spring boot", "Java", "stars")))
                .thenReturn(new SearchResponse("Repositories fetched and saved successfully", List.of(REPO)));

        mockMvc.perform(post("/api/github/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query": "spring boot", "language": "Java", "sort": "stars"}"""))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Repositories fetched and saved successfully"))
                .andExpect(jsonPath("$.repositories", hasSize(1)))
                .andExpect(jsonPath("$.repositories[0].id").value(123456))
                .andExpect(jsonPath("$.repositories[0].name").value("spring-boot-example"))
                .andExpect(jsonPath("$.repositories[0].description").value("An example repository for Spring Boot"))
                .andExpect(jsonPath("$.repositories[0].owner").value("user123"))
                .andExpect(jsonPath("$.repositories[0].language").value("Java"))
                .andExpect(jsonPath("$.repositories[0].stars").value(450))
                .andExpect(jsonPath("$.repositories[0].forks").value(120))
                .andExpect(jsonPath("$.repositories[0].lastUpdated").value("2024-01-01T12:00:00Z"));
    }

    @Test
    void searchWithOnlyQueryIsAccepted() throws Exception {
        when(searchService.search(any())).thenReturn(new SearchResponse("No repositories found matching the search criteria", List.of()));

        mockMvc.perform(post("/api/github/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"zzzz\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositories", hasSize(0)));

        verify(searchService).search(new SearchRequest("zzzz", null, null));
    }

    @Test
    void searchRejectsBlankQuery() throws Exception {
        mockMvc.perform(post("/api/github/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"   \", \"language\": \"Java\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("query"))
                .andExpect(jsonPath("$.errors[0].message").value("query must not be blank"));

        verifyNoInteractions(searchService);
    }

    @Test
    void searchRejectsMissingQuery() throws Exception {
        mockMvc.perform(post("/api/github/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\": \"Java\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("query"));
    }

    @Test
    void searchRejectsLanguageWithQualifierInjection() throws Exception {
        mockMvc.perform(post("/api/github/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"x\", \"language\": \"Java stars:>1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("language"));
    }

    @Test
    void searchRejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/github/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void searchRejectsNonJsonContentType() throws Exception {
        mockMvc.perform(post("/api/github/search")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("spring boot"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void searchReportsInvalidSortAs400() throws Exception {
        when(searchService.search(any())).thenThrow(
                new InvalidRequestException("Invalid sort 'popularity'. Allowed values: stars, forks, updated"));

        mockMvc.perform(post("/api/github/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"x\", \"sort\": \"popularity\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail", containsString("Allowed values")));
    }

    @Test
    void searchReportsRateLimitAs429WithRetryAfter() throws Exception {
        when(searchService.search(any())).thenThrow(
                new GitHubRateLimitException("GitHub API rate limit exceeded. Please retry later.",
                        Instant.now().plusSeconds(120)));

        mockMvc.perform(post("/api/github/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"x\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.title").value("GitHub rate limit exceeded"))
                .andExpect(jsonPath("$.retryAfterSeconds").isNumber())
                .andExpect(jsonPath("$.resetAt").exists());
    }

    @Test
    void searchReportsRateLimitWithoutResetTime() throws Exception {
        when(searchService.search(any())).thenThrow(new GitHubRateLimitException("limited", null));

        mockMvc.perform(post("/api/github/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"x\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().doesNotExist("Retry-After"));
    }

    @Test
    void searchReportsGitHubErrorAs502() throws Exception {
        when(searchService.search(any())).thenThrow(new GitHubApiException("GitHub API error (status 500)", 500));

        mockMvc.perform(post("/api/github/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"x\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("GitHub API error"));
    }

    @Test
    void searchReportsGitHubOutageAs503() throws Exception {
        when(searchService.search(any())).thenThrow(new GitHubUnavailableException("down", null));

        mockMvc.perform(post("/api/github/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"x\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("GitHub unavailable"));
    }

    @Test
    void unexpectedErrorsBecome500WithoutLeakingDetails() throws Exception {
        when(searchService.search(any())).thenThrow(new IllegalStateException("secret internal detail"));

        mockMvc.perform(post("/api/github/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"x\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred. Please try again later."));
    }

    // ---- GET /api/github/repositories ----

    @Test
    void getRepositoriesPassesFiltersToService() throws Exception {
        when(queryService.find("Java", 100, "stars", 0, 50)).thenReturn(new RepositoryListResponse(List.of(REPO)));

        mockMvc.perform(get("/api/github/repositories")
                        .param("language", "Java")
                        .param("minStars", "100")
                        .param("sort", "stars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositories", hasSize(1)))
                .andExpect(jsonPath("$.repositories[0].id").value(123456))
                .andExpect(jsonPath("$.repositories[0].stars").value(450));
    }

    @Test
    void getRepositoriesWorksWithoutAnyParameters() throws Exception {
        when(queryService.find(isNull(), isNull(), isNull(), eq(0), eq(50))).thenReturn(new RepositoryListResponse(List.of()));

        mockMvc.perform(get("/api/github/repositories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositories", hasSize(0)));
    }

    @Test
    void getRepositoriesAcceptsPaging() throws Exception {
        when(queryService.find(isNull(), isNull(), isNull(), eq(3), eq(20))).thenReturn(new RepositoryListResponse(List.of()));

        mockMvc.perform(get("/api/github/repositories").param("page", "3").param("size", "20"))
                .andExpect(status().isOk());
    }

    @Test
    void getRepositoriesRejectsNegativeMinStars() throws Exception {
        mockMvc.perform(get("/api/github/repositories").param("minStars", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("minStars must be 0 or greater")));

        verifyNoInteractions(queryService);
    }

    @Test
    void getRepositoriesRejectsOversizedPage() throws Exception {
        mockMvc.perform(get("/api/github/repositories").param("size", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("size must be between 1 and 100")));
    }

    @Test
    void getRepositoriesRejectsNonNumericMinStars() throws Exception {
        mockMvc.perform(get("/api/github/repositories").param("minStars", "lots"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(queryService);
    }

    @Test
    void getRepositoriesReportsInvalidSortAs400() throws Exception {
        when(queryService.find(any(), any(), eq("name"), anyInt(), anyInt()))
                .thenThrow(new InvalidRequestException("Invalid sort 'name'. Allowed values: stars, forks, updated"));

        mockMvc.perform(get("/api/github/repositories").param("sort", "name"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Invalid sort 'name'")));
    }

    @Test
    void unsupportedMethodIs405() throws Exception {
        mockMvc.perform(post("/api/github/repositories"))
                .andExpect(status().isMethodNotAllowed());
    }
}
