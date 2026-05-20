package io.example.common.domain;

public record PaginationMeta(
    int currentPage,
    int pageSize,
    int totalPages,
    int totalRecords) {
}
