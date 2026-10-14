package com.shreya.githubsearcher.exception;

import java.time.Instant;
import java.util.Optional;

/** GitHub refused the request because the rate limit was hit (maps to HTTP 429). */
public class GitHubRateLimitException extends RuntimeException {

    private final transient Instant resetAt;

    public GitHubRateLimitException(String message, Instant resetAt) {
        super(message);
        this.resetAt = resetAt;
    }

    /** When GitHub says the limit resets, if it told us. */
    public Optional<Instant> getResetAt() {
        return Optional.ofNullable(resetAt);
    }
}
