package io.example.merchant.repository.impl;

import io.example.merchant.model.MerchantTransactions;
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
import pb.merchant.Merchant.FindAllMerchantTransaction;
import pb.merchant.Merchant.FindAllMerchantTransactionApikey;
import pb.merchant.Merchant.FindAllMerchantTransactionId;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({ MockitoExtension.class, VertxExtension.class })
class MerchantTransactionRepositoryImplTest {

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

  private MerchantTransactionRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new MerchantTransactionRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void mockTxnRow() {
    when(row.getInteger("transaction_id")).thenReturn(100);
    when(row.getString("card_number")).thenReturn("1234");
    when(row.getLong("amount")).thenReturn(1000L);
    when(row.getString("payment_method")).thenReturn("CREDIT");
    when(row.getInteger("merchant_id")).thenReturn(10);
    when(row.getString("merchant_name")).thenReturn("Merchant A");
    when(row.get(LocalDateTime.class, "transaction_time")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.get(LocalDateTime.class, "created_at")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.get(LocalDateTime.class, "updated_at")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.get(LocalDateTime.class, "deleted_at")).thenReturn(null);
    when(row.getInteger("total_count")).thenReturn(1);
    when(rowSet.iterator()).thenReturn(iterator);
  }

  @Test
  @DisplayName("findAllTransactionMerchant success")
  void findAllSuccess(VertxTestContext ctx) {
    mockPool();
    mockTxnRow();
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = FindAllMerchantTransaction.newBuilder().setPage(1).setPageSize(10).build();
    repo.findAllTransactionMerchant(req)
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res.getData()).hasSize(1);
          assertThat(res.getTotalRecords()).isEqualTo(1);
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }

  @Test
  @DisplayName("findAllTransactionByMerchant success")
  void findAllByMerchantSuccess(VertxTestContext ctx) {
    mockPool();
    mockTxnRow();
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = FindAllMerchantTransactionId.newBuilder().setId(10).setPage(1).setPageSize(10).build();
    repo.findAllTransactionByMerchant(req)
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res.getData()).hasSize(1);
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }

  @Test
  @DisplayName("findAllTransactionByApikey success")
  void findAllByApikeySuccess(VertxTestContext ctx) {
    mockPool();
    mockTxnRow();
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);
    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = FindAllMerchantTransactionApikey.newBuilder().setApiKey("key_10").setPage(1).setPageSize(10).build();
    repo.findAllTransactionByApikey(req)
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res.getData()).hasSize(1);
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }
}
