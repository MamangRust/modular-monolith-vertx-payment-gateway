package io.example.auth;

import io.example.auth.handler.ProtoConverter;
import io.example.auth.model.AuthUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AuthProtoConverterTest {

  /* ───────── toUserResponse tests ───────── */

  @Test
  @DisplayName("null input throws NullPointerException")
  void nullInputThrowsNpe() {
    org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class,
        () -> ProtoConverter.toUserResponse(null));
  }

  @Test
  @DisplayName("maps all fields correctly from AuthUser to UserResponse")
  void mapsAllFields() {
    var now = LocalDateTime.parse("2026-06-26T10:00:00");
    var user = AuthUser.builder()
        .userId(42)
        .firstname("Alice")
        .lastname("Wonderland")
        .email("alice@example.com")
        .createdAt(now)
        .updatedAt(now)
        .build();

    var result = ProtoConverter.toUserResponse(user);

    assertThat(result.getId()).isEqualTo(42);
    assertThat(result.getFirstname()).isEqualTo("Alice");
    assertThat(result.getLastname()).isEqualTo("Wonderland");
    assertThat(result.getEmail()).isEqualTo("alice@example.com");
    assertThat(result.getCreatedAt()).isEqualTo(now.toString());
    assertThat(result.getUpdatedAt()).isEqualTo(now.toString());
  }

  @Test
  @DisplayName("null fields are replaced with empty/default values")
  void nullFieldsDefaultToEmpty() {
    var user = new AuthUser();

    var result = ProtoConverter.toUserResponse(user);

    assertThat(result.getId()).isZero();
    assertThat(result.getFirstname()).isEmpty();
    assertThat(result.getLastname()).isEmpty();
    assertThat(result.getEmail()).isEmpty();
    assertThat(result.getCreatedAt()).isEmpty();
    assertThat(result.getUpdatedAt()).isEmpty();
  }
}
