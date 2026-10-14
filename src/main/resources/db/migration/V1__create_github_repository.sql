CREATE TABLE github_repository
(
    id                BIGINT PRIMARY KEY, -- GitHub's repository id
    name              VARCHAR(255) NOT NULL,
    full_name         VARCHAR(512) NOT NULL,
    description       TEXT,
    owner_name        VARCHAR(255) NOT NULL,
    language          VARCHAR(100),
    stars_count       INTEGER      NOT NULL DEFAULT 0,
    forks_count       INTEGER      NOT NULL DEFAULT 0,
    watchers_count    INTEGER      NOT NULL DEFAULT 0,
    open_issues_count INTEGER      NOT NULL DEFAULT 0,
    html_url          VARCHAR(1024),
    created_at        TIMESTAMPTZ,
    last_updated      TIMESTAMPTZ,
    pushed_at         TIMESTAMPTZ,
    fetched_at        TIMESTAMPTZ  NOT NULL
);

-- Supports the case-insensitive language filter on GET /api/github/repositories
CREATE INDEX idx_github_repository_language ON github_repository (LOWER(language));

-- Support the three sort options
CREATE INDEX idx_github_repository_stars ON github_repository (stars_count DESC);
CREATE INDEX idx_github_repository_forks ON github_repository (forks_count DESC);
CREATE INDEX idx_github_repository_last_updated ON github_repository (last_updated DESC);
