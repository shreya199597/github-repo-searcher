package com.shreya.githubsearcher.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A GitHub repository as stored in PostgreSQL. The primary key is GitHub's own repository id,
 * which is what makes upserts possible: the same repository always maps to the same row.
 */
@Entity
@Table(name = "github_repository")
public class GitHubRepo {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    private String language;

    @Column(name = "stars_count", nullable = false)
    private int stars;

    @Column(name = "forks_count", nullable = false)
    private int forks;

    @Column(name = "watchers_count", nullable = false)
    private int watchers;

    @Column(name = "open_issues_count", nullable = false)
    private int openIssues;

    @Column(name = "html_url")
    private String htmlUrl;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    @Column(name = "pushed_at")
    private Instant pushedAt;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    protected GitHubRepo() {
        // required by JPA
    }

    private GitHubRepo(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.fullName = builder.fullName;
        this.description = builder.description;
        this.ownerName = builder.ownerName;
        this.language = builder.language;
        this.stars = builder.stars;
        this.forks = builder.forks;
        this.watchers = builder.watchers;
        this.openIssues = builder.openIssues;
        this.htmlUrl = builder.htmlUrl;
        this.createdAt = builder.createdAt;
        this.lastUpdated = builder.lastUpdated;
        this.pushedAt = builder.pushedAt;
        this.fetchedAt = builder.fetchedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDescription() {
        return description;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getLanguage() {
        return language;
    }

    public int getStars() {
        return stars;
    }

    public int getForks() {
        return forks;
    }

    public int getWatchers() {
        return watchers;
    }

    public int getOpenIssues() {
        return openIssues;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public Instant getPushedAt() {
        return pushedAt;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public static final class Builder {
        private Long id;
        private String name;
        private String fullName;
        private String description;
        private String ownerName;
        private String language;
        private int stars;
        private int forks;
        private int watchers;
        private int openIssues;
        private String htmlUrl;
        private Instant createdAt;
        private Instant lastUpdated;
        private Instant pushedAt;
        private Instant fetchedAt;

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder ownerName(String ownerName) {
            this.ownerName = ownerName;
            return this;
        }

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        public Builder stars(int stars) {
            this.stars = stars;
            return this;
        }

        public Builder forks(int forks) {
            this.forks = forks;
            return this;
        }

        public Builder watchers(int watchers) {
            this.watchers = watchers;
            return this;
        }

        public Builder openIssues(int openIssues) {
            this.openIssues = openIssues;
            return this;
        }

        public Builder htmlUrl(String htmlUrl) {
            this.htmlUrl = htmlUrl;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder lastUpdated(Instant lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public Builder pushedAt(Instant pushedAt) {
            this.pushedAt = pushedAt;
            return this;
        }

        public Builder fetchedAt(Instant fetchedAt) {
            this.fetchedAt = fetchedAt;
            return this;
        }

        public GitHubRepo build() {
            return new GitHubRepo(this);
        }
    }
}
