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
import pb.merchant.MerchantCommand.CreateMerchantRequest;
import pb.merchant.MerchantCommand.UpdateMerchantRequest;
import pb.merchant.MerchantCommand.UpdateMerchantStatusRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class MerchantCommandRepositoryImplTest {

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

  private MerchantCommandRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new MerchantCommandRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void mockMerchantRow() {
    when(row.getInteger("id")).thenReturn(10);
    when(row.getString("name")).thenReturn("Test Merchant");
    when(row.getString("api_key")).thenReturn("key_10");
    when(row.getInteger("user_id")).thenReturn(42);
    when(row.getString("status")).thenReturn("pending");
    when(row.get(LocalDateTime.class, "created_at")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.get(LocalDateTime.class, "updated_at")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.get(LocalDateTime.class, "deleted_at")).thenReturn(null);
  }

  private void stubRowsPresent() {
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true);
    when(iterator.next()).thenReturn(row);
  }

  private void stubDeleteResult(int count) {
    when(rowSet.rowCount()).thenReturn(count);
  }

  @Test
  @DisplayName("createMerchant success")
  void createSuccess(VertxTestContext ctx) {
    mockPool();
    mockMerchantRow();
    stubRowsPresent();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = CreateMerchantRequest.newBuilder().setName("Test Merchant").setUserId(42).build();
    repo.createMerchant(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).isNotNull();
          assertThat(res.getId()).isEqualTo(10);
          assertThat(res.getName()).isEqualTo("Test Merchant");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("updateMerchant success")
  void updateSuccess(VertxTestContext ctx) {
    mockPool();
    mockMerchantRow();
    stubRowsPresent();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = UpdateMerchantRequest.newBuilder().setMerchantId(10).setName("Updated").setUserId(42).setStatus("active").build();
    repo.updateMerchant(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).isNotNull();
          assertThat(res.getId()).isEqualTo(10);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("updateMerchantStatus success")
  void updateStatusSuccess(VertxTestContext ctx) {
    mockPool();
    mockMerchantRow();
    stubRowsPresent();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = UpdateMerchantStatusRequest.newBuilder().setMerchantId(10).setStatus("active").build();
    repo.updateMerchantStatus(req)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).isNotNull();
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("trashedMerchant success")
  void trashedSuccess(VertxTestContext ctx) {
    mockPool();
    mockMerchantRow();
    stubRowsPresent();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.trashedMerchant(10)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).isNotNull();
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("restoreMerchant success")
  void restoreSuccess(VertxTestContext ctx) {
    mockPool();
    mockMerchantRow();
    stubRowsPresent();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.restoreMerchant(10)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).isNotNull();
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("deleteMerchantPermanent success")
  void deletePermanentSuccess(VertxTestContext ctx) {
    mockPool();
    stubDeleteResult(1);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.deleteMerchantPermanent(10)
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).isTrue();
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("restoreAllMerchants success")
  void restoreAllSuccess(VertxTestContext ctx) {
    when(pool.query(anyString())).thenReturn(preparedQuery);
    stubDeleteResult(5);
    when(preparedQuery.execute()).thenReturn(Future.succeededFuture(rowSet));

    repo.restoreAllMerchants()
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).isEqualTo(5);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("deleteAllMerchantsPermanent success")
  void deleteAllPermanentSuccess(VertxTestContext ctx) {
    when(pool.query(anyString())).thenReturn(preparedQuery);
    stubDeleteResult(3);
    when(preparedQuery.execute()).thenReturn(Future.succeededFuture(rowSet));

    repo.deleteAllMerchantsPermanent()
        .onComplete(ctx.succeeding(res -> ctx.verify(() -> {
          assertThat(res).isEqualTo(3);
          ctx.completeNow();
        })));
  }
}
