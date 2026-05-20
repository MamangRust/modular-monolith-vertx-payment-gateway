package io.example.role.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoleResponse {
  private Integer id;
  private String name;
  private String createdAt;
  private String updatedAt;

  public static RoleResponse from(Role r) {
    if (r == null) return null;
    return RoleResponse.builder()
        .id(r.getRoleId())
        .name(r.getRoleName())
        .createdAt(r.getCreatedAt() != null ? r.getCreatedAt().toInstant().toString() : "")
        .updatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt().toInstant().toString() : "")
        .build();
  }
}
