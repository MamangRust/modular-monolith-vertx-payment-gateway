package io.example.merchant.repository.impl;

import io.example.merchant.model.MerchantDocument;
import io.example.merchant.repository.MerchantDocumentCommandRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;
import pb.merchant_document.MerchantDocumentCommand.CreateMerchantDocumentRequest;
import pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentRequest;
import pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentStatusRequest;

@RequiredArgsConstructor
public class MerchantDocumentCommandRepositoryImpl implements MerchantDocumentCommandRepository {
  private final Pool pool;

  @Override
  public Future<MerchantDocument> createMerchantDocument(CreateMerchantDocumentRequest request) {
    String sql = """
        INSERT INTO merchant_documents (merchant_id, document_type, document_url, status, note, created_at, updated_at)
        VALUES ($1, $2, $3, 'pending', 'note', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        RETURNING document_id AS id, merchant_id, document_type, document_url, status, note, created_at, updated_at, deleted_at
        """;
    return pool.preparedQuery(sql)
        .execute(Tuple.of(request.getMerchantId(), request.getDocumentType(), request.getDocumentUrl()))
        .map(rows -> rows.iterator().hasNext() ? MerchantDocument.fromRow(rows.iterator().next()) : null);
  }

  @Override
  public Future<MerchantDocument> updateMerchantDocument(UpdateMerchantDocumentRequest request) {
    String sql = """
        UPDATE merchant_documents SET document_type = $2, document_url = $3, status = $4, note = $5, updated_at = CURRENT_TIMESTAMP
        WHERE document_id = $1 AND deleted_at IS NULL
        RETURNING document_id AS id, merchant_id, document_type, document_url, status, note, created_at, updated_at, deleted_at
        """;
    return pool.preparedQuery(sql)
        .execute(Tuple.of(request.getDocumentId(), request.getDocumentType(), request.getDocumentUrl(),
            request.getStatus(), request.getNote()))
        .map(rows -> rows.iterator().hasNext() ? MerchantDocument.fromRow(rows.iterator().next()) : null);
  }

  @Override
  public Future<MerchantDocument> updateMerchantDocumentStatus(UpdateMerchantDocumentStatusRequest request) {
    String sql = """
        UPDATE merchant_documents SET status = $2, note = $3, updated_at = CURRENT_TIMESTAMP
        WHERE document_id = $1 AND deleted_at IS NULL
        RETURNING document_id AS id, merchant_id, document_type, document_url, status, note, created_at, updated_at, deleted_at
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(request.getDocumentId(), request.getStatus(), request.getNote()))
        .map(rows -> rows.iterator().hasNext() ? MerchantDocument.fromRow(rows.iterator().next()) : null);
  }

  @Override
  public Future<MerchantDocument> trashedMerchantDocument(Integer merchantDocumentId) {
    String sql = """
        UPDATE merchant_documents SET deleted_at = CURRENT_TIMESTAMP WHERE document_id = $1 AND deleted_at IS NULL
        RETURNING document_id AS id, merchant_id, document_type, document_url, status, note, created_at, updated_at, deleted_at
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(merchantDocumentId))
        .map(rows -> rows.iterator().hasNext() ? MerchantDocument.fromRow(rows.iterator().next()) : null);
  }

  @Override
  public Future<MerchantDocument> restoreMerchantDocument(Integer merchantDocumentId) {
    String sql = """
        UPDATE merchant_documents SET deleted_at = NULL WHERE document_id = $1 AND deleted_at IS NOT NULL
        RETURNING document_id AS id, merchant_id, document_type, document_url, status, note, created_at, updated_at, deleted_at
        """;
    return pool.preparedQuery(sql).execute(Tuple.of(merchantDocumentId))
        .map(rows -> rows.iterator().hasNext() ? MerchantDocument.fromRow(rows.iterator().next()) : null);
  }

  @Override
  public Future<Boolean> deleteMerchantDocumentPermanent(Integer merchantDocumentId) {
    return pool.preparedQuery("DELETE FROM merchant_documents WHERE document_id = $1 AND deleted_at IS NOT NULL")
        .execute(Tuple.of(merchantDocumentId))
        .map(rows -> rows.rowCount() > 0);
  }

  @Override
  public Future<Integer> restoreAllMerchantDocuments() {
    return pool.query("UPDATE merchant_documents SET deleted_at = NULL WHERE deleted_at IS NOT NULL")
        .execute()
        .map(rows -> rows.rowCount());
  }

  @Override
  public Future<Integer> deleteAllMerchantDocumentsPermanent() {
    return pool.query("DELETE FROM merchant_documents WHERE deleted_at IS NOT NULL")
        .execute()
        .map(rows -> rows.rowCount());
  }
}
