package io.example.user.repository.impl;

import io.example.user.domain.requests.FindAllUsers;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class UserQueryRepositoryImplTest {

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

  private UserQueryRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new UserQueryRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private void mockUserRow() {
    when(row.getInteger("user_id")).thenReturn(1);
    when(row.getString("firstname")).thenReturn("John");
    when(row.getString("lastname")).thenReturn("Doe");
    when(row.getString("email")).thenReturn("john@example.com");
    when(row.getString("password")).thenReturn("hashed_pwd");
    when(row.get(LocalDateTime.class, "created_at")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.get(LocalDateTime.class, "updated_at")).thenReturn(LocalDateTime.of(2026, 6, 26, 10, 0, 0));
    when(row.get(LocalDateTime.class, "deleted_at")).thenReturn(null);
    when(row.getInteger("total_count")).thenReturn(1);
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

  /* ─── getUsers ─── */

  @Test
  @DisplayName("getUsers returns paged users")
  void getUsersSuccess(VertxTestContext ctx) {
    mockPool();
    mockUserRow();
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = FindAllUsers.builder().page(1).pageSize(10).build();

    repo.getUsers(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getTotalRecords()).isEqualTo(1);
          assertThat(result.getData().get(0).getUserId()).isEqualTo(1);
          assertThat(result.getData().get(0).getEmail()).isEqualTo("john@example.com");
          ctx.completeNow();
        })));
  }

  /* ─── getActiveUsers ─── */

  @Test
  @DisplayName("getActiveUsers returns paged active users")
  void getActiveUsersSuccess(VertxTestContext ctx) {
    mockPool();
    mockUserRow();
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = FindAllUsers.builder().page(1).pageSize(10).build();

    repo.getActiveUsers(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getTotalRecords()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── getTrashedUsers ─── */

  @Test
  @DisplayName("getTrashedUsers returns paged trashed users")
  void getTrashedUsersSuccess(VertxTestContext ctx) {
    mockPool();
    mockUserRow();
    when(iterator.hasNext()).thenReturn(true, false);
    when(iterator.next()).thenReturn(row);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = FindAllUsers.builder().page(1).pageSize(10).build();

    repo.getTrashedUsers(req)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result.getData()).hasSize(1);
          assertThat(result.getTotalRecords()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── getUserById ─── */

  @Test
  @DisplayName("getUserById returns user when found")
  void getUserByIdFound(VertxTestContext ctx) {
    mockPool();
    mockUserRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getUserById(1)
        .onComplete(ctx.succeeding(user -> ctx.verify(() -> {
          assertThat(user).isNotNull();
          assertThat(user.getUserId()).isEqualTo(1);
          assertThat(user.getEmail()).isEqualTo("john@example.com");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getUserById returns null when not found")
  void getUserByIdNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getUserById(99)
        .onComplete(ctx.succeeding(user -> ctx.verify(() -> {
          assertThat(user).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── findByTrashedId ─── */

  @Test
  @DisplayName("findByTrashedId returns trashed user when found")
  void findByTrashedIdFound(VertxTestContext ctx) {
    mockPool();
    mockUserRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByTrashedId(1)
        .onComplete(ctx.succeeding(user -> ctx.verify(() -> {
          assertThat(user).isNotNull();
          assertThat(user.getUserId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByTrashedId returns null when not found")
  void findByTrashedIdNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByTrashedId(99)
        .onComplete(ctx.succeeding(user -> ctx.verify(() -> {
          assertThat(user).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── getUserByEmail ─── */

  @Test
  @DisplayName("getUserByEmail returns user when found")
  void getUserByEmailFound(VertxTestContext ctx) {
    mockPool();
    mockUserRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getUserByEmail("john@example.com")
        .onComplete(ctx.succeeding(user -> ctx.verify(() -> {
          assertThat(user).isNotNull();
          assertThat(user.getEmail()).isEqualTo("john@example.com");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("getUserByEmail returns null when not found")
  void getUserByEmailNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.getUserByEmail("unknown@example.com")
        .onComplete(ctx.succeeding(user -> ctx.verify(() -> {
          assertThat(user).isNull();
          ctx.completeNow();
        })));
  }
}
