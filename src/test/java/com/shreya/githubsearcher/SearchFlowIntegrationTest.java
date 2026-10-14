package com.shreya.githubsearcher;

import com.shreya.githubsearcher.client.GitHubClient;
import com.shreya.githubsearcher.client.dto.GitHubSearchResponse;
import com.shreya.githubsearcher.repository.GitHubRepoRepository;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Whole application wired together on a real PostgreSQL; only the call to GitHub is stubbed.
 * Proves that searching twice updates rows instead of duplicating them, and that the
 * GET endpoint reads back what the search saved.
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY, type = POSTGRES)
class SearchFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GitHubRepoRepository repository;

    @MockitoBean
    private GitHubClient gitHubClient;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void searchSavesResultsAndRepeatSearchUpdatesThemInPlace() throws Exception {
        when(gitHubClient.searchRepositories(anyString(), any(), any())).thenReturn(new GitHubSearchResponse(2, false, List.of(
                TestData.item(1L, "spring-boot-a", "Java", 450),
                TestData.item(2L, "spring-boot-b", "Java", 120))));

        search();
        assertThat(repository.count()).isEqualTo(2);

        // Same repositories come back from GitHub with new star counts
        when(gitHubClient.searchRepositories(anyString(), any(), any())).thenReturn(new GitHubSearchResponse(2, false, List.of(
                TestData.item(1L, "spring-boot-a", "Java", 500),
                TestData.item(2L, "spring-boot-b", "Java", 130))));

        search();
        assertThat(repository.count()).isEqualTo(2);
        assertThat(repository.findById(1L).orElseThrow().getStars()).isEqualTo(500);
    }

    @Test
    void storedResultsCanBeFilteredAndSorted() throws Exception {
        when(gitHubClient.searchRepositories(anyString(), any(), any())).thenReturn(new GitHubSearchResponse(3, false, List.of(
                TestData.item(1L, "java-big", "Java", 800),
                TestData.item(2L, "java-small", "Java", 50),
                TestData.item(3L, "py-mid", "Python", 400))));
        search();

        mockMvc.perform(get("/api/github/repositories")
                        .param("language", "java")
                        .param("minStars", "100")
                        .param("sort", "stars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repositories", hasSize(1)))
                .andExpect(jsonPath("$.repositories[0].name").value("java-big"));

        mockMvc.perform(get("/api/github/repositories").param("sort", "stars"))
                .andExpect(jsonPath("$.repositories[*].name").value(contains("java-big", "py-mid", "java-small")));
    }

    @Test
    void invalidSortOnGetIsRejected() throws Exception {
        mockMvc.perform(get("/api/github/repositories").param("sort", "name"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid sort 'name'. Allowed values: stars, forks, updated"));
    }

    @Test
    void swaggerDocsAndHealthAreServed() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/github/search']").exists())
                .andExpect(jsonPath("$.paths['/api/github/repositories']").exists());

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/"))
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
    }

    private void search() throws Exception {
        mockMvc.perform(post("/api/github/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"spring boot\", \"language\": \"Java\", \"sort\": \"stars\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Repositories fetched and saved successfully"));
    }
}
