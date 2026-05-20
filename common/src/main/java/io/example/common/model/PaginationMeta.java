package io.example.common.model;

public record PaginationMeta(
    int currentPage,
    int pageSize,
    int totalPages,
    int totalRecords) {
}
