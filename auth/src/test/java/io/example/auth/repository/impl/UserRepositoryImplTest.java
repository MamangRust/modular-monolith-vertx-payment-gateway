package io.example.auth.repository.impl;

import io.example.auth.domain.requests.CreateUserRequest;
import io.example.auth.model.AuthUser;
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
class UserRepositoryImplTest {

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

  private UserRepositoryImpl repo;

  @BeforeEach
  void setUp() {
    repo = new UserRepositoryImpl(pool);
  }

  private void mockPool() {
    when(pool.preparedQuery(anyString())).thenReturn(preparedQuery);
  }

  private AuthUser mockUserRow() {
    var now = LocalDateTime.of(2026, 6, 26, 10, 0, 0);
    when(row.getInteger("user_id")).thenReturn(1);
    when(row.getString("firstname")).thenReturn("Alice");
    when(row.getString("lastname")).thenReturn("Wonderland");
    when(row.getString("email")).thenReturn("alice@example.com");
    when(row.getString("password")).thenReturn("hashed-bcrypt");
    when(row.getLocalDateTime("created_at")).thenReturn(now);
    when(row.getLocalDateTime("updated_at")).thenReturn(now);
    when(row.getLocalDateTime("deleted_at")).thenReturn(null);
    when(rowSet.iterator()).thenReturn(iterator);
    return AuthUser.fromRow(row);
  }

  private void stubSingleRow() {
    when(iterator.hasNext()).thenReturn(true);
    when(iterator.next()).thenReturn(row);
  }

  private void stubNoRows() {
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(false);
  }

  /* ─── findByEmail ─── */

  @Test
  @DisplayName("findByEmail returns user when found")
  void findByEmailFound(VertxTestContext ctx) {
    mockPool();
    mockUserRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByEmail("alice@example.com")
        .onComplete(ctx.succeeding(user -> ctx.verify(() -> {
          assertThat(user).isNotNull();
          assertThat(user.getUserId()).isEqualTo(1);
          assertThat(user.getEmail()).isEqualTo("alice@example.com");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByEmail returns null when not found")
  void findByEmailNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByEmail("missing@example.com")
        .onComplete(ctx.succeeding(user -> ctx.verify(() -> {
          assertThat(user).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── findByEmailAndVerify ─── */

  @Test
  @DisplayName("findByEmailAndVerify returns verified user")
  void findByEmailAndVerifyFound(VertxTestContext ctx) {
    mockPool();
    mockUserRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByEmailAndVerify("alice@example.com")
        .onComplete(ctx.succeeding(user -> ctx.verify(() -> {
          assertThat(user).isNotNull();
          assertThat(user.getEmail()).isEqualTo("alice@example.com");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByEmailAndVerify returns null when not found")
  void findByEmailAndVerifyNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByEmailAndVerify("unverified@example.com")
        .onComplete(ctx.succeeding(user -> ctx.verify(() -> {
          assertThat(user).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── findById ─── */

  @Test
  @DisplayName("findById returns user when found")
  void findByIdFound(VertxTestContext ctx) {
    mockPool();
    mockUserRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findById(1)
        .onComplete(ctx.succeeding(user -> ctx.verify(() -> {
          assertThat(user).isNotNull();
          assertThat(user.getUserId()).isEqualTo(1);
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
        .onComplete(ctx.succeeding(user -> ctx.verify(() -> {
          assertThat(user).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── createUser ─── */

  @Test
  @DisplayName("createUser inserts and returns new user")
  void createUserSuccess(VertxTestContext ctx) {
    mockPool();
    mockUserRow();
    // createUser does not check hasNext() — directly calls next()
    when(iterator.next()).thenReturn(row);

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    var request = CreateUserRequest.builder()
        .firstName("Alice")
        .lastName("Wonderland")
        .email("alice@example.com")
        .password("hashed-bcrypt")
        .verificationCode("abc123")
        .build();

    repo.createUser(request)
        .onComplete(ctx.succeeding(user -> ctx.verify(() -> {
          assertThat(user).isNotNull();
          assertThat(user.getUserId()).isEqualTo(1);
          assertThat(user.getEmail()).isEqualTo("alice@example.com");
          ctx.completeNow();
        })));
  }

  /* ─── updateUserIsVerified ─── */

  @Test
  @DisplayName("updateUserIsVerified updates and returns user")
  void updateUserIsVerifiedSuccess(VertxTestContext ctx) {
    mockPool();
    mockUserRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.updateUserIsVerified(1, true)
        .onComplete(ctx.succeeding(user -> ctx.verify(() -> {
          assertThat(user).isNotNull();
          assertThat(user.getUserId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("updateUserIsVerified returns null when user not found")
  void updateUserIsVerifiedNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.updateUserIsVerified(99, true)
        .onComplete(ctx.succeeding(user -> ctx.verify(() -> {
          assertThat(user).isNull();
          ctx.completeNow();
        })));
  }

  /* ─── updateUserPassword ─── */

  @Test
  @DisplayName("updateUserPassword updates and returns user")
  void updateUserPasswordSuccess(VertxTestContext ctx) {
    mockPool();
    mockUserRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.updateUserPassword(1, "new-hashed-password")
        .onComplete(ctx.succeeding(user -> ctx.verify(() -> {
          assertThat(user).isNotNull();
          assertThat(user.getUserId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── findByVerificationCode ─── */

  @Test
  @DisplayName("findByVerificationCode returns user when found")
  void findByVerificationCodeFound(VertxTestContext ctx) {
    mockPool();
    mockUserRow();
    stubSingleRow();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByVerificationCode("abc123")
        .onComplete(ctx.succeeding(user -> ctx.verify(() -> {
          assertThat(user).isNotNull();
          assertThat(user.getUserId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByVerificationCode returns null when not found")
  void findByVerificationCodeNotFound(VertxTestContext ctx) {
    mockPool();
    stubNoRows();

    when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

    repo.findByVerificationCode("invalid")
        .onComplete(ctx.succeeding(user -> ctx.verify(() -> {
          assertThat(user).isNull();
          ctx.completeNow();
        })));
  }
}
