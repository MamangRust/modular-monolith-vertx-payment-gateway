package io.example.merchant.repository.impl;

import io.example.merchant.domain.requests.merchant.MonthYearPaymentMethodApiKey;
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
class MerchantStatsMethodByApiKeyRepositoryImplTest {
  @Mock private Pool pool; @Mock private PreparedQuery<RowSet<Row>> pq;
  @Mock private RowSet<Row> rs; @Mock private RowIterator<Row> it; @Mock private Row row;
  private MerchantStatsMethodByApiKeyRepositoryImpl repo;
  private MonthYearPaymentMethodApiKey req = MonthYearPaymentMethodApiKey.builder().apikey("key123").year(2026).build();
  @BeforeEach void setUp() { repo = new MerchantStatsMethodByApiKeyRepositoryImpl(pool); }
  void m() { when(pool.preparedQuery(anyString())).thenReturn(pq); }

  @Test void getMonthlyPaymentMethodByApikey(VertxTestContext ctx) {
    m();
    when(row.getValue("month")).thenReturn("Jan"); when(row.getString("payment_method")).thenReturn("CC"); when(row.getLong("amount")).thenReturn(500L);
    when(rs.iterator()).thenReturn(it); when(it.hasNext()).thenReturn(true, false); when(it.next()).thenReturn(row);
    when(pq.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rs));
    repo.getMonthlyPaymentMethodByApikey(req).onComplete(ctx.succeeding(l -> ctx.verify(() -> { assertThat(l).hasSize(1); ctx.completeNow(); })));
  }

  @Test void getYearlyPaymentMethodByApikey(VertxTestContext ctx) {
    m();
    when(row.getValue("year")).thenReturn("2025"); when(row.getString("payment_method")).thenReturn("CC"); when(row.getLong("amount")).thenReturn(6000L);
    when(rs.iterator()).thenReturn(it); when(it.hasNext()).thenReturn(true, false); when(it.next()).thenReturn(row);
    when(pq.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rs));
    repo.getYearlyPaymentMethodByApikey(req).onComplete(ctx.succeeding(l -> ctx.verify(() -> { assertThat(l).hasSize(1); ctx.completeNow(); })));
  }
}
