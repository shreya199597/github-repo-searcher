package com.shreya.githubsearcher.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Criteria for searching GitHub repositories")
public record SearchRequest(

        @Schema(description = "Text to match against repository names (partial or full match)",
                example = "spring boot", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "query must not be blank")
        @Size(max = 200, message = "query must be at most 200 characters")
        String query,

        @Schema(description = "Programming language to filter by", example = "Java")
        @Size(max = 50, message = "language must be at most 50 characters")
        @Pattern(regexp = "^[\\p{L}\\p{N} .+#_-]*$",
                message = "language may only contain letters, digits, spaces and . + # _ -")
        String language,

        @Schema(description = "Sort order (descending). Defaults to stars.",
                allowableValues = {"stars", "forks", "updated"}, example = "stars")
        String sort
) {
}
