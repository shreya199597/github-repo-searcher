package com.shreya.githubsearcher.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;

/** One repository from GitHub's search results. */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GitHubRepositoryItem(
        Long id,
        String name,
        String fullName,
        String description,
        Owner owner,
        String language,
        int stargazersCount,
        int forksCount,
        int watchersCount,
        int openIssuesCount,
        String htmlUrl,
        Instant createdAt,
        Instant updatedAt,
        Instant pushedAt
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Owner(String login) {
    }
}
