package com.shreya.githubsearcher.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Turns every error into an RFC 9457 "problem detail" JSON body, so clients always get the same shape:
 * {@code type, title, status, detail, instance}, plus {@code errors} for validation failures.
 *
 * <p>Extending {@link ResponseEntityExceptionHandler} covers Spring MVC's own errors
 * (malformed JSON, wrong parameter types, unsupported media types, unknown paths, ...).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidRequestException.class)
    public ProblemDetail handleInvalidRequest(InvalidRequestException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", e.getMessage());
    }

    @ExceptionHandler(GitHubRateLimitException.class)
    public ResponseEntity<ProblemDetail> handleRateLimit(GitHubRateLimitException e) {
        ProblemDetail body = problem(HttpStatus.TOO_MANY_REQUESTS, "GitHub rate limit exceeded", e.getMessage());
        HttpHeaders headers = new HttpHeaders();
        e.getResetAt().ifPresent(resetAt -> {
            long seconds = Math.max(1, Duration.between(Instant.now(), resetAt).toSeconds());
            headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(seconds));
            body.setProperty("retryAfterSeconds", seconds);
            body.setProperty("resetAt", resetAt);
        });
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).headers(headers).body(body);
    }

    @ExceptionHandler(GitHubApiException.class)
    public ProblemDetail handleGitHubApi(GitHubApiException e) {
        log.error("GitHub API error", e);
        return problem(HttpStatus.BAD_GATEWAY, "GitHub API error", e.getMessage());
    }

    @ExceptionHandler(GitHubUnavailableException.class)
    public ProblemDetail handleGitHubUnavailable(GitHubUnavailableException e) {
        log.error("GitHub unavailable", e);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "GitHub unavailable", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
                "An unexpected error occurred. Please try again later.");
    }

    /** Invalid request body fields (e.g. blank query). */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException e, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        List<Map<String, String>> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", String.valueOf(error.getDefaultMessage())))
                .toList();
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Validation failed", "One or more fields are invalid");
        body.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    /** Invalid query parameters (e.g. minStars=-1). */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            @NonNull HandlerMethodValidationException e, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        List<String> errors = e.getAllErrors().stream()
                .map(error -> String.valueOf(error.getDefaultMessage()))
                .toList();
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Validation failed", String.join("; ", errors));
        body.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
