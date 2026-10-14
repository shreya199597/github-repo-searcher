package com.shreya.githubsearcher.exception;

/** GitHub could not be reached or timed out (maps to HTTP 503). */
public class GitHubUnavailableException extends RuntimeException {

    public GitHubUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
