package io.example.user;

import io.example.user.handler.ProtoConverter;
import io.example.user.model.UserResponse;
import io.example.user.model.UserResponseDeleteAt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserProtoConverterTest {

  /* ───────── toUserResponse tests ───────── */

  @Test
  @DisplayName("null UserResponse returns default instance")
  void nullInputReturnsDefault() {
    var result = ProtoConverter.toUserResponse(null);
    assertThat(result.getId()).isZero();
    assertThat(result.getFirstname()).isEmpty();
    assertThat(result.getLastname()).isEmpty();
    assertThat(result.getEmail()).isEmpty();
  }

  @Test
  @DisplayName("maps all fields from UserResponse to protobuf")
  void mapsAllFields() {
    var response = UserResponse.builder()
        .userId(99)
        .firstname("Bob")
        .lastname("Builder")
        .email("bob@test.com")
        .createdAt("2026-01-15T08:00:00Z")
        .updatedAt("2026-06-20T12:00:00Z")
        .build();

    var result = ProtoConverter.toUserResponse(response);

    assertThat(result.getId()).isEqualTo(99);
    assertThat(result.getFirstname()).isEqualTo("Bob");
    assertThat(result.getLastname()).isEqualTo("Builder");
    assertThat(result.getEmail()).isEqualTo("bob@test.com");
    assertThat(result.getCreatedAt()).isEqualTo("2026-01-15T08:00:00Z");
    assertThat(result.getUpdatedAt()).isEqualTo("2026-06-20T12:00:00Z");
  }

  @Test
  @DisplayName("null fields default to empty values")
  void nullFieldsDefaultToEmpty() {
    var response = new UserResponse();

    var result = ProtoConverter.toUserResponse(response);

    assertThat(result.getId()).isZero();
    assertThat(result.getFirstname()).isEmpty();
    assertThat(result.getLastname()).isEmpty();
    assertThat(result.getEmail()).isEmpty();
    assertThat(result.getCreatedAt()).isEmpty();
    assertThat(result.getUpdatedAt()).isEmpty();
  }

  /* ───────── toUserDeleteAt tests ───────── */

  @Test
  @DisplayName("toUserDeleteAt includes deletedAt when present and non-empty")
  void deleteAtIncludesDeleted() {
    var response = UserResponseDeleteAt.builder()
        .userId(10)
        .firstname("Charlie")
        .deletedAt("2026-06-26T00:00:00Z")
        .build();

    var result = ProtoConverter.toUserDeleteAt(response);

    assertThat(result.getId()).isEqualTo(10);
    assertThat(result.getFirstname()).isEqualTo("Charlie");
    assertThat(result.hasDeletedAt()).isTrue();
    assertThat(result.getDeletedAt().getValue()).isEqualTo("2026-06-26T00:00:00Z");
  }

  @Test
  @DisplayName("toUserDeleteAt omits deletedAt when null")
  void deleteAtOmitsDeletedWhenNull() {
    var response = UserResponseDeleteAt.builder().userId(11).build();

    var result = ProtoConverter.toUserDeleteAt(response);

    assertThat(result.hasDeletedAt()).isFalse();
  }

  @Test
  @DisplayName("toUserDeleteAt omits deletedAt when empty string")
  void deleteAtOmitsEmptyString() {
    var response = UserResponseDeleteAt.builder()
        .userId(12)
        .deletedAt("")
        .build();

    var result = ProtoConverter.toUserDeleteAt(response);

    assertThat(result.hasDeletedAt()).isFalse();
  }
}
