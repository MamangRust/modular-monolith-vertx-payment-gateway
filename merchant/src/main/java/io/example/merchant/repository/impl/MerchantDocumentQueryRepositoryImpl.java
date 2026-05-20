package io.example.merchant.repository.impl;

import java.util.ArrayList;
import java.util.List;

import io.example.common.domain.PagedResult;
import io.example.merchant.model.MerchantDocument;
import io.example.merchant.repository.MerchantDocumentQueryRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import pb.merchant_document.MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest;

public class MerchantDocumentQueryRepositoryImpl implements MerchantDocumentQueryRepository {
  private final Pool pool;

  public MerchantDocumentQueryRepositoryImpl(Pool pool) {
    this.pool = pool;
  }

  private String normalizeSearch(String search) {
    return (search == null || search.isBlank()) ? null : search;
  }

  @Override
  public Future<PagedResult<MerchantDocument>> findAllDocuments(FindAllMerchantDocumentsRequest request) {
    int pageSize = request.getPageSize() > 0 ? request.getPageSize() : 10;
    int offset = Math.max(0, request.getPage() - 1) * pageSize;
    String search = normalizeSearch(request.getSearch());

    String sql = """
        SELECT document_id AS id, merchant_id, document_type, document_url, status, note, created_at, updated_at, deleted_at, COUNT(*) OVER() AS total_count
        FROM merchant_documents
        WHERE ($1::TEXT IS NULL OR document_type ILIKE '%' || $1 || '%' OR status ILIKE '%' || $1 || '%' OR note ILIKE '%' || $1 || '%')
        ORDER BY document_id LIMIT $2 OFFSET $3
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(search, pageSize, offset))
        .map(rows -> {
          List<MerchantDocument> list = new ArrayList<>();
          int total = 0;
          for (Row row : rows) {
            list.add(MerchantDocument.fromRow(row));
            if (total == 0)
              total = row.getInteger("total_count");
          }
          return new PagedResult<>(list, total);
        });
  }

  @Override
  public Future<MerchantDocument> findByIdDocument(int id) {
    String sql = "SELECT document_id AS id, merchant_id, document_type, document_url, status, note, created_at, updated_at, deleted_at FROM merchant_documents WHERE document_id = $1 AND deleted_at IS NULL";
    return pool.preparedQuery(sql).execute(Tuple.of(id))
        .map(rows -> rows.iterator().hasNext() ? MerchantDocument.fromRow(rows.iterator().next()) : null);
  }

  @Override
  public Future<PagedResult<MerchantDocument>> findByActiveDocuments(FindAllMerchantDocumentsRequest request) {
    int pageSize = request.getPageSize() > 0 ? request.getPageSize() : 10;
    int offset = Math.max(0, request.getPage() - 1) * pageSize;
    String search = normalizeSearch(request.getSearch());

    String sql = """
        SELECT document_id AS id, merchant_id, document_type, document_url, status, note, created_at, updated_at, deleted_at, COUNT(*) OVER() AS total_count
        FROM merchant_documents
        WHERE deleted_at IS NULL
          AND ($1::TEXT IS NULL OR document_type ILIKE '%' || $1 || '%' OR status ILIKE '%' || $1 || '%' OR note ILIKE '%' || $1 || '%')
        ORDER BY document_id LIMIT $2 OFFSET $3
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(search, pageSize, offset))
        .map(rows -> {
          List<MerchantDocument> list = new ArrayList<>();
          int total = 0;
          for (Row row : rows) {
            list.add(MerchantDocument.fromRow(row));
            if (total == 0)
              total = row.getInteger("total_count");
          }
          return new PagedResult<>(list, total);
        });
  }

  @Override
  public Future<PagedResult<MerchantDocument>> findByTrashedDocuments(FindAllMerchantDocumentsRequest request) {
    int pageSize = request.getPageSize() > 0 ? request.getPageSize() : 10;
    int offset = Math.max(0, request.getPage() - 1) * pageSize;
    String search = normalizeSearch(request.getSearch());

    String sql = """
        SELECT document_id AS id, merchant_id, document_type, document_url, status, note, created_at, updated_at, deleted_at, COUNT(*) OVER() AS total_count
        FROM merchant_documents
        WHERE deleted_at IS NOT NULL
          AND ($1::TEXT IS NULL OR document_type ILIKE '%' || $1 || '%' OR status ILIKE '%' || $1 || '%' OR note ILIKE '%' || $1 || '%')
        ORDER BY document_id LIMIT $2 OFFSET $3
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(search, pageSize, offset))
        .map(rows -> {
          List<MerchantDocument> list = new ArrayList<>();
          int total = 0;
          for (Row row : rows) {
            list.add(MerchantDocument.fromRow(row));
            if (total == 0)
              total = row.getInteger("total_count");
          }
          return new PagedResult<>(list, total);
        });
  }
}
