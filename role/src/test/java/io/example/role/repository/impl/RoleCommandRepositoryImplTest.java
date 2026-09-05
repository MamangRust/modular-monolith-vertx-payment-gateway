package io.example.role.repository.impl;

import io.example.role.domain.requests.CreateRoleRequest;
import io.example.role.domain.requests.UpdateRoleRequest;
import io.example.role.model.Role;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class RoleCommandRepositoryImplTest {
  @Mock private Pool pool; @Mock private PreparedQuery<RowSet<Row>> pq;
  @Mock private io.vertx.sqlclient.Query<RowSet<Row>> q;
  @Mock private RowSet<Row> rs; @Mock private RowIterator<Row> it; @Mock private Row row;
  private RoleCommandRepositoryImpl repo;

  @BeforeEach void setUp() { repo = new RoleCommandRepositoryImpl(pool); }
  void mp() { when(pool.preparedQuery(anyString())).thenReturn(pq); }
  void mr() {
    when(row.getInteger("role_id")).thenReturn(1);
    when(row.getString("role_name")).thenReturn("ROLE_ADMIN");
    when(row.get(LocalDateTime.class, "created_at")).thenReturn(LocalDateTime.of(2026,6,26,10,0,0));
    when(row.get(LocalDateTime.class, "updated_at")).thenReturn(LocalDateTime.of(2026,6,26,10,0,0));
    when(row.get(LocalDateTime.class, "deleted_at")).thenReturn(null);
  }
  void sr() { when(rs.iterator()).thenReturn(it); when(it.hasNext()).thenReturn(true); when(it.next()).thenReturn(row); }
  void nr() { when(rs.iterator()).thenReturn(it); when(it.hasNext()).thenReturn(false); }

  @Test void createRole(VertxTestContext ctx) {
    mp(); mr(); sr();
    when(pq.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rs));
    repo.createRole(CreateRoleRequest.builder().name("ROLE_ADMIN").build())
        .onComplete(ctx.succeeding(r -> ctx.verify(() -> { assertThat(r).isNotNull(); ctx.completeNow(); })));
  }

  @Test void updateRole(VertxTestContext ctx) {
    mp(); mr(); sr();
    when(pq.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rs));
    repo.updateRole(UpdateRoleRequest.builder().roleId(1).name("ROLE_USER").build())
        .onComplete(ctx.succeeding(r -> ctx.verify(() -> { assertThat(r).isNotNull(); ctx.completeNow(); })));
  }

  @Test void updateRole_notFound(VertxTestContext ctx) {
    mp(); nr();
    when(pq.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rs));
    repo.updateRole(UpdateRoleRequest.builder().roleId(99).name("GHOST").build())
        .onComplete(ctx.succeeding(r -> ctx.verify(() -> { assertThat(r).isNull(); ctx.completeNow(); })));
  }

  @Test void trashed(VertxTestContext ctx) {
    mp(); mr(); sr();
    when(pq.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rs));
    repo.trashed(1).onComplete(ctx.succeeding(r -> ctx.verify(() -> { assertThat(r).isNotNull(); ctx.completeNow(); })));
  }

  @Test void restore(VertxTestContext ctx) {
    mp(); mr(); sr();
    when(pq.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rs));
    repo.restore(1).onComplete(ctx.succeeding(r -> ctx.verify(() -> { assertThat(r).isNotNull(); ctx.completeNow(); })));
  }

  @Test void deletePermanent_true(VertxTestContext ctx) {
    mp(); when(rs.rowCount()).thenReturn(1);
    when(pq.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rs));
    repo.deletePermanent(1).onComplete(ctx.succeeding(b -> ctx.verify(() -> { assertThat(b).isTrue(); ctx.completeNow(); })));
  }

  @Test void deletePermanent_false(VertxTestContext ctx) {
    mp(); when(rs.rowCount()).thenReturn(0);
    when(pq.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rs));
    repo.deletePermanent(99).onComplete(ctx.succeeding(b -> ctx.verify(() -> { assertThat(b).isFalse(); ctx.completeNow(); })));
  }

  @Test void restoreAllRoles(VertxTestContext ctx) {
    when(pool.query(anyString())).thenReturn(q); when(q.execute()).thenReturn(Future.succeededFuture(rs));
    when(rs.rowCount()).thenReturn(3);
    repo.restoreAllRoles().onComplete(ctx.succeeding(c -> ctx.verify(() -> { assertThat(c).isEqualTo(3); ctx.completeNow(); })));
  }

  @Test void deleteAllPermanentRoles(VertxTestContext ctx) {
    when(pool.query(anyString())).thenReturn(q); when(q.execute()).thenReturn(Future.succeededFuture(rs));
    when(rs.rowCount()).thenReturn(2);
    repo.deleteAllPermanentRoles().onComplete(ctx.succeeding(c -> ctx.verify(() -> { assertThat(c).isEqualTo(2); ctx.completeNow(); })));
  }
}
