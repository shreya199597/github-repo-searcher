package com.shreya.githubsearcher.controller;

import com.shreya.githubsearcher.dto.RepositoryListResponse;
import com.shreya.githubsearcher.dto.SearchRequest;
import com.shreya.githubsearcher.dto.SearchResponse;
import com.shreya.githubsearcher.service.GitHubSearchService;
import com.shreya.githubsearcher.service.RepositoryQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/github", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "GitHub repositories", description = "Search GitHub and query the saved results")
public class GitHubController {

    private final GitHubSearchService searchService;
    private final RepositoryQueryService queryService;

    public GitHubController(GitHubSearchService searchService, RepositoryQueryService queryService) {
        this.searchService = searchService;
        this.queryService = queryService;
    }

    @PostMapping(path = "/search", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Search GitHub repositories and save the results",
            description = """
                    Searches repository names on GitHub (partial or full match), optionally filtered by \
                    language, and saves every result. Repositories already in the database are updated \
                    instead of duplicated.""")
    @ApiResponse(responseCode = "200", description = "Search completed; results saved (the list may be empty)")
    @ApiResponse(responseCode = "400", description = "Invalid request body or search query",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "429", description = "GitHub rate limit exceeded; see the Retry-After header",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "502", description = "GitHub returned an error",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "503", description = "GitHub could not be reached",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public SearchResponse search(@Valid @RequestBody SearchRequest request) {
        return searchService.search(request);
    }

    @GetMapping("/repositories")
    @Operation(summary = "Get saved repositories",
            description = "Returns repositories saved by earlier searches, filtered and sorted in descending order.")
    @ApiResponse(responseCode = "200", description = "Matching repositories (the list may be empty)")
    @ApiResponse(responseCode = "400", description = "Invalid query parameter",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public RepositoryListResponse getRepositories(
            @Parameter(description = "Programming language (case-insensitive exact match)", example = "Java")
            @RequestParam(required = false) String language,

            @Parameter(description = "Minimum number of stars", example = "100")
            @RequestParam(required = false) @Min(value = 0, message = "minStars must be 0 or greater") Integer minStars,

            @Parameter(description = "Sort field, descending", schema = @Schema(allowableValues = {"stars", "forks", "updated"}, defaultValue = "stars"))
            @RequestParam(required = false) String sort,

            @Parameter(description = "Zero-based page number")
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be 0 or greater") int page,

            @Parameter(description = "Page size (1-100)")
            @RequestParam(defaultValue = "50")
            @Min(value = 1, message = "size must be between 1 and 100")
            @Max(value = 100, message = "size must be between 1 and 100") int size) {
        return queryService.find(language, minStars, sort, page, size);
    }
}
