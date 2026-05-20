package io.example.auth.handler;

import io.example.auth.model.AuthUser;

public class ProtoConverter {

  public static pb.user.User.UserResponse toUserResponse(AuthUser model) {
    pb.user.User.UserResponse.Builder b = pb.user.User.UserResponse.newBuilder()
        .setId(model.getUserId() != null ? model.getUserId() : 0)
        .setFirstname(model.getFirstname() != null ? model.getFirstname() : "")
        .setLastname(model.getLastname() != null ? model.getLastname() : "")
        .setEmail(model.getEmail() != null ? model.getEmail() : "");

    if (model.getCreatedAt() != null) b.setCreatedAt(model.getCreatedAt().toString());
    if (model.getUpdatedAt() != null) b.setUpdatedAt(model.getUpdatedAt().toString());

    return b.build();
  }
}
