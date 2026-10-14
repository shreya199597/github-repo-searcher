package com.shreya.githubsearcher.domain;

import com.shreya.githubsearcher.exception.InvalidRequestException;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * The sort options both endpoints accept. Each value knows its name in the GitHub API
 * and the entity property used when sorting stored results.
 */
public enum SortField {

    STARS("stars", "stars"),
    FORKS("forks", "forks"),
    UPDATED("updated", "lastUpdated");

    public static final SortField DEFAULT = STARS;

    private final String value;
    private final String entityProperty;

    SortField(String value, String entityProperty) {
        this.value = value;
        this.entityProperty = entityProperty;
    }

    public String value() {
        return value;
    }

    public String entityProperty() {
        return entityProperty;
    }

    /**
     * Parses a sort value case-insensitively. A missing value falls back to {@link #DEFAULT}.
     *
     * @throws InvalidRequestException if the value is not one of the supported options
     */
    public static SortField fromValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(field -> field.value.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException(
                        "Invalid sort '%s'. Allowed values: %s".formatted(raw, allowedValues())));
    }

    public static String allowedValues() {
        return Arrays.stream(values()).map(SortField::value).collect(Collectors.joining(", "));
    }
}
