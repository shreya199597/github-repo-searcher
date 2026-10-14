package com.shreya.githubsearcher.repository;

import com.shreya.githubsearcher.entity.GitHubRepo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Reads use Spring Data JPA (with {@link GitHubRepoSpecifications} for optional filters);
 * writes go through the batch upsert in {@link GitHubRepoUpsertRepository}.
 */
public interface GitHubRepoRepository extends JpaRepository<GitHubRepo, Long>,
        JpaSpecificationExecutor<GitHubRepo>,
        GitHubRepoUpsertRepository {
}
