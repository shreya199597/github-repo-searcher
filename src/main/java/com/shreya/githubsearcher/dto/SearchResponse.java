package com.shreya.githubsearcher.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Result of a GitHub search; the repositories have been saved to the database")
public record SearchResponse(
        @Schema(example = "Repositories fetched and saved successfully") String message,
        List<RepositoryResponse> repositories
) {
}
