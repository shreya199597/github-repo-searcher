package com.shreya.githubsearcher.service;

import com.shreya.githubsearcher.TestData;
import com.shreya.githubsearcher.dto.RepositoryListResponse;
import com.shreya.githubsearcher.entity.GitHubRepo;
import com.shreya.githubsearcher.exception.InvalidRequestException;
import com.shreya.githubsearcher.repository.GitHubRepoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryQueryServiceTest {

    @Mock
    private GitHubRepoRepository repository;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    private RepositoryQueryService service;

    @BeforeEach
    void setUp() {
        service = new RepositoryQueryService(repository);
    }

    @Test
    void returnsMappedRepositories() {
        GitHubRepo repo = TestData.repo(1L, "a", "Java", 500, 50, Instant.parse("2024-01-01T12:00:00Z"));
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(repo)));

        RepositoryListResponse result = service.find("Java", 100, "stars", 0, 50);

        assertThat(result.repositories()).singleElement()
                .satisfies(r -> {
                    assertThat(r.id()).isEqualTo(1L);
                    assertThat(r.stars()).isEqualTo(500);
                });
    }

    @Test
    void sortsDescendingByChosenFieldWithIdAsTieBreaker() {
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        service.find(null, null, "updated", 2, 10);

        verify(repository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("lastUpdated").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(pageable.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void defaultsToSortingByStars() {
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        service.find(null, null, null, 0, 50);

        verify(repository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("stars")).isNotNull();
    }

    @Test
    void rejectsInvalidSort() {
        assertThatThrownBy(() -> service.find(null, null, "name", 0, 50))
                .isInstanceOf(InvalidRequestException.class);

        verifyNoInteractions(repository);
    }
}
