package com.shreya.githubsearcher.client;

/**
 * Builds the {@code q} parameter for GitHub's repository search.
 *
 * <p>The search terms are restricted to repository names with the {@code in:name} qualifier,
 * which gives the partial or full name match the assignment asks for. A language, if given,
 * becomes a {@code language:} qualifier, quoted so that names with spaces such as
 * "Visual Basic .NET" work.
 */
public final class GitHubQueryBuilder {

    private GitHubQueryBuilder() {
    }

    public static String build(String query, String language) {
        StringBuilder q = new StringBuilder(query.trim()).append(" in:name");
        if (language != null && !language.isBlank()) {
            q.append(" language:\"").append(language.trim().replace("\"", "")).append('"');
        }
        return q.toString();
    }
}
