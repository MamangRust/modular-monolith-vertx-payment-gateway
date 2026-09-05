package io.example.merchant.repository.impl;

import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pb.merchant_document.MerchantDocumentCommand.CreateMerchantDocumentRequest;
import pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentRequest;
import pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentStatusRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class MerchantDocumentCommandRepositoryImplTest {

  @Mock
  private Pool pool;

  @Mock
  private PreparedQuery<RowSet<Row>> preparedQuery;

  @Mock
  private RowSet<Row> rowSet;

  @Mock
  private RowIterator<Row> iterator;

  @Mock
  private Row row;

  private MerchantDocumentCommandRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new MerchantDocumentCommandRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void mockDocRow() {
    when(row.getInteger("id")).thenReturn(100);
    when(row.getInteger("document_id")).thenReturn(100);
    when(row.getInteger("merchant_id")).thenReturn(10);
    when(row.getString("document_type")).thenReturn("ID_CARD");
    when(row.getString("document_url")).thenReturn("http://url");
    when(row.getString("status")).thenReturn("pending");
    when(row.getString("note")).thenReturn("some note");
    when(row.get(LocalDateTime.class, "created_at")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.get(LocalDateTime.class, "updated_at")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.get(LocalDateTime.class, "deleted_at")).thenReturn(null);
    when(rowSet.iterator()).thenReturn(iterator);
  }

  private void stubSingleRow() {
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);
  }

  @Test
  @DisplayName("createMerchantDocument success")
  void createSuccess(VertxTestContext ctx) {
    mockPool();
    mockDocRow();
    stubSingleRow();
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = CreateMerchantDocumentRequest.newBuilder()
        .setMerchantId(10).setDocumentType("ID_CARD").setDocumentUrl("http://url").build();
    repo.createMerchantDocument(req)
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res).isNotNull();
          assertThat(res.getId()).isEqualTo(100);
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }

  @Test
  @DisplayName("updateMerchantDocument success")
  void updateSuccess(VertxTestContext ctx) {
    mockPool();
    mockDocRow();
    stubSingleRow();
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = UpdateMerchantDocumentRequest.newBuilder()
        .setDocumentId(100).setDocumentType("ID_CARD").setDocumentUrl("http://url")
        .setStatus("pending").setNote("some note").build();
    repo.updateMerchantDocument(req)
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res).isNotNull();
          assertThat(res.getId()).isEqualTo(100);
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }

  @Test
  @DisplayName("updateMerchantDocumentStatus success")
  void updateStatusSuccess(VertxTestContext ctx) {
    mockPool();
    mockDocRow();
    stubSingleRow();
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = UpdateMerchantDocumentStatusRequest.newBuilder()
        .setDocumentId(100).setStatus("approved").setNote("some note").build();
    repo.updateMerchantDocumentStatus(req)
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res).isNotNull();
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }

  @Test
  @DisplayName("trashedMerchantDocument success")
  void trashedSuccess(VertxTestContext ctx) {
    mockPool();
    mockDocRow();
    stubSingleRow();
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.trashedMerchantDocument(100)
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res).isNotNull();
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }

  @Test
  @DisplayName("restoreMerchantDocument success")
  void restoreSuccess(VertxTestContext ctx) {
    mockPool();
    mockDocRow();
    stubSingleRow();
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.restoreMerchantDocument(100)
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res).isNotNull();
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }

  @Test
  @DisplayName("deleteMerchantDocumentPermanent success")
  void deletePermanentSuccess(VertxTestContext ctx) {
    mockPool();
    when(rowSet.rowCount()).thenReturn(1);
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.deleteMerchantDocumentPermanent(100)
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res).isTrue();
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }

  @Test
  @DisplayName("restoreAllMerchantDocuments success")
  void restoreAllSuccess(VertxTestContext ctx) {
    when(pool.query(anyString())).thenReturn(preparedQuery);
    when(rowSet.rowCount()).thenReturn(5);
    when(preparedQuery.execute()).thenReturn(Future.succeededFuture(rowSet));

    repo.restoreAllMerchantDocuments()
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res).isEqualTo(5);
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }

  @Test
  @DisplayName("deleteAllMerchantDocumentsPermanent success")
  void deleteAllPermanentSuccess(VertxTestContext ctx) {
    when(pool.query(anyString())).thenReturn(preparedQuery);
    when(rowSet.rowCount()).thenReturn(3);
    when(preparedQuery.execute()).thenReturn(Future.succeededFuture(rowSet));

    repo.deleteAllMerchantDocumentsPermanent()
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res).isEqualTo(3);
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }
}