package io.example.role;

import io.example.role.handler.ProtoConverter;
import io.example.role.model.Role;
import io.example.role.model.RoleResponse;
import io.example.role.model.RoleResponseDeleteAt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RoleProtoConverterTest {

  /* ───────── toRoleResponse tests ───────── */

  @Test
  @DisplayName("null Role returns default instance")
  void nullRoleReturnsDefault() {
    var result = ProtoConverter.toRoleResponse(null);
    assertThat(result.getId()).isZero();
    assertThat(result.getName()).isEmpty();
  }

  @Test
  @DisplayName("maps all fields from Role to RoleResponse")
  void mapsAllFields() {
    var now = Timestamp.from(Instant.parse("2026-06-26T10:00:00Z"));
    var role = Role.builder()
        .roleId(3)
        .roleName("ROLE_ADMIN")
        .createdAt(now)
        .updatedAt(now)
        .build();

    var result = ProtoConverter.toRoleResponse(role);

    assertThat(result.getId()).isEqualTo(3);
    assertThat(result.getName()).isEqualTo("ROLE_ADMIN");
    assertThat(result.getCreatedAt()).isEqualTo(now.toString());
    assertThat(result.getUpdatedAt()).isEqualTo(now.toString());
  }

  /* ───────── toRoleDeleteAt tests ───────── */

  @Test
  @DisplayName("toRoleDeleteAt maps fields and includes deletedAt when present")
  void deleteAtIncludesDeletedAt() {
    var now = Timestamp.from(Instant.parse("2026-06-26T10:00:00Z"));
    var role = Role.builder()
        .roleId(5)
        .roleName("ROLE_USER")
        .createdAt(now)
        .updatedAt(now)
        .deletedAt(now)
        .build();

    var result = ProtoConverter.toRoleDeleteAt(role);

    assertThat(result.getId()).isEqualTo(5);
    assertThat(result.getName()).isEqualTo("ROLE_USER");
    assertThat(result.hasDeletedAt()).isTrue();
    assertThat(result.getDeletedAt().getValue()).isEqualTo(now.toString());
  }

  @Test
  @DisplayName("toRoleDeleteAt omits deletedAt when null")
  void deleteAtOmitsDeletedWhenNull() {
    var role = Role.builder().roleId(1).build();

    var result = ProtoConverter.toRoleDeleteAt(role);

    assertThat(result.getId()).isEqualTo(1);
    assertThat(result.hasDeletedAt()).isFalse();
  }

  /* ───────── fromRoleResponse tests ───────── */

  @Test
  @DisplayName("null RoleResponse returns default instance")
  void nullRoleResponseReturnsDefault() {
    var result = ProtoConverter.fromRoleResponse(null);
    assertThat(result.getId()).isZero();
    assertThat(result.getName()).isEmpty();
  }

  @Test
  @DisplayName("fromRoleResponse maps all fields")
  void fromRoleResponseMapsFields() {
    var response = RoleResponse.builder()
        .id(10)
        .name("ROLE_MODERATOR")
        .createdAt("2026-01-01")
        .updatedAt("2026-06-01")
        .build();

    var result = ProtoConverter.fromRoleResponse(response);

    assertThat(result.getId()).isEqualTo(10);
    assertThat(result.getName()).isEqualTo("ROLE_MODERATOR");
    assertThat(result.getCreatedAt()).isEqualTo("2026-01-01");
    assertThat(result.getUpdatedAt()).isEqualTo("2026-06-01");
  }

  /* ───────── fromRoleResponseDeleteAt tests ───────── */

  @Test
  @DisplayName("fromRoleResponseDeleteAt includes deletedAt when present")
  void fromDeleteAtIncludesDeleted() {
    var response = RoleResponseDeleteAt.builder()
        .id(7)
        .name("ROLE_DELETED")
        .deletedAt("2026-06-26")
        .build();

    var result = ProtoConverter.fromRoleResponseDeleteAt(response);

    assertThat(result.getId()).isEqualTo(7);
    assertThat(result.hasDeletedAt()).isTrue();
    assertThat(result.getDeletedAt().getValue()).isEqualTo("2026-06-26");
  }

  @Test
  @DisplayName("fromRoleResponseDeleteAt omits deletedAt when null")
  void fromDeleteAtOmitsDeletedWhenNull() {
    var response = RoleResponseDeleteAt.builder().id(8).build();

    var result = ProtoConverter.fromRoleResponseDeleteAt(response);

    assertThat(result.hasDeletedAt()).isFalse();
  }
}
