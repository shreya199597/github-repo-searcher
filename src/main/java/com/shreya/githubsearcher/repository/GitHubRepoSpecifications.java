package com.shreya.githubsearcher.repository;

import com.shreya.githubsearcher.entity.GitHubRepo;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/** Composable query filters for stored repositories. A null argument means "don't filter". */
public final class GitHubRepoSpecifications {

    private GitHubRepoSpecifications() {
    }

    /** Case-insensitive exact match, so "java" matches "Java" but not "JavaScript". */
    public static Specification<GitHubRepo> hasLanguage(String language) {
        if (language == null || language.isBlank()) {
            return null;
        }
        String normalized = language.trim().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> cb.equal(cb.lower(root.get("language")), normalized);
    }

    public static Specification<GitHubRepo> hasMinStars(Integer minStars) {
        if (minStars == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("stars"), minStars);
    }
}
