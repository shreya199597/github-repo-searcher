package com.shreya.githubsearcher.service;

import com.shreya.githubsearcher.domain.SortField;
import com.shreya.githubsearcher.dto.RepositoryListResponse;
import com.shreya.githubsearcher.dto.RepositoryResponse;
import com.shreya.githubsearcher.entity.GitHubRepo;
import com.shreya.githubsearcher.mapper.RepositoryMapper;
import com.shreya.githubsearcher.repository.GitHubRepoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.shreya.githubsearcher.repository.GitHubRepoSpecifications.hasLanguage;
import static com.shreya.githubsearcher.repository.GitHubRepoSpecifications.hasMinStars;

/** Reads previously saved repositories. */
@Service
public class RepositoryQueryService {

    private final GitHubRepoRepository repository;

    public RepositoryQueryService(GitHubRepoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public RepositoryListResponse find(String language, Integer minStars, String sort, int page, int size) {
        SortField sortField = SortField.fromValue(sort);
        Specification<GitHubRepo> filters = Specification.allOf(hasLanguage(language), hasMinStars(minStars));

        // Descending on the chosen field; id breaks ties so paging is stable.
        // NULLs sort last via hibernate.order_by.default_null_ordering (Criteria queries can't set it per query).
        Sort order = Sort.by(Sort.Order.desc(sortField.entityProperty()), Sort.Order.desc("id"));

        List<RepositoryResponse> repositories = repository
                .findAll(filters, PageRequest.of(page, size, order))
                .map(RepositoryMapper::toResponse)
                .getContent();
        return new RepositoryListResponse(repositories);
    }
}
