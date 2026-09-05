package io.example.auth.model;

import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.RowIterator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthUserModelTest {

  @Mock
  private Row row;

  @Mock
  private RowSet<Row> rowSet;

  @Mock
  private RowIterator<Row> iterator;

  @Test
  @DisplayName("fromRow maps all columns correctly")
  void fromRowMapsAllColumns() {
    var now = LocalDateTime.of(2026, 6, 26, 10, 0, 0);
    when(row.getInteger("user_id")).thenReturn(1);
    when(row.getString("firstname")).thenReturn("Alice");
    when(row.getString("lastname")).thenReturn("Wonderland");
    when(row.getString("email")).thenReturn("alice@example.com");
    when(row.getString("password")).thenReturn("hashed-bcrypt");
    when(row.getLocalDateTime("created_at")).thenReturn(now);
    when(row.getLocalDateTime("updated_at")).thenReturn(now);
    when(row.getLocalDateTime("deleted_at")).thenReturn(null);

    var user = AuthUser.fromRow(row);

    assertThat(user).isNotNull();
    assertThat(user.getUserId()).isEqualTo(1);
    assertThat(user.getFirstname()).isEqualTo("Alice");
    assertThat(user.getLastname()).isEqualTo("Wonderland");
    assertThat(user.getEmail()).isEqualTo("alice@example.com");
    assertThat(user.getPassword()).isEqualTo("hashed-bcrypt");
    assertThat(user.getCreatedAt()).isEqualTo(now);
    assertThat(user.getUpdatedAt()).isEqualTo(now);
    assertThat(user.getDeletedAt()).isNull();
    assertThat(user.getRoles()).isEmpty();
  }

  @Test
  @DisplayName("fromRow returns null for null input")
  void fromRowNullInput() {
    assertThat(AuthUser.fromRow(null)).isNull();
  }

  @Test
  @DisplayName("fromRow handles nullable fields as null")
  void fromRowHandlesNullableFields() {
    when(row.getInteger("user_id")).thenReturn(2);
    when(row.getString("firstname")).thenReturn("Bob");
    when(row.getString("lastname")).thenReturn(null);
    when(row.getString("email")).thenReturn("bob@example.com");
    when(row.getString("password")).thenReturn(null);
    when(row.getLocalDateTime("created_at")).thenReturn(null);
    when(row.getLocalDateTime("updated_at")).thenReturn(null);
    when(row.getLocalDateTime("deleted_at")).thenReturn(null);

    var user = AuthUser.fromRow(row);

    assertThat(user).isNotNull();
    assertThat(user.getUserId()).isEqualTo(2);
    assertThat(user.getFirstname()).isEqualTo("Bob");
    assertThat(user.getLastname()).isNull();
    assertThat(user.getPassword()).isNull();
    assertThat(user.getCreatedAt()).isNull();
    assertThat(user.getUpdatedAt()).isNull();
    assertThat(user.getDeletedAt()).isNull();
  }

  @Test
  @DisplayName("fromRowsWithRoles extracts user and collects unique role names")
  void fromRowsWithRolesCollectsRoles() {
    var now = LocalDateTime.of(2026, 6, 26, 10, 0, 0);

    // First row — user data
    var row1 = row;
    when(row1.getInteger("user_id")).thenReturn(1);
    when(row1.getString("firstname")).thenReturn("Charlie");
    when(row1.getString("lastname")).thenReturn("Brown");
    when(row1.getString("email")).thenReturn("charlie@example.com");
    when(row1.getString("password")).thenReturn("hash");
    when(row1.getLocalDateTime("created_at")).thenReturn(now);
    when(row1.getLocalDateTime("updated_at")).thenReturn(now);
    when(row1.getLocalDateTime("deleted_at")).thenReturn(null);
    // Stub role_name on row1 leniently — row1 is consumed by fromRow()
    // (which doesn't read role_name), and the shared iterator has already
    // advanced past row1 when the for-loop runs.
    lenient().when(row1.getString("role_name")).thenReturn("ROLE_ADMIN");

    // Second row — same user, different role
    var row2 = org.mockito.Mockito.mock(Row.class);
    when(row2.getString("role_name")).thenReturn("ROLE_USER");

    // Third row — same user, duplicate role (should be deduped)
    var row3 = org.mockito.Mockito.mock(Row.class);
    when(row3.getString("role_name")).thenReturn("ROLE_ADMIN");

    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(true, true, true, false); // row1, row2, row3, then stop
    when(iterator.next()).thenReturn(row1, row2, row3);

    var user = AuthUser.fromRowsWithRoles(rowSet);

    assertThat(user).isNotNull();
    assertThat(user.getUserId()).isEqualTo(1);
    assertThat(user.getFirstname()).isEqualTo("Charlie");
    // The iterator is shared: first call consumes row1 for user data,
    // then the for-loop processes remaining rows (row2, row3) for roles
    assertThat(user.getRoles()).containsExactly("ROLE_USER", "ROLE_ADMIN");
  }

  @Test
  @DisplayName("fromRowsWithRoles returns null for empty RowSet")
  void fromRowsWithRolesEmpty() {
    when(rowSet.iterator()).thenReturn(iterator);
    when(iterator.hasNext()).thenReturn(false);

    assertThat(AuthUser.fromRowsWithRoles(rowSet)).isNull();
  }

  @Test
  @DisplayName("fromRowsWithRoles returns null for null input")
  void fromRowsWithRolesNull() {
    assertThat(AuthUser.fromRowsWithRoles(null)).isNull();
  }
}
