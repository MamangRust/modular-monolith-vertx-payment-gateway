package io.example.auth.model;

import io.vertx.sqlclient.Row;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class RefreshTokenModelTest {

  @Mock
  private Row row;

  @Test
  @DisplayName("fromRow maps all columns with snake_case fallback")
  void fromRowMapsAllColumns() {
    var expiration = LocalDateTime.of(2026, 6, 27, 10, 0, 0);
    var now = LocalDateTime.of(2026, 6, 26, 10, 0, 0);

    when(row.getInteger("refresh_token_id")).thenReturn(null);
    when(row.getInteger("refreshTokenId")).thenReturn(100);
    when(row.getInteger("user_id")).thenReturn(null);
    when(row.getInteger("userId")).thenReturn(42);
    when(row.getString("token")).thenReturn("rt-abc123");
    when(row.getLocalDateTime("expiration")).thenReturn(expiration);
    when(row.getLocalDateTime("created_at")).thenReturn(now);
    when(row.getLocalDateTime("updated_at")).thenReturn(now);
    when(row.getLocalDateTime("deleted_at")).thenReturn(null);

    var rt = RefreshToken.fromRow(row);

    assertThat(rt).isNotNull();
    assertThat(rt.getRefreshTokenId()).isEqualTo(100);
    assertThat(rt.getUserId()).isEqualTo(42);
    assertThat(rt.getToken()).isEqualTo("rt-abc123");
    assertThat(rt.getExpiration()).isEqualTo(expiration);
    assertThat(rt.getCreatedAt()).isEqualTo(now);
    assertThat(rt.getUpdatedAt()).isEqualTo(now);
    assertThat(rt.getDeletedAt()).isNull();
  }

  @Test
  @DisplayName("fromRow prefers snake_case when both column names exist")
  void fromRowPrefersSnakeCase() {
    when(row.getInteger("refresh_token_id")).thenReturn(200);
    when(row.getInteger("refreshTokenId")).thenReturn(999); // should be ignored
    when(row.getInteger("user_id")).thenReturn(10);
    when(row.getInteger("userId")).thenReturn(888); // should be ignored
    when(row.getString("token")).thenReturn("rt-xyz");
    when(row.getLocalDateTime("expiration")).thenReturn(null);
    when(row.getLocalDateTime("created_at")).thenReturn(null);
    when(row.getLocalDateTime("updated_at")).thenReturn(null);
    when(row.getLocalDateTime("deleted_at")).thenReturn(null);

    var rt = RefreshToken.fromRow(row);

    assertThat(rt.getRefreshTokenId()).isEqualTo(200);
    assertThat(rt.getUserId()).isEqualTo(10);
  }

  @Test
  @DisplayName("fromRow returns null for null input")
  void fromRowNullInput() {
    assertThat(RefreshToken.fromRow(null)).isNull();
  }

  @Test
  @DisplayName("toJson converts all fields including nullable timestamps")
  void toJsonIncludesAllFields() {
    var expiration = LocalDateTime.of(2026, 6, 27, 10, 0, 0);
    var now = LocalDateTime.of(2026, 6, 26, 10, 0, 0);

    var rt = RefreshToken.builder()
        .refreshTokenId(100)
        .userId(42)
        .token("rt-abc123")
        .expiration(expiration)
        .createdAt(now)
        .updatedAt(now)
        .deletedAt(null)
        .build();

    var json = rt.toJson();

    assertThat(json.getInteger("refreshTokenId")).isEqualTo(100);
    assertThat(json.getInteger("userId")).isEqualTo(42);
    assertThat(json.getString("token")).isEqualTo("rt-abc123");
    assertThat(json.getString("expiration")).isEqualTo("2026-06-27T10:00");
    assertThat(json.getString("createdAt")).isEqualTo("2026-06-26T10:00");
    assertThat(json.getString("updatedAt")).isEqualTo("2026-06-26T10:00");
    assertThat(json.containsKey("deletedAt")).isFalse();
  }

  @Test
  @DisplayName("toJson handles null timestamps gracefully")
  void toJsonNullTimestamps() {
    var rt = RefreshToken.builder()
        .refreshTokenId(1)
        .userId(2)
        .token("rt-null-test")
        .build();
    // expiration, createdAt, updatedAt, deletedAt are all null by default

    var json = rt.toJson();

    assertThat(json.getInteger("refreshTokenId")).isEqualTo(1);
    assertThat(json.getInteger("userId")).isEqualTo(2);
    assertThat(json.getString("token")).isEqualTo("rt-null-test");
    assertThat(json.containsKey("expiration")).isFalse();
    assertThat(json.containsKey("createdAt")).isFalse();
    assertThat(json.containsKey("updatedAt")).isFalse();
    assertThat(json.containsKey("deletedAt")).isFalse();
  }
}
