package com.shreya.githubsearcher.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "A GitHub repository")
public record RepositoryResponse(
        @Schema(description = "GitHub repository id", example = "123456") Long id,
        @Schema(example = "spring-boot-example") String name,
        @Schema(example = "An example repository for Spring Boot") String description,
        @Schema(description = "Login of the repository owner", example = "user123") String owner,
        @Schema(example = "Java") String language,
        @Schema(example = "450") int stars,
        @Schema(example = "120") int forks,
        @Schema(example = "2024-01-01T12:00:00Z") Instant lastUpdated
) {
}
