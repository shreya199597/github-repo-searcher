package com.shreya.githubsearcher.repository;

import com.shreya.githubsearcher.TestData;
import com.shreya.githubsearcher.entity.GitHubRepo;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

import static com.shreya.githubsearcher.repository.GitHubRepoSpecifications.hasLanguage;
import static com.shreya.githubsearcher.repository.GitHubRepoSpecifications.hasMinStars;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against a real PostgreSQL (embedded binary) with the Flyway schema applied,
 * because ON CONFLICT and LOWER() indexes are PostgreSQL features an in-memory H2 would only imitate.
 */
@DataJpaTest
@AutoConfigureEmbeddedDatabase(provider = ZONKY, type = POSTGRES)
@ImportAutoConfiguration(JdbcTemplateAutoConfiguration.class)
class GitHubRepoRepositoryIntegrationTest {

    private static final Instant JAN = Instant.parse("2024-01-01T00:00:00Z");
    private static final Instant FEB = Instant.parse("2024-02-01T00:00:00Z");
    private static final Instant MAR = Instant.parse("2024-03-01T00:00:00Z");

    @Autowired
    private GitHubRepoRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void upsertInsertsNewRepositories() {
        int written = repository.upsertAll(List.of(
                TestData.repo(1L, "alpha", "Java", 100, 10, JAN),
                TestData.repo(2L, "beta", "Go", 50, 5, FEB)));

        assertThat(written).isEqualTo(2);
        assertThat(repository.count()).isEqualTo(2);
        GitHubRepo alpha = repository.findById(1L).orElseThrow();
        assertThat(alpha.getName()).isEqualTo("alpha");
        assertThat(alpha.getOwnerName()).isEqualTo("user123");
        assertThat(alpha.getLastUpdated()).isEqualTo(JAN);
        assertThat(alpha.getFetchedAt()).isEqualTo(TestData.FETCHED_AT);
    }

    @Test
    void upsertUpdatesExistingRepositoryInsteadOfDuplicating() {
        repository.upsertAll(List.of(TestData.repo(1L, "alpha", "Java", 100, 10, JAN)));

        repository.upsertAll(List.of(TestData.repo(1L, "alpha-renamed", "Kotlin", 999, 77, MAR)));
        entityManager.clear();

        assertThat(repository.count()).isEqualTo(1);
        GitHubRepo updated = repository.findById(1L).orElseThrow();
        assertThat(updated.getName()).isEqualTo("alpha-renamed");
        assertThat(updated.getLanguage()).isEqualTo("Kotlin");
        assertThat(updated.getStars()).isEqualTo(999);
        assertThat(updated.getForks()).isEqualTo(77);
        assertThat(updated.getLastUpdated()).isEqualTo(MAR);
    }

    @Test
    void upsertHandlesMixOfNewAndExisting() {
        repository.upsertAll(List.of(TestData.repo(1L, "alpha", "Java", 100, 10, JAN)));

        repository.upsertAll(List.of(
                TestData.repo(1L, "alpha", "Java", 150, 12, FEB),
                TestData.repo(2L, "beta", "Java", 20, 1, FEB)));
        entityManager.clear();

        assertThat(repository.count()).isEqualTo(2);
        assertThat(repository.findById(1L).orElseThrow().getStars()).isEqualTo(150);
    }

    @Test
    void upsertOfEmptyListDoesNothing() {
        assertThat(repository.upsertAll(List.of())).isZero();
        assertThat(repository.count()).isZero();
    }

    @Test
    void storesNullableFields() {
        GitHubRepo bare = GitHubRepo.builder().id(9L).name("bare").fullName("o/bare").ownerName("o")
                .fetchedAt(TestData.FETCHED_AT).build();

        repository.upsertAll(List.of(bare));
        entityManager.clear();

        GitHubRepo stored = repository.findById(9L).orElseThrow();
        assertThat(stored.getDescription()).isNull();
        assertThat(stored.getLanguage()).isNull();
        assertThat(stored.getLastUpdated()).isNull();
    }

    @Test
    void filtersByLanguageCaseInsensitivelyAndExactly() {
        seed();

        List<GitHubRepo> result = repository.findAll(hasLanguage("java"), Sort.by("id"));

        assertThat(result).extracting(GitHubRepo::getName).containsExactly("java-big", "java-small");
    }

    @Test
    void filtersByMinimumStarsInclusive() {
        seed();

        List<GitHubRepo> result = repository.findAll(hasMinStars(300), Sort.by("id"));

        assertThat(result).extracting(GitHubRepo::getName).containsExactly("java-big", "js-mid");
    }

    @Test
    void combinesFiltersAndIgnoresMissingOnes() {
        seed();

        assertThat(repository.findAll(Specification.allOf(hasLanguage("Java"), hasMinStars(300))))
                .extracting(GitHubRepo::getName).containsExactly("java-big");
        assertThat(repository.findAll(Specification.allOf(hasLanguage(null), hasMinStars(null))))
                .hasSize(4);
    }

    @Test
    void sortsByForksAndByUpdatedDateWithNullsLast() {
        seed();

        assertThat(repository.findAll(Sort.by(Sort.Order.desc("forks"))))
                .extracting(GitHubRepo::getName)
                .containsExactly("js-mid", "java-big", "java-small", "no-lang");

        // NULLs last comes from hibernate.order_by.default_null_ordering in application.yml
        assertThat(repository.findAll(Sort.by(Sort.Order.desc("lastUpdated"))))
                .extracting(GitHubRepo::getName)
                .containsExactly("java-small", "js-mid", "java-big", "no-lang");
    }

    @Test
    void pagesResults() {
        seed();

        var page = repository.findAll(Specification.<GitHubRepo>allOf(),
                PageRequest.of(1, 2, Sort.by(Sort.Order.desc("stars"))));

        assertThat(page.getContent()).extracting(GitHubRepo::getName).containsExactly("java-small", "no-lang");
        assertThat(page.getTotalElements()).isEqualTo(4);
    }

    private void seed() {
        repository.upsertAll(List.of(
                TestData.repo(1L, "java-big", "Java", 500, 40, JAN),
                TestData.repo(2L, "java-small", "Java", 120, 5, MAR),
                TestData.repo(3L, "js-mid", "JavaScript", 300, 90, FEB),
                TestData.repo(4L, "no-lang", null, 10, 1, null)));
        entityManager.clear();
    }
}
