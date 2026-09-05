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
import pb.merchant_document.MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class MerchantDocumentQueryRepositoryImplTest {

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

  private MerchantDocumentQueryRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new MerchantDocumentQueryRepositoryImpl(pool);
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
    when(row.getInteger("total_count")).thenReturn(1);
    when(rowSet.iterator()).thenReturn(iterator);
  }

  @Test
  @DisplayName("findAllDocuments success")
  void findAllSuccess(VertxTestContext ctx) {
    mockPool();
    mockDocRow();
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = FindAllMerchantDocumentsRequest.newBuilder().setPage(1).setPageSize(10).build();
    repo.findAllDocuments(req)
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res.getData()).hasSize(1);
          assertThat(res.getTotalRecords()).isEqualTo(1);
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }

  @Test
  @DisplayName("findByIdDocument success")
  void findByIdSuccess(VertxTestContext ctx) {
    mockPool();
    mockDocRow();
    when(iterator.hasNext()).thenReturn(true);
    when(iterator.next()).thenReturn(row);
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByIdDocument(100)
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res).isNotNull();
          assertThat(res.getId()).isEqualTo(100);
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }

  @Test
  @DisplayName("findByTrashedByIdDocument success")
  void findByTrashedByIdSuccess(VertxTestContext ctx) {
    mockPool();
    mockDocRow();
    when(iterator.hasNext()).thenReturn(true);
    when(iterator.next()).thenReturn(row);
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByTrashedByIdDocument(100)
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res).isNotNull();
          assertThat(res.getId()).isEqualTo(100);
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }

  @Test
  @DisplayName("findByActiveDocuments success")
  void findActiveSuccess(VertxTestContext ctx) {
    mockPool();
    mockDocRow();
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = FindAllMerchantDocumentsRequest.newBuilder().setPage(1).setPageSize(10).build();
    repo.findByActiveDocuments(req)
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res.getData()).hasSize(1);
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }

  @Test
  @DisplayName("findByTrashedDocuments success")
  void findTrashedSuccess(VertxTestContext ctx) {
    mockPool();
    mockDocRow();
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = FindAllMerchantDocumentsRequest.newBuilder().setPage(1).setPageSize(10).build();
    repo.findByTrashedDocuments(req)
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res.getData()).hasSize(1);
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }
}
