package io.example.transaction.repository.impl;

import io.example.common.domain.PagedResult;
import io.example.transaction.domain.requests.FindAllTransactionCardNumber;
import io.example.transaction.domain.requests.FindAllTransactions;
import io.example.transaction.model.Transaction;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionQueryRepositoryImplTest {

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

  private TransactionQueryRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new TransactionQueryRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void mockTransactionRow() {
    when(row.getInteger("transaction_id")).thenReturn(1);
    when(row.getInteger("id")).thenReturn(null);
    when(row.getUUID("transaction_no")).thenReturn(UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890"));
    when(row.getString("transaction_no")).thenReturn(null);
    when(row.getString("card_number")).thenReturn("4111111111111111");
    when(row.getLong("amount")).thenReturn(50000L);
    when(row.getString("payment_method")).thenReturn("credit_card");
    when(row.getInteger("merchant_id")).thenReturn(42);
    when(row.getString("status")).thenReturn("pending");
    when(row.getLocalDateTime("transaction_time")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.getLocalDateTime("created_at")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.getLocalDateTime("updated_at")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.getLocalDateTime("deleted_at")).thenReturn(null);
  }

  private void mockTransactionRowWithCount() {
    mockTransactionRow();
    when(row.getValue("total_count")).thenReturn(1);
  }

  private void stubPagedRows() {
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);
  }

  private void stubSingleRow() {
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true);
    when(iterator.next()).thenReturn(row);
  }

  private void stubNoRows() {
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(false);
  }

  /* ─── getTransactions ─── */

  @Test
  @DisplayName("getTransactions returns paged results when found")
  void getTransactionsFound(VertxTestContext ctx) {
    mockPool();
    mockTransactionRowWithCount();
    stubPagedRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = FindAllTransactions.builder().page(1).pageSize(10).build();

    repo.getTransactions(req)
        .onComplete(ctx.succeeding(page -> ctx.verify(() -> {
          assertThat(page).isNotNull();
          assertThat(page.getData()).hasSize(1);
          assertThat(page.getTotalRecords()).isEqualTo(1);
          assertThat(page.getData().get(0).getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getTransactions returns empty paged result when no transactions")
  void getTransactionsEmpty(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = FindAllTransactions.builder().page(1).pageSize(10).build();

    repo.getTransactions(req)
        .onComplete(ctx.succeeding(page -> ctx.verify(() -> {
          assertThat(page).isNotNull();
          assertThat(page.getData()).isEmpty();
          assertThat(page.getTotalRecords()).isEqualTo(0);
          ctx.completeNow();
        })));
  }

  /* ─── getTransactionsByCardNumber ─── */

  @Test
  @DisplayName("getTransactionsByCardNumber returns paged results when found")
  void getTransactionsByCardNumberFound(VertxTestContext ctx) {
    mockPool();
    mockTransactionRowWithCount();
    stubPagedRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = FindAllTransactionCardNumber.builder()
        .cardNumber("4111111111111111")
        .page(1).pageSize(10)
        .build();

    repo.getTransactionsByCardNumber(req)
        .onComplete(ctx.succeeding(page -> ctx.verify(() -> {
          assertThat(page).isNotNull();
          assertThat(page.getData()).hasSize(1);
          assertThat(page.getTotalRecords()).isEqualTo(1);
          assertThat(page.getData().get(0).getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  /* ─── getTransactionById ─── */

  @Test
  @DisplayName("getTransactionById returns transaction when found")
  void getTransactionByIdFound(VertxTestContext ctx) {
    mockPool();
    mockTransactionRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getTransactionById(1)
        .onComplete(ctx.succeeding(txn -> ctx.verify(() -> {
          assertThat(txn).isNotNull();
          assertThat(txn.getId()).isEqualTo(1);
          assertThat(txn.getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getTransactionById returns null when not found")
  void getTransactionByIdNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getTransactionById(99)
        .onComplete(ctx.succeeding(txn -> ctx.verify(() -> {
          assertThat(txn).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── getTransactionsByMerchantId ─── */

  @Test
  @DisplayName("getTransactionsByMerchantId returns paged results when found")
  void getTransactionsByMerchantIdFound(VertxTestContext ctx) {
    mockPool();
    mockTransactionRowWithCount();
    stubPagedRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getTransactionsByMerchantId(42)
        .onComplete(ctx.succeeding(page -> ctx.verify(() -> {
          assertThat(page).isNotNull();
          assertThat(page.getData()).hasSize(1);
          assertThat(page.getTotalRecords()).isEqualTo(1);
          page.getData().forEach(txn -> assertThat(txn.getMerchantId()).isEqualTo(42));
          ctx.completeNow();
        })));
  }

  /* ─── findByTrashed ─── */

  @Test
  @DisplayName("findByTrashed returns trashed transaction when found")
  void findByTrashedFound(VertxTestContext ctx) {
    mockPool();
    mockTransactionRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByTrashed(1)
        .onComplete(ctx.succeeding(txn -> ctx.verify(() -> {
          assertThat(txn).isNotNull();
          assertThat(txn.getId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByTrashed returns null when not found")
  void findByTrashedNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByTrashed(99)
        .onComplete(ctx.succeeding(txn -> ctx.verify(() -> {
          assertThat(txn).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── getActiveTransactions ─── */

  @Test
  @DisplayName("getActiveTransactions returns paged results when found")
  void getActiveTransactionsFound(VertxTestContext ctx) {
    mockPool();
    mockTransactionRowWithCount();
    stubPagedRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = FindAllTransactions.builder().page(1).pageSize(10).build();

    repo.getActiveTransactions(req)
        .onComplete(ctx.succeeding(page -> ctx.verify(() -> {
          assertThat(page).isNotNull();
          assertThat(page.getData()).hasSize(1);
          assertThat(page.getTotalRecords()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── getTrashedTransactions ─── */

  @Test
  @DisplayName("getTrashedTransactions returns paged results when found")
  void getTrashedTransactionsFound(VertxTestContext ctx) {
    mockPool();
    mockTransactionRowWithCount();
    stubPagedRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = FindAllTransactions.builder().page(1).pageSize(10).build();

    repo.getTrashedTransactions(req)
        .onComplete(ctx.succeeding(page -> ctx.verify(() -> {
          assertThat(page).isNotNull();
          assertThat(page.getData()).hasSize(1);
          assertThat(page.getTotalRecords()).isEqualTo(1);
          ctx.completeNow();
        })));
  }
}
