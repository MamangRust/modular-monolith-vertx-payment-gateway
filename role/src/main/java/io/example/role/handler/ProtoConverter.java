package io.example.role.handler;

import com.google.protobuf.StringValue;
import io.example.role.model.Role;
import io.example.role.model.RoleResponse;
import io.example.role.model.RoleResponseDeleteAt;

public class ProtoConverter {

  public static pb.role.Role.RoleResponse toRoleResponse(Role r) {
    if (r == null) return pb.role.Role.RoleResponse.getDefaultInstance();
    return pb.role.Role.RoleResponse.newBuilder()
        .setId(r.getRoleId() != null ? r.getRoleId() : 0)
        .setName(r.getRoleName() != null ? r.getRoleName() : "")
        .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt().toString() : "")
        .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt().toString() : "")
        .build();
  }

  public static pb.role.Role.RoleResponseDeleteAt toRoleDeleteAt(Role r) {
    if (r == null) return pb.role.Role.RoleResponseDeleteAt.getDefaultInstance();
    pb.role.Role.RoleResponseDeleteAt.Builder b = pb.role.Role.RoleResponseDeleteAt.newBuilder()
        .setId(r.getRoleId() != null ? r.getRoleId() : 0)
        .setName(r.getRoleName() != null ? r.getRoleName() : "")
        .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt().toString() : "")
        .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt().toString() : "");

    if (r.getDeletedAt() != null) {
      b.setDeletedAt(StringValue.of(r.getDeletedAt().toString()));
    }
    return b.build();
  }

  public static pb.role.Role.RoleResponse fromRoleResponse(RoleResponse r) {
    if (r == null) return pb.role.Role.RoleResponse.getDefaultInstance();
    return pb.role.Role.RoleResponse.newBuilder()
        .setId(r.getId() != null ? r.getId() : 0)
        .setName(r.getName() != null ? r.getName() : "")
        .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : "")
        .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt() : "")
        .build();
  }

  public static pb.role.Role.RoleResponseDeleteAt fromRoleResponseDeleteAt(RoleResponseDeleteAt r) {
    if (r == null) return pb.role.Role.RoleResponseDeleteAt.getDefaultInstance();
    pb.role.Role.RoleResponseDeleteAt.Builder b = pb.role.Role.RoleResponseDeleteAt.newBuilder()
        .setId(r.getId() != null ? r.getId() : 0)
        .setName(r.getName() != null ? r.getName() : "")
        .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : "")
        .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt() : "");

    if (r.getDeletedAt() != null) {
      b.setDeletedAt(StringValue.of(r.getDeletedAt()));
    }
    return b.build();
  }
}

