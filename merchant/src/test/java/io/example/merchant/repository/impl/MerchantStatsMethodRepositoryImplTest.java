package io.example.merchant.repository.impl;

import io.example.merchant.model.MerchantStats;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class MerchantStatsMethodRepositoryImplTest {
  @Mock private Pool pool; @Mock private PreparedQuery<RowSet<Row>> pq;
  @Mock private RowSet<Row> rs; @Mock private RowIterator<Row> it; @Mock private Row row;
  private MerchantStatsMethodRepositoryImpl repo;
  @BeforeEach void setUp() { repo = new MerchantStatsMethodRepositoryImpl(pool); }
  void m() { when(pool.preparedQuery(anyString())).thenReturn(pq); }

  @Test void getMonthlyPaymentMethodsMerchant(VertxTestContext ctx) {
    m();
    when(row.getValue("month")).thenReturn("Jan"); when(row.getString("payment_method")).thenReturn("CC"); when(row.getLong("amount")).thenReturn(1000L);
    when(rs.iterator()).thenReturn(it); when(it.hasNext()).thenReturn(true, false); when(it.next()).thenReturn(row);
    when(pq.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rs));
    repo.getMonthlyPaymentMethodsMerchant(2026).onComplete(ctx.succeeding(l -> ctx.verify(() -> { assertThat(l).hasSize(1); ctx.completeNow(); })));
  }

  @Test void getYearlyPaymentMethodMerchant(VertxTestContext ctx) {
    m();
    when(row.getValue("year")).thenReturn("2025"); when(row.getString("payment_method")).thenReturn("CC"); when(row.getLong("amount")).thenReturn(12000L);
    when(rs.iterator()).thenReturn(it); when(it.hasNext()).thenReturn(true, false); when(it.next()).thenReturn(row);
    when(pq.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rs));
    repo.getYearlyPaymentMethodMerchant(2026).onComplete(ctx.succeeding(l -> ctx.verify(() -> { assertThat(l).hasSize(1); ctx.completeNow(); })));
  }
}
