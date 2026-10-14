package com.shreya.githubsearcher.service;

import com.shreya.githubsearcher.client.GitHubClient;
import com.shreya.githubsearcher.client.dto.GitHubRepositoryItem;
import com.shreya.githubsearcher.client.dto.GitHubSearchResponse;
import com.shreya.githubsearcher.domain.SortField;
import com.shreya.githubsearcher.dto.RepositoryResponse;
import com.shreya.githubsearcher.dto.SearchRequest;
import com.shreya.githubsearcher.dto.SearchResponse;
import com.shreya.githubsearcher.entity.GitHubRepo;
import com.shreya.githubsearcher.mapper.RepositoryMapper;
import com.shreya.githubsearcher.repository.GitHubRepoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/** Searches GitHub and saves what it finds. */
@Service
public class GitHubSearchService {

    static final String SAVED_MESSAGE = "Repositories fetched and saved successfully";
    static final String NO_RESULTS_MESSAGE = "No repositories found matching the search criteria";

    private static final Logger log = LoggerFactory.getLogger(GitHubSearchService.class);

    private final GitHubClient gitHubClient;
    private final GitHubRepoRepository repository;
    private final Clock clock;

    public GitHubSearchService(GitHubClient gitHubClient, GitHubRepoRepository repository, Clock clock) {
        this.gitHubClient = gitHubClient;
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Deliberately not {@code @Transactional}: the GitHub call can take seconds and should not hold
     * a database connection. The upsert runs in its own transaction once the results are in.
     */
    public SearchResponse search(SearchRequest request) {
        SortField sort = SortField.fromValue(request.sort());
        GitHubSearchResponse response = gitHubClient.searchRepositories(request.query(), request.language(), sort);

        Instant fetchedAt = Instant.now(clock);
        List<GitHubRepo> repos = response.itemsOrEmpty().stream()
                .filter(GitHubSearchService::isUsable)
                .map(item -> RepositoryMapper.toEntity(item, fetchedAt))
                .toList();

        if (repos.isEmpty()) {
            return new SearchResponse(NO_RESULTS_MESSAGE, List.of());
        }

        int written = repository.upsertAll(repos);
        log.info("Upserted {} repositories", written);

        List<RepositoryResponse> body = repos.stream().map(RepositoryMapper::toResponse).toList();
        return new SearchResponse(SAVED_MESSAGE, body);
    }

    private static boolean isUsable(GitHubRepositoryItem item) {
        boolean usable = item != null && item.id() != null && item.name() != null
                && item.owner() != null && item.owner().login() != null;
        if (!usable) {
            log.warn("Skipping GitHub search item with missing id, name or owner: {}", item);
        }
        return usable;
    }
}
