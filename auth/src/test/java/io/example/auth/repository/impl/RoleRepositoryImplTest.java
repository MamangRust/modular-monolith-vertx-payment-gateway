package io.example.auth.repository.impl;

import io.example.auth.model.Role;
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
class RoleRepositoryImplTest {

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

  private RoleRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new RoleRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void mockRoleRow() {
    var now = LocalDateTime.of(2026, 6, 26, 10, 0, 0);
    when(row.getInteger("role_id")).thenReturn(1);
    when(row.getString("role_name")).thenReturn("ROLE_ADMIN");
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

  /* ─── findById ─── */

  @Test
  @DisplayName("findById returns role when found")
  void findByIdFound(VertxTestContext ctx) {
    mockPool();
    mockRoleRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findById(1)
        .onComplete(ctx.succeeding(role -> ctx.verify(() -> {
          assertThat(role).isNotNull();
          assertThat(role.getRoleId()).isEqualTo(1);
          assertThat(role.getRoleName()).isEqualTo("ROLE_ADMIN");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findById returns null when not found")
  void findByIdNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findById(99)
        .onComplete(ctx.succeeding(role -> ctx.verify(() -> {
          assertThat(role).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── findByName ─── */

  @Test
  @DisplayName("findByName returns role when found")
  void findByNameFound(VertxTestContext ctx) {
    mockPool();
    mockRoleRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByName("ROLE_ADMIN")
        .onComplete(ctx.succeeding(role -> ctx.verify(() -> {
          assertThat(role).isNotNull();
          assertThat(role.getRoleName()).isEqualTo("ROLE_ADMIN");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByName returns null when not found")
  void findByNameNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByName("ROLE_GHOST")
        .onComplete(ctx.succeeding(role -> ctx.verify(() -> {
          assertThat(role).isNull();
          ctx.completeNow();
        })));
  }
}
