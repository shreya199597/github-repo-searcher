package com.shreya.githubsearcher.service;

import com.shreya.githubsearcher.TestData;
import com.shreya.githubsearcher.client.GitHubClient;
import com.shreya.githubsearcher.client.dto.GitHubRepositoryItem;
import com.shreya.githubsearcher.client.dto.GitHubSearchResponse;
import com.shreya.githubsearcher.domain.SortField;
import com.shreya.githubsearcher.dto.RepositoryResponse;
import com.shreya.githubsearcher.dto.SearchRequest;
import com.shreya.githubsearcher.dto.SearchResponse;
import com.shreya.githubsearcher.entity.GitHubRepo;
import com.shreya.githubsearcher.exception.GitHubRateLimitException;
import com.shreya.githubsearcher.exception.InvalidRequestException;
import com.shreya.githubsearcher.repository.GitHubRepoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitHubSearchServiceTest {

    @Mock
    private GitHubClient gitHubClient;

    @Mock
    private GitHubRepoRepository repository;

    @Captor
    private ArgumentCaptor<Collection<GitHubRepo>> savedCaptor;

    private GitHubSearchService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(TestData.FETCHED_AT, ZoneOffset.UTC);
        service = new GitHubSearchService(gitHubClient, repository, fixedClock);
    }

    @Test
    void searchesGitHubSavesResultsAndReturnsThem() {
        when(gitHubClient.searchRepositories("spring boot", "Java", SortField.STARS))
                .thenReturn(response(TestData.item(1L, "spring-boot-a", "Java", 450),
                        TestData.item(2L, "spring-boot-b", "Java", 200)));
        when(repository.upsertAll(any())).thenReturn(2);

        SearchResponse result = service.search(new SearchRequest("spring boot", "Java", "stars"));

        assertThat(result.message()).isEqualTo(GitHubSearchService.SAVED_MESSAGE);
        assertThat(result.repositories()).extracting(RepositoryResponse::id).containsExactly(1L, 2L);
        assertThat(result.repositories().getFirst().owner()).isEqualTo("user123");

        verify(repository).upsertAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue())
                .extracting(GitHubRepo::getId, GitHubRepo::getFetchedAt)
                .containsExactly(
                        tuple(1L, TestData.FETCHED_AT),
                        tuple(2L, TestData.FETCHED_AT));
    }

    @Test
    void defaultsToStarsWhenSortIsMissing() {
        when(gitHubClient.searchRepositories("cli", null, SortField.STARS)).thenReturn(response());

        service.search(new SearchRequest("cli", null, null));

        verify(gitHubClient).searchRepositories("cli", null, SortField.STARS);
    }

    @Test
    void passesUpdatedSortThrough() {
        when(gitHubClient.searchRepositories("cli", null, SortField.UPDATED)).thenReturn(response());

        service.search(new SearchRequest("cli", null, "UPDATED"));

        verify(gitHubClient).searchRepositories("cli", null, SortField.UPDATED);
    }

    @Test
    void returnsMessageAndSkipsDatabaseWhenNothingFound() {
        when(gitHubClient.searchRepositories(anyString(), any(), any())).thenReturn(response());

        SearchResponse result = service.search(new SearchRequest("zzzz", null, "stars"));

        assertThat(result.message()).isEqualTo(GitHubSearchService.NO_RESULTS_MESSAGE);
        assertThat(result.repositories()).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    void skipsItemsWithMissingRequiredFields() {
        GitHubRepositoryItem noId = new GitHubRepositoryItem(null, "x", null, null,
                new GitHubRepositoryItem.Owner("o"), null, 0, 0, 0, 0, null, null, null, null);
        GitHubRepositoryItem noOwner = new GitHubRepositoryItem(5L, "y", null, null,
                null, null, 0, 0, 0, 0, null, null, null, null);
        when(gitHubClient.searchRepositories(anyString(), any(), any()))
                .thenReturn(response(noId, noOwner, TestData.item(3L, "valid", "Go", 10)));

        SearchResponse result = service.search(new SearchRequest("x", null, null));

        assertThat(result.repositories()).extracting(RepositoryResponse::id).containsExactly(3L);
    }

    @Test
    void rejectsInvalidSortBeforeCallingGitHub() {
        assertThatThrownBy(() -> service.search(new SearchRequest("cli", null, "popularity")))
                .isInstanceOf(InvalidRequestException.class);

        verifyNoInteractions(gitHubClient, repository);
    }

    @Test
    void propagatesGitHubErrorsWithoutTouchingTheDatabase() {
        when(gitHubClient.searchRepositories(anyString(), any(), any()))
                .thenThrow(new GitHubRateLimitException("limit", null));

        assertThatThrownBy(() -> service.search(new SearchRequest("cli", null, null)))
                .isInstanceOf(GitHubRateLimitException.class);

        verifyNoInteractions(repository);
    }

    private static GitHubSearchResponse response(GitHubRepositoryItem... items) {
        List<GitHubRepositoryItem> list = Arrays.asList(items);
        return new GitHubSearchResponse(list.size(), false, list);
    }
}
