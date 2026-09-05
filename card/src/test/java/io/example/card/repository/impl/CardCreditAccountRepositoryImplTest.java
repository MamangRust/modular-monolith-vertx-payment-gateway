package io.example.card.repository.impl;

import io.example.card.model.CardCreditAccount;
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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class CardCreditAccountRepositoryImplTest {

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

  private CardCreditAccountRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new CardCreditAccountRepositoryImpl(pool);
  }

  private void mockPool() {
    lenient().when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void mockAccountRow() {
    lenient().when(row.getInteger("account_id")).thenReturn(1);
    lenient().when(row.getString("card_number")).thenReturn("4111111111111111");
    lenient().when(row.getLong("credit_limit")).thenReturn(10000000L);
    lenient().when(row.getLong("used_credit")).thenReturn(2000000L);
    lenient().when(row.getLong("available_credit")).thenReturn(8000000L);
    lenient().when(row.getInteger("billing_cycle_day")).thenReturn(15);
    lenient().when(row.getInteger("payment_due_days")).thenReturn(20);
    lenient().when(row.getInteger("annual_rate_bps")).thenReturn(1800);
    lenient().when(row.getString("status")).thenReturn("ACTIVE");
    lenient().when(row.getLocalDate("last_statement_date")).thenReturn(LocalDate.of(2026, 5, 15));
    lenient().when(row.getLocalDate("next_statement_date")).thenReturn(LocalDate.of(2026, 6, 15));
    lenient().when(row.getInteger("delinquency_bucket")).thenReturn(0);
    lenient().when(row.getInteger("dpd")).thenReturn(0);
    lenient().when(rowSet.iterator()).thenReturn(iterator);
  }

  private void stubSingleRow() {
    lenient().when(iterator.hasNext()).thenReturn(true, false);
    lenient().when(iterator.next()).thenReturn(row);
    lenient().doAnswer(invocation -> {
      java.util.function.Consumer<Row> consumer = invocation.getArgument(0);
      consumer.accept(row);
      return null;
    }).when(rowSet).forEach(any(java.util.function.Consumer.class));
  }

  private void stubNoRows() {
    lenient().when(rowSet.iterator()).thenReturn(iterator);
    lenient().when(iterator.hasNext()).thenReturn(false);
  }

  @Test
  @DisplayName("findByCardNumber returns credit account if found")
  void findByCardNumberSuccess(VertxTestContext ctx) {
    mockPool();
    mockAccountRow();
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByCardNumber("4111111111111111")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getCardNumber()).isEqualTo("4111111111111111");
          assertThat(result.getAvailableCredit()).isEqualTo(8000000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("createAccount inserts and returns new credit account")
  void createAccountSuccess(VertxTestContext ctx) {
    mockPool();
    mockAccountRow();
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.createAccount("4111111111111111", 10000000L, 15, 1800)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getCardNumber()).isEqualTo("4111111111111111");
          assertThat(result.getCreditLimit()).isEqualTo(10000000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("decrementAvailableCredit updates used credit and returns account")
  void decrementAvailableCreditSuccess(VertxTestContext ctx) {
    mockPool();
    mockAccountRow();
    lenient().when(row.getLong("used_credit")).thenReturn(3000000L);
    lenient().when(row.getLong("available_credit")).thenReturn(7000000L);
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.decrementAvailableCredit("4111111111111111", 1000000L)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getUsedCredit()).isEqualTo(3000000L);
          assertThat(result.getAvailableCredit()).isEqualTo(7000000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("releaseCredit decreases used credit and returns account")
  void releaseCreditSuccess(VertxTestContext ctx) {
    mockPool();
    mockAccountRow();
    lenient().when(row.getLong("used_credit")).thenReturn(1000000L);
    lenient().when(row.getLong("available_credit")).thenReturn(9000000L);
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.releaseCredit("4111111111111111", 1000000L)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getUsedCredit()).isEqualTo(1000000L);
          assertThat(result.getAvailableCredit()).isEqualTo(9000000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("updateStatus updates account status and returns account")
  void updateStatusSuccess(VertxTestContext ctx) {
    mockPool();
    mockAccountRow();
    lenient().when(row.getString("status")).thenReturn("SUSPENDED");
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.updateStatus("4111111111111111", "SUSPENDED")
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getStatus()).isEqualTo("SUSPENDED");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findAccountsDueForBilling returns due accounts")
  void findAccountsDueForBillingSuccess(VertxTestContext ctx) {
    mockPool();
    mockAccountRow();
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findAccountsDueForBilling(15)
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getBillingCycleDay()).isEqualTo(15);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("setCreditLimit updates credit limit and returns account")
  void setCreditLimitSuccess(VertxTestContext ctx) {
    mockPool();
    mockAccountRow();
    lenient().when(row.getLong("credit_limit")).thenReturn(15000000L);
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.setCreditLimit("4111111111111111", 15000000L)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getCreditLimit()).isEqualTo(15000000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("adjustCreditLimit adjusts credit limit and returns account")
  void adjustCreditLimitSuccess(VertxTestContext ctx) {
    mockPool();
    mockAccountRow();
    lenient().when(row.getLong("credit_limit")).thenReturn(12000000L);
    stubSingleRow();

    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.adjustCreditLimit("4111111111111111", 2000000L)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getCreditLimit()).isEqualTo(12000000L);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("deleteByCardNumber deletes credit account and returns true if deleted")
  void deleteByCardNumberSuccess(VertxTestContext ctx) {
    mockPool();
    lenient().when(rowSet.rowCount()).thenReturn(1);
    lenient().when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.deleteByCardNumber("4111111111111111")
        .onComplete(ctx.succeeding(deleted -> ctx.verify(() -> {
          assertThat(deleted).isTrue();
          ctx.completeNow();
        })));
  }
}
