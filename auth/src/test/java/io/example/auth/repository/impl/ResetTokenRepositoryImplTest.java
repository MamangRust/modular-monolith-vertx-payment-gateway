package io.example.auth.repository.impl;

import io.example.auth.model.ResetToken;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class ResetTokenRepositoryImplTest {

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

  private ResetTokenRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new ResetTokenRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void mockTokenRow() {
    var now = LocalDateTime.of(2026, 6, 26, 10, 0, 0);
    when(row.getInteger("user_id")).thenReturn(1);
    when(row.getString("token")).thenReturn("reset-token-value");
    when(row.getLocalDateTime("expiry_date")).thenReturn(now.plusHours(24));
    when(rowSet.iterator()).thenReturn(iterator);
  }

  private void stubSingleRow() {
    when(iterator.hasNext()).thenReturn(true);
    when(iterator.next()).thenReturn(row);
  }

  private void stubNoRows() {
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(false);
  }

  /* ─── findByToken ─── */

  @Test
  @DisplayName("findByToken returns token when found")
  void findByTokenFound(VertxTestContext ctx) {
    mockPool();
    mockTokenRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByToken("reset-token-value")
        .onComplete(ctx.succeeding(rt -> ctx.verify(() -> {
          assertThat(rt).isNotNull();
          assertThat(rt.getToken()).isEqualTo("reset-token-value");
          assertThat(rt.getUserId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByToken returns null when not found")
  void findByTokenNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByToken("nonexistent")
        .onComplete(ctx.succeeding(rt -> ctx.verify(() -> {
          assertThat(rt).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── createResetToken ─── */

  @Test
  @DisplayName("createResetToken inserts and returns token")
  void createResetTokenSuccess(VertxTestContext ctx) {
    mockPool();
    mockTokenRow();
    // createResetToken does not check hasNext() — directly calls next()
    when(iterator.next()).thenReturn(row);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.createResetToken(1, "new-reset-token", LocalDateTime.now().plusHours(24))
        .onComplete(ctx.succeeding(rt -> ctx.verify(() -> {
          assertThat(rt).isNotNull();
          assertThat(rt.getToken()).isEqualTo("reset-token-value");
          ctx.completeNow();
        })));
  }

  /* ─── deleteResetToken ─── */

  @Test
  @DisplayName("deleteResetToken deletes by userId")
  void deleteResetTokenSuccess(VertxTestContext ctx) {
    mockPool();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.deleteResetToken(1)
        .onComplete(ctx.succeeding(v -> ctx.verify(ctx::completeNow)));
  }
}
