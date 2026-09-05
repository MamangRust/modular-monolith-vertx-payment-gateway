package io.example.card.repository.impl;

import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Query;
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
class CardDashboardTopupRepositoryImplTest {
  @Mock private Pool pool; @Mock private Query<RowSet<Row>> query;
  @Mock private PreparedQuery<RowSet<Row>> pq;
  @Mock private RowSet<Row> rs; @Mock private RowIterator<Row> it; @Mock private Row row;
  private CardDashboardTopupRepositoryImpl repo;
  @BeforeEach void setUp() { repo = new CardDashboardTopupRepositoryImpl(pool); }

  @Test void getTotalTopAmount(VertxTestContext ctx) {
    when(pool.query(anyString())).thenReturn(query);
    when(query.execute()).thenReturn(Future.succeededFuture(rs));
    when(rs.iterator()).thenReturn(it); when(it.hasNext()).thenReturn(true); when(it.next()).thenReturn(row);
    when(row.getLong(0)).thenReturn(50000L);
    repo.getTotalTopAmount().onComplete(ctx.succeeding(v -> ctx.verify(() -> { assertThat(v).isEqualTo(50000L); ctx.completeNow(); })));
  }

  @Test void getTotalTopupAmountByCardNumber(VertxTestContext ctx) {
    when(pool.preparedQuery(anyString())).thenReturn(pq);
    when(pq.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rs));
    when(rs.iterator()).thenReturn(it); when(it.hasNext()).thenReturn(true); when(it.next()).thenReturn(row);
    when(row.getLong(0)).thenReturn(10000L);
    repo.getTotalTopupAmountByCardNumber("4111").onComplete(ctx.succeeding(v -> ctx.verify(() -> { assertThat(v).isEqualTo(10000L); ctx.completeNow(); })));
  }
}
