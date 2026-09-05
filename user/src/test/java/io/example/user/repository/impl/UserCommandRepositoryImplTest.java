package io.example.user.repository.impl;

import io.example.user.domain.requests.CreateUserRequest;
import io.example.user.domain.requests.UpdatePasswordRequest;
import io.example.user.domain.requests.UpdateUserRequest;
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

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class UserCommandRepositoryImplTest {

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

  private UserCommandRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new UserCommandRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private Timestamp now() {
    return Timestamp.from(Instant.parse("2026-06-26T10:00:00Z"));
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

  private void stubDeleteResult(int count) {
    when(rowSet.rowCount()).thenReturn(count);
  }

  /* ─── createUser ─── */

  @Test
  @DisplayName("createUser inserts and returns new user")
  void createUserSuccess(VertxTestContext ctx) {
    mockPool();
    mockUserRow();
    when(iterator.next()).thenReturn(row);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = CreateUserRequest.builder()
        .firstName("John").lastName("Doe").email("john@example.com").password("plain_pwd")
        .build();

    repo.createUser(req)
        .onComplete(ctx.succeeding(user -> ctx.verify(() -> {
          assertThat(user).isNotNull();
          assertThat(user.getUserId()).isEqualTo(1);
          assertThat(user.getFirstname()).isEqualTo("John");
          assertThat(user.getLastname()).isEqualTo("Doe");
          assertThat(user.getEmail()).isEqualTo("john@example.com");
          assertThat(user.getPassword()).isEqualTo("hashed_pwd");
          assertThat(user.getCreatedAt()).isNotNull();
          assertThat(user.getUpdatedAt()).isNotNull();
          ctx.completeNow();
        })));
  }

  /* ─── assignDefaultAdminRole ─── */

  @Test
  @DisplayName("assignDefaultAdminRole succeeds for valid userId")
  void assignDefaultAdminRoleSuccess(VertxTestContext ctx) {
    mockPool();
    stubDeleteResult(1);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.assignDefaultAdminRole(1)
        .onComplete(ctx.succeeding(v -> ctx.verify(() -> {
          ctx.completeNow();
        })));
  }

  /* ─── updateUser ─── */

  @Test
  @DisplayName("updateUser updates and returns user")
  void updateUserSuccess(VertxTestContext ctx) {
    mockPool();
    mockUserRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = UpdateUserRequest.builder()
        .userId(1).firstName("Jane").lastName("Doe").email("jane@example.com")
        .build();

    repo.updateUser(req)
        .onComplete(ctx.succeeding(user -> ctx.verify(() -> {
          assertThat(user).isNotNull();
          assertThat(user.getUserId()).isEqualTo(1);
          assertThat(user.getFirstname()).isEqualTo("John");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("updateUser returns null when user not found")
  void updateUserNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = UpdateUserRequest.builder().userId(99).build();

    repo.updateUser(req)
        .onComplete(ctx.succeeding(user -> ctx.verify(() -> {
          assertThat(user).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── updatePassword ─── */

  @Test
  @DisplayName("updatePassword updates and returns user")
  void updatePasswordSuccess(VertxTestContext ctx) {
    mockPool();
    mockUserRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = UpdatePasswordRequest.builder().userId(1).password("new_hashed_pwd").build();

    repo.updatePassword(req)
        .onComplete(ctx.succeeding(user -> ctx.verify(() -> {
          assertThat(user).isNotNull();
          assertThat(user.getUserId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("updatePassword returns null when user not found")
  void updatePasswordNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var req = UpdatePasswordRequest.builder().userId(99).password("new_pwd").build();

    repo.updatePassword(req)
        .onComplete(ctx.succeeding(user -> ctx.verify(() -> {
          assertThat(user).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── restore ─── */

  @Test
  @DisplayName("restore sets deleted_at null and returns user")
  void restoreSuccess(VertxTestContext ctx) {
    mockPool();
    mockUserRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.restore(1)
        .onComplete(ctx.succeeding(user -> ctx.verify(() -> {
          assertThat(user).isNotNull();
          assertThat(user.getUserId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── trashed ─── */

  @Test
  @DisplayName("trashed soft-deletes and returns user")
  void trashedSuccess(VertxTestContext ctx) {
    mockPool();
    mockUserRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.trashed(1)
        .onComplete(ctx.succeeding(user -> ctx.verify(() -> {
          assertThat(user).isNotNull();
          assertThat(user.getUserId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("trashed returns null when user not found or already deleted")
  void trashedNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.trashed(99)
        .onComplete(ctx.succeeding(user -> ctx.verify(() -> {
          assertThat(user).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── deletePermanent ─── */

  @Test
  @DisplayName("deletePermanent deletes and returns true")
  void deletePermanentTrue(VertxTestContext ctx) {
    mockPool();
    stubDeleteResult(1);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.deletePermanent(1)
        .onComplete(ctx.succeeding(deleted -> ctx.verify(() -> {
          assertThat(deleted).isTrue();
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("deletePermanent returns false when no rows affected")
  void deletePermanentFalse(VertxTestContext ctx) {
    mockPool();
    stubDeleteResult(0);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.deletePermanent(99)
        .onComplete(ctx.succeeding(deleted -> ctx.verify(() -> {
          assertThat(deleted).isFalse();
          ctx.completeNow();
        })));
  }

  /* ─── restoreAllUsers ─── */

  @Test
  @DisplayName("restoreAllUsers returns count of restored users")
  void restoreAllUsersSuccess(VertxTestContext ctx) {
    when(pool.query(anyString())).thenReturn(preparedQuery);
    stubDeleteResult(3);

    when(preparedQuery.execute()).thenReturn(Future.succeededFuture(rowSet));

    repo.restoreAllUsers()
        .onComplete(ctx.succeeding(count -> ctx.verify(() -> {
          assertThat(count).isEqualTo(3);
          ctx.completeNow();
        })));
  }

  /* ─── deleteAllPermanentUsers ─── */

  @Test
  @DisplayName("deleteAllPermanentUsers returns count of deleted users")
  void deleteAllPermanentUsersSuccess(VertxTestContext ctx) {
    when(pool.query(anyString())).thenReturn(preparedQuery);
    stubDeleteResult(2);

    when(preparedQuery.execute()).thenReturn(Future.succeededFuture(rowSet));

    repo.deleteAllPermanentUsers()
        .onComplete(ctx.succeeding(count -> ctx.verify(() -> {
          assertThat(count).isEqualTo(2);
          ctx.completeNow();
        })));
  }
}
