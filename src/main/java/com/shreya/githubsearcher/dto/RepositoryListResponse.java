package com.shreya.githubsearcher.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Stored repositories matching the filters")
public record RepositoryListResponse(List<RepositoryResponse> repositories) {
}
