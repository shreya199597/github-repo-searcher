package com.shreya.githubsearcher.exception;

/** GitHub returned an error or a response we could not use (maps to HTTP 502). */
public class GitHubApiException extends RuntimeException {

    private final int upstreamStatus;

    public GitHubApiException(String message, int upstreamStatus) {
        super(message);
        this.upstreamStatus = upstreamStatus;
    }

    public GitHubApiException(String message, Throwable cause) {
        super(message, cause);
        this.upstreamStatus = 0;
    }

    /** The HTTP status GitHub returned, or 0 if there was no usable HTTP response. */
    public int getUpstreamStatus() {
        return upstreamStatus;
    }
}
