package io.example.card.repository.impl;

import io.example.card.domain.requests.MonthYearCardNumberCard;
import io.example.card.model.CardStats;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class CardStatsBalanceByCardRepositoryImplTest {

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

  private CardStatsBalanceByCardRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new CardStatsBalanceByCardRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private MonthYearCardNumberCard aReq() {
    return MonthYearCardNumberCard.builder().year(2026).cardNumber("4111111111111111").build();
  }

  /* ─── getMonthlyBalancesByCardNumber ─── */

  @Test
  @DisplayName("getMonthlyBalancesByCardNumber returns balances filtered by card number")
  void getMonthlyBalancesByCardNumber(VertxTestContext ctx) {
    mockPool();

    when(row.getInteger("month")).thenReturn(3);
    when(row.getLong("balance")).thenReturn(5000L);

    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getMonthlyBalancesByCardNumber(aReq())
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getMonth()).isEqualTo(3);
          assertThat(list.get(0).getBalance()).isEqualTo(5000L);
          ctx.completeNow();
        })));
  }

  /* ─── getYearlyBalancesByCardNumber ─── */

  @Test
  @DisplayName("getYearlyBalancesByCardNumber returns yearly balances filtered by card number")
  void getYearlyBalancesByCardNumber(VertxTestContext ctx) {
    mockPool();

    when(row.getInteger("year")).thenReturn(2025);
    when(row.getLong("balance")).thenReturn(8000L);

    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getYearlyBalancesByCardNumber(aReq())
        .onComplete(ctx.succeeding(list -> ctx.verify(() -> {
          assertThat(list).hasSize(1);
          assertThat(list.get(0).getYear()).isEqualTo(2025);
          assertThat(list.get(0).getBalance()).isEqualTo(8000L);
          ctx.completeNow();
        })));
  }
}
