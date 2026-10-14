package com.shreya.githubsearcher.domain;

import com.shreya.githubsearcher.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SortFieldTest {

    @ParameterizedTest
    @CsvSource({
            "stars, STARS",
            "forks, FORKS",
            "updated, UPDATED",
            "STARS, STARS",
            "' Forks ', FORKS"
    })
    void parsesValuesCaseInsensitively(String raw, SortField expected) {
        assertThat(SortField.fromValue(raw)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void defaultsToStarsWhenMissing(String raw) {
        assertThat(SortField.fromValue(raw)).isEqualTo(SortField.STARS);
    }

    @ParameterizedTest
    @ValueSource(strings = {"popularity", "star", "updated_at", "name"})
    void rejectsUnknownValues(String raw) {
        assertThatThrownBy(() -> SortField.fromValue(raw))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Invalid sort '" + raw + "'")
                .hasMessageContaining("stars, forks, updated");
    }

    @Test
    void mapsToGitHubValueAndEntityProperty() {
        assertThat(SortField.UPDATED.value()).isEqualTo("updated");
        assertThat(SortField.UPDATED.entityProperty()).isEqualTo("lastUpdated");
        assertThat(SortField.STARS.entityProperty()).isEqualTo("stars");
        assertThat(SortField.FORKS.entityProperty()).isEqualTo("forks");
    }
}
