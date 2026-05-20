package io.example.user.handler;

import com.google.protobuf.StringValue;
import io.example.user.model.UserResponse;
import io.example.user.model.UserResponseDeleteAt;

public class ProtoConverter {

  public static pb.user.User.UserResponse toUserResponse(UserResponse u) {
    if (u == null) return pb.user.User.UserResponse.getDefaultInstance();
    return pb.user.User.UserResponse.newBuilder()
        .setId(u.getUserId() != null ? u.getUserId() : 0)
        .setFirstname(u.getFirstname() != null ? u.getFirstname() : "")
        .setLastname(u.getLastname() != null ? u.getLastname() : "")
        .setEmail(u.getEmail() != null ? u.getEmail() : "")
        .setCreatedAt(u.getCreatedAt() != null ? u.getCreatedAt() : "")
        .setUpdatedAt(u.getUpdatedAt() != null ? u.getUpdatedAt() : "")
        .build();
  }

  public static pb.user.User.UserResponseDeleteAt toUserDeleteAt(UserResponseDeleteAt u) {
    if (u == null) return pb.user.User.UserResponseDeleteAt.getDefaultInstance();
    pb.user.User.UserResponseDeleteAt.Builder b = pb.user.User.UserResponseDeleteAt.newBuilder()
        .setId(u.getUserId() != null ? u.getUserId() : 0)
        .setFirstname(u.getFirstname() != null ? u.getFirstname() : "")
        .setLastname(u.getLastname() != null ? u.getLastname() : "")
        .setEmail(u.getEmail() != null ? u.getEmail() : "")
        .setCreatedAt(u.getCreatedAt() != null ? u.getCreatedAt() : "")
        .setUpdatedAt(u.getUpdatedAt() != null ? u.getUpdatedAt() : "");

    if (u.getDeletedAt() != null && !u.getDeletedAt().isEmpty()) {
      b.setDeletedAt(StringValue.of(u.getDeletedAt()));
    }
    return b.build();
  }
}
