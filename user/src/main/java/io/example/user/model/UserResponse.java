package io.example.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
  private Integer userId;
  private String firstname;
  private String lastname;
  private String email;
  private String createdAt;
  private String updatedAt;

  public static UserResponse from(User u) {
    if (u == null) return null;
    return UserResponse.builder()
        .userId(u.getUserId())
        .firstname(u.getFirstname())
        .lastname(u.getLastname())
        .email(u.getEmail())
        .createdAt(u.getCreatedAt() != null ? u.getCreatedAt().toInstant().toString() : "")
        .updatedAt(u.getUpdatedAt() != null ? u.getUpdatedAt().toInstant().toString() : "")
        .build();
  }
}
