package io.example.merchant.repository.impl;

import io.example.merchant.model.Merchant;
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
import pb.merchant.Merchant.FindAllMerchantRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class MerchantQueryRepositoryImplTest {

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

  private MerchantQueryRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new MerchantQueryRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void mockMerchantRow() {
    when(row.getInteger("id")).thenReturn(10);
    when(row.getInteger("merchant_id")).thenReturn(10);
    when(row.getString("name")).thenReturn("Test Merchant");
    when(row.getString("api_key")).thenReturn("key_10");
    when(row.getInteger("user_id")).thenReturn(42);
    when(row.getString("status")).thenReturn("pending");
    when(row.get(LocalDateTime.class, "created_at")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.get(LocalDateTime.class, "updated_at")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.get(LocalDateTime.class, "deleted_at")).thenReturn(null);
    when(row.getInteger("total_count")).thenReturn(1);
    when(rowSet.iterator()).thenReturn(iterator);
  }

  @Test
  @DisplayName("findAllMerchants success")
  void findAllSuccess(VertxTestContext ctx) {
    mockPool();
    mockMerchantRow();
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = FindAllMerchantRequest.newBuilder().setPage(1).setPageSize(10).build();
    repo.findAllMerchants(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getData()).hasSize(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByActive success")
  void findByActiveSuccess(VertxTestContext ctx) {
    mockPool();
    mockMerchantRow();
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = FindAllMerchantRequest.newBuilder().setPage(1).setPageSize(10).build();
    repo.findByActive(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getData()).hasSize(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByTrashed success")
  void findByTrashedSuccess(VertxTestContext ctx) {
    mockPool();
    mockMerchantRow();
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = FindAllMerchantRequest.newBuilder().setPage(1).setPageSize(10).build();
    repo.findByTrashed(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res.getData()).hasSize(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByApiKey success")
  void findByApiKeySuccess(VertxTestContext ctx) {
    mockPool();
    mockMerchantRow();
    when(iterator.hasNext()).thenReturn(true);
    when(iterator.next()).thenReturn(row);
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByApiKey("key_10")
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).isNotNull();
          assertThat(res.getId()).isEqualTo(10);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByMerchantId success")
  void findByMerchantIdSuccess(VertxTestContext ctx) {
    mockPool();
    mockMerchantRow();
    when(iterator.hasNext()).thenReturn(true);
    when(iterator.next()).thenReturn(row);
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByMerchantId(10)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).isNotNull();
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByName success")
  void findByNameSuccess(VertxTestContext ctx) {
    mockPool();
    mockMerchantRow();
    when(iterator.hasNext()).thenReturn(true);
    when(iterator.next()).thenReturn(row);
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByName("Test Merchant")
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).isNotNull();
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByTrashedById success")
  void findByTrashedByIdSuccess(VertxTestContext ctx) {
    mockPool();
    mockMerchantRow();
    when(iterator.hasNext()).thenReturn(true);
    when(iterator.next()).thenReturn(row);
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByTrashedById(10)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).isNotNull();
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByRestoredById success")
  void findByRestoredByIdSuccess(VertxTestContext ctx) {
    mockPool();
    mockMerchantRow();
    when(iterator.hasNext()).thenReturn(true);
    when(iterator.next()).thenReturn(row);
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByRestoredById(10)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).isNotNull();
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByMerchantUserId success")
  void findByMerchantUserIdSuccess(VertxTestContext ctx) {
    mockPool();
    mockMerchantRow();
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByMerchantUserId(42)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).hasSize(1);
          ctx.completeNow();
        })));
  }
}
