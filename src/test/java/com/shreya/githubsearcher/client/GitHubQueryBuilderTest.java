package com.shreya.githubsearcher.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubQueryBuilderTest {

    @Test
    void restrictsSearchToRepositoryNames() {
        assertThat(GitHubQueryBuilder.build("spring boot", null)).isEqualTo("spring boot in:name");
    }

    @Test
    void addsQuotedLanguageQualifier() {
        assertThat(GitHubQueryBuilder.build("spring boot", "Java"))
                .isEqualTo("spring boot in:name language:\"Java\"");
    }

    @Test
    void keepsLanguagesWithSpacesAndSymbolsIntact() {
        assertThat(GitHubQueryBuilder.build("parser", "Visual Basic .NET"))
                .isEqualTo("parser in:name language:\"Visual Basic .NET\"");
        assertThat(GitHubQueryBuilder.build("engine", "C++"))
                .isEqualTo("engine in:name language:\"C++\"");
    }

    @Test
    void trimsInputAndStripsQuotesFromLanguage() {
        assertThat(GitHubQueryBuilder.build("  cli  ", " \"Go\" "))
                .isEqualTo("cli in:name language:\"Go\"");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "  ")
    void omitsLanguageQualifierWhenBlank(String language) {
        assertThat(GitHubQueryBuilder.build("cli", language)).isEqualTo("cli in:name");
    }
}
