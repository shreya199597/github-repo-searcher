package com.shreya.githubsearcher.repository;

import com.shreya.githubsearcher.entity.GitHubRepo;

import java.util.Collection;

/** Custom Spring Data fragment for writing repositories with a single batched upsert. */
public interface GitHubRepoUpsertRepository {

    /**
     * Inserts repositories that are new and updates the ones that already exist (matched on GitHub id).
     *
     * @return the number of repositories written
     */
    int upsertAll(Collection<GitHubRepo> repositories);
}
