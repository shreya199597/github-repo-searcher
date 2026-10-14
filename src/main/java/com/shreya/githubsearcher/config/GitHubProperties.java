package com.shreya.githubsearcher.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Settings for talking to the GitHub REST API, bound from the {@code github.*} properties.
 *
 * @param baseUrl        GitHub API root, overridable for tests
 * @param token          optional personal access token; raises the search rate limit from 10 to 30 requests/minute
 * @param perPage        how many repositories to request per search (GitHub allows at most 100)
 * @param connectTimeout TCP connect timeout
 * @param readTimeout    time to wait for GitHub's response
 */
@Validated
@ConfigurationProperties(prefix = "github")
public record GitHubProperties(
        @NotBlank String baseUrl,
        String token,
        @Min(1) @Max(100) int perPage,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout
) {

    public boolean hasToken() {
        return token != null && !token.isBlank();
    }
}
