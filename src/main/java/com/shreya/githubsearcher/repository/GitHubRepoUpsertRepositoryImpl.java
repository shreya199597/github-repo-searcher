package com.shreya.githubsearcher.repository;

import com.shreya.githubsearcher.entity.GitHubRepo;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;

/**
 * Upserts with PostgreSQL's {@code INSERT ... ON CONFLICT DO UPDATE}, sent as one JDBC batch.
 *
 * <p>This is one round trip per search instead of a SELECT plus an INSERT or UPDATE per repository,
 * and it is safe when two searches save the same repository at the same time, because the database
 * resolves the conflict atomically.
 *
 * <p>Spring Data finds this class by its name: fragment interface name + {@code Impl}.
 */
public class GitHubRepoUpsertRepositoryImpl implements GitHubRepoUpsertRepository {

    static final String UPSERT_SQL = """
            INSERT INTO github_repository (
                id, name, full_name, description, owner_name, language,
                stars_count, forks_count, watchers_count, open_issues_count,
                html_url, created_at, last_updated, pushed_at, fetched_at)
            VALUES (
                :id, :name, :fullName, :description, :ownerName, :language,
                :stars, :forks, :watchers, :openIssues,
                :htmlUrl, :createdAt, :lastUpdated, :pushedAt, :fetchedAt)
            ON CONFLICT (id) DO UPDATE SET
                name              = EXCLUDED.name,
                full_name         = EXCLUDED.full_name,
                description       = EXCLUDED.description,
                owner_name        = EXCLUDED.owner_name,
                language          = EXCLUDED.language,
                stars_count       = EXCLUDED.stars_count,
                forks_count       = EXCLUDED.forks_count,
                watchers_count    = EXCLUDED.watchers_count,
                open_issues_count = EXCLUDED.open_issues_count,
                html_url          = EXCLUDED.html_url,
                created_at        = EXCLUDED.created_at,
                last_updated      = EXCLUDED.last_updated,
                pushed_at         = EXCLUDED.pushed_at,
                fetched_at        = EXCLUDED.fetched_at
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public GitHubRepoUpsertRepositoryImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public int upsertAll(Collection<GitHubRepo> repositories) {
        if (repositories.isEmpty()) {
            return 0;
        }
        SqlParameterSource[] batch = repositories.stream()
                .map(GitHubRepoUpsertRepositoryImpl::toParameters)
                .toArray(SqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(UPSERT_SQL, batch);
        return batch.length;
    }

    private static SqlParameterSource toParameters(GitHubRepo repo) {
        return new MapSqlParameterSource()
                .addValue("id", repo.getId())
                .addValue("name", repo.getName())
                .addValue("fullName", repo.getFullName())
                .addValue("description", repo.getDescription())
                .addValue("ownerName", repo.getOwnerName())
                .addValue("language", repo.getLanguage())
                .addValue("stars", repo.getStars())
                .addValue("forks", repo.getForks())
                .addValue("watchers", repo.getWatchers())
                .addValue("openIssues", repo.getOpenIssues())
                .addValue("htmlUrl", repo.getHtmlUrl())
                .addValue("createdAt", toUtc(repo.getCreatedAt()))
                .addValue("lastUpdated", toUtc(repo.getLastUpdated()))
                .addValue("pushedAt", toUtc(repo.getPushedAt()))
                .addValue("fetchedAt", toUtc(repo.getFetchedAt()));
    }

    // The PostgreSQL driver binds OffsetDateTime to TIMESTAMPTZ natively; Instant is not supported directly.
    private static OffsetDateTime toUtc(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
