package com.shreya.githubsearcher.mapper;

import com.shreya.githubsearcher.client.dto.GitHubRepositoryItem;
import com.shreya.githubsearcher.dto.RepositoryResponse;
import com.shreya.githubsearcher.entity.GitHubRepo;

import java.time.Instant;

/** Converts between GitHub's API model, our entity and our API response. */
public final class RepositoryMapper {

    private RepositoryMapper() {
    }

    public static GitHubRepo toEntity(GitHubRepositoryItem item, Instant fetchedAt) {
        String owner = item.owner() != null ? item.owner().login() : null;
        return GitHubRepo.builder()
                .id(item.id())
                .name(item.name())
                .fullName(item.fullName() != null ? item.fullName() : owner + "/" + item.name())
                .description(item.description())
                .ownerName(owner)
                .language(item.language())
                .stars(item.stargazersCount())
                .forks(item.forksCount())
                .watchers(item.watchersCount())
                .openIssues(item.openIssuesCount())
                .htmlUrl(item.htmlUrl())
                .createdAt(item.createdAt())
                .lastUpdated(item.updatedAt())
                .pushedAt(item.pushedAt())
                .fetchedAt(fetchedAt)
                .build();
    }

    public static RepositoryResponse toResponse(GitHubRepo repo) {
        return new RepositoryResponse(
                repo.getId(),
                repo.getName(),
                repo.getDescription(),
                repo.getOwnerName(),
                repo.getLanguage(),
                repo.getStars(),
                repo.getForks(),
                repo.getLastUpdated());
    }
}
