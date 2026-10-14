package com.shreya.githubsearcher.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

/** The parts of GitHub's {@code GET /search/repositories} response that we use. */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GitHubSearchResponse(
        long totalCount,
        boolean incompleteResults,
        List<GitHubRepositoryItem> items
) {

    public List<GitHubRepositoryItem> itemsOrEmpty() {
        return items == null ? List.of() : items;
    }
}
