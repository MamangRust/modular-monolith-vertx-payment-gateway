package io.example.auth.repository.impl;

import io.example.auth.model.UserRole;
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
class UserRoleRepositoryImplTest {

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

  private UserRoleRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new UserRoleRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void mockUserRoleRow() {
    when(row.getInteger("user_id")).thenReturn(1);
    when(row.getInteger("role_id")).thenReturn(10);
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

  /* ─── assignRoleToUser ─── */

  @Test
  @DisplayName("assignRoleToUser inserts and returns user-role relation")
  void assignRoleToUserSuccess(VertxTestContext ctx) {
    mockPool();
    mockUserRoleRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.assignRoleToUser(1, 10)
        .onComplete(ctx.succeeding(ur -> ctx.verify(() -> {
          assertThat(ur).isNotNull();
          assertThat(ur.getUserId()).isEqualTo(1);
          assertThat(ur.getRoleId()).isEqualTo(10);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("assignRoleToUser returns null when conflict (ON CONFLICT DO NOTHING)")
  void assignRoleToUserConflict(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.assignRoleToUser(1, 10)
        .onComplete(ctx.succeeding(ur -> ctx.verify(() -> {
          assertThat(ur).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── removeRoleFromUser ─── */

  @Test
  @DisplayName("removeRoleFromUser deletes the user-role relation")
  void removeRoleFromUserSuccess(VertxTestContext ctx) {
    mockPool();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.removeRoleFromUser(1, 10)
        .onComplete(ctx.succeeding(v -> ctx.verify(ctx::completeNow)));
  }
}
