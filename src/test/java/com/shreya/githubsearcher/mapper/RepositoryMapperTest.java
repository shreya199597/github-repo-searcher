package com.shreya.githubsearcher.mapper;

import com.shreya.githubsearcher.TestData;
import com.shreya.githubsearcher.client.dto.GitHubRepositoryItem;
import com.shreya.githubsearcher.dto.RepositoryResponse;
import com.shreya.githubsearcher.entity.GitHubRepo;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryMapperTest {

    @Test
    void mapsGitHubItemToEntity() {
        GitHubRepositoryItem item = TestData.item(123456L, "spring-boot-example", "Java", 450);

        GitHubRepo repo = RepositoryMapper.toEntity(item, TestData.FETCHED_AT);

        assertThat(repo.getId()).isEqualTo(123456L);
        assertThat(repo.getName()).isEqualTo("spring-boot-example");
        assertThat(repo.getFullName()).isEqualTo("user123/spring-boot-example");
        assertThat(repo.getDescription()).isEqualTo("Description of spring-boot-example");
        assertThat(repo.getOwnerName()).isEqualTo("user123");
        assertThat(repo.getLanguage()).isEqualTo("Java");
        assertThat(repo.getStars()).isEqualTo(450);
        assertThat(repo.getForks()).isEqualTo(112);
        assertThat(repo.getWatchers()).isEqualTo(450);
        assertThat(repo.getOpenIssues()).isEqualTo(3);
        assertThat(repo.getHtmlUrl()).isEqualTo("https://github.com/user123/spring-boot-example");
        assertThat(repo.getLastUpdated()).isEqualTo(Instant.parse("2024-01-01T12:00:00Z"));
        assertThat(repo.getFetchedAt()).isEqualTo(TestData.FETCHED_AT);
    }

    @Test
    void derivesFullNameWhenGitHubOmitsIt() {
        GitHubRepositoryItem item = new GitHubRepositoryItem(1L, "repo", null, null,
                new GitHubRepositoryItem.Owner("octocat"), null, 0, 0, 0, 0, null, null, null, null);

        assertThat(RepositoryMapper.toEntity(item, TestData.FETCHED_AT).getFullName()).isEqualTo("octocat/repo");
    }

    @Test
    void mapsEntityToApiResponse() {
        Instant updated = Instant.parse("2024-01-01T12:00:00Z");
        GitHubRepo repo = TestData.repo(123456L, "spring-boot-example", "Java", 450, 120, updated);

        RepositoryResponse response = RepositoryMapper.toResponse(repo);

        assertThat(response).isEqualTo(new RepositoryResponse(123456L, "spring-boot-example",
                "Description of spring-boot-example", "user123", "Java", 450, 120, updated));
    }
}
