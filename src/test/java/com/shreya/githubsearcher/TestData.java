package com.shreya.githubsearcher;

import com.shreya.githubsearcher.client.dto.GitHubRepositoryItem;
import com.shreya.githubsearcher.entity.GitHubRepo;

import java.time.Instant;

/** Builders for test objects, so each test only spells out the fields it cares about. */
public final class TestData {

    public static final Instant FETCHED_AT = Instant.parse("2026-10-14T10:00:00Z");

    private TestData() {
    }

    public static GitHubRepositoryItem item(long id, String name, String language, int stars) {
        return new GitHubRepositoryItem(
                id, name, "user123/" + name, "Description of " + name,
                new GitHubRepositoryItem.Owner("user123"), language,
                stars, stars / 4, stars, 3,
                "https://github.com/user123/" + name,
                Instant.parse("2020-05-01T08:00:00Z"),
                Instant.parse("2024-01-01T12:00:00Z"),
                Instant.parse("2024-01-01T11:00:00Z"));
    }

    public static GitHubRepo repo(long id, String name, String language, int stars, int forks, Instant lastUpdated) {
        return GitHubRepo.builder()
                .id(id)
                .name(name)
                .fullName("user123/" + name)
                .description("Description of " + name)
                .ownerName("user123")
                .language(language)
                .stars(stars)
                .forks(forks)
                .watchers(stars)
                .openIssues(1)
                .htmlUrl("https://github.com/user123/" + name)
                .createdAt(Instant.parse("2020-05-01T08:00:00Z"))
                .lastUpdated(lastUpdated)
                .pushedAt(lastUpdated)
                .fetchedAt(FETCHED_AT)
                .build();
    }
}
