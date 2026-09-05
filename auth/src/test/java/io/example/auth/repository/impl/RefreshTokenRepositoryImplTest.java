package io.example.auth.repository.impl;

import io.example.auth.model.RefreshToken;
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
class RefreshTokenRepositoryImplTest {

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

  private RefreshTokenRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new RefreshTokenRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void mockTokenRow() {
    var now = LocalDateTime.of(2026, 6, 26, 10, 0, 0);
    when(row.getInteger("refresh_token_id")).thenReturn(10);
    when(row.getInteger("user_id")).thenReturn(1);
    when(row.getString("token")).thenReturn("refresh-token-value");
    when(row.getLocalDateTime("expiration")).thenReturn(now.plusHours(24));
    when(row.getLocalDateTime("created_at")).thenReturn(now);
    when(row.getLocalDateTime("updated_at")).thenReturn(now);
    when(row.getLocalDateTime("deleted_at")).thenReturn(null);
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

    repo.findByToken("refresh-token-value")
        .onComplete(ctx.succeeding(rt -> ctx.verify(() -> {
          assertThat(rt).isNotNull();
          assertThat(rt.getToken()).isEqualTo("refresh-token-value");
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

  /* ─── findByUserId ─── */

  @Test
  @DisplayName("findByUserId returns latest token for user")
  void findByUserIdFound(VertxTestContext ctx) {
    mockPool();
    mockTokenRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByUserId(1)
        .onComplete(ctx.succeeding(rt -> ctx.verify(() -> {
          assertThat(rt).isNotNull();
          assertThat(rt.getUserId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── createRefreshToken ─── */

  @Test
  @DisplayName("createRefreshToken inserts and returns token")
  void createRefreshTokenSuccess(VertxTestContext ctx) {
    mockPool();
    mockTokenRow();
    // createRefreshToken does not check hasNext() — directly calls next()
    when(iterator.next()).thenReturn(row);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.createRefreshToken(1, "new-token", LocalDateTime.now().plusHours(24))
        .onComplete(ctx.succeeding(rt -> ctx.verify(() -> {
          assertThat(rt).isNotNull();
          assertThat(rt.getToken()).isEqualTo("refresh-token-value");
          ctx.completeNow();
        })));
  }

  /* ─── updateRefreshToken ─── */

  @Test
  @DisplayName("updateRefreshToken updates and returns token")
  void updateRefreshTokenSuccess(VertxTestContext ctx) {
    mockPool();
    mockTokenRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.updateRefreshToken(1, "updated-token", LocalDateTime.now().plusHours(24))
        .onComplete(ctx.succeeding(rt -> ctx.verify(() -> {
          assertThat(rt).isNotNull();
          assertThat(rt.getToken()).isEqualTo("refresh-token-value");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("updateRefreshToken returns null when user not found")
  void updateRefreshTokenNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.updateRefreshToken(99, "token", LocalDateTime.now().plusHours(24))
        .onComplete(ctx.succeeding(rt -> ctx.verify(() -> {
          assertThat(rt).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── deleteRefreshToken ─── */

  @Test
  @DisplayName("deleteRefreshToken deletes by token")
  void deleteRefreshTokenSuccess(VertxTestContext ctx) {
    mockPool();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.deleteRefreshToken("token-to-delete")
        .onComplete(ctx.succeeding(v -> ctx.verify(ctx::completeNow)));
  }

  /* ─── deleteRefreshTokenByUserId ─── */

  @Test
  @DisplayName("deleteRefreshTokenByUserId deletes by userId")
  void deleteRefreshTokenByUserIdSuccess(VertxTestContext ctx) {
    mockPool();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.deleteRefreshTokenByUserId(1)
        .onComplete(ctx.succeeding(v -> ctx.verify(ctx::completeNow)));
  }
}
