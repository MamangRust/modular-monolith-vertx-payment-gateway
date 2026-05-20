package io.example.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDeleteAt {
  private Integer userId;
  private String firstname;
  private String lastname;
  private String email;
  private String createdAt;
  private String updatedAt;
  private String deletedAt;

  public static UserResponseDeleteAt from(User u) {
    if (u == null) return null;
    return UserResponseDeleteAt.builder()
        .userId(u.getUserId())
        .firstname(u.getFirstname())
        .lastname(u.getLastname())
        .email(u.getEmail())
        .createdAt(u.getCreatedAt() != null ? u.getCreatedAt().toInstant().toString() : "")
        .updatedAt(u.getUpdatedAt() != null ? u.getUpdatedAt().toInstant().toString() : "")
        .deletedAt(u.getDeletedAt() != null ? u.getDeletedAt().toInstant().toString() : "")
        .build();
  }
}
