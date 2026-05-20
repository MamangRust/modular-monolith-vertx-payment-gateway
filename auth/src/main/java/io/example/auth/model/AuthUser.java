package io.example.auth.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUser {
  private Integer userId;
  private String firstname;
  private String lastname;
  private String email;
  private String password;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime deletedAt;

  @Builder.Default
  private List<String> roles = new ArrayList<>();

  public static AuthUser fromRow(Row row) {
    if (row == null) {
      return null;
    }
    return AuthUser.builder()
        .userId(row.getInteger("user_id"))
        .firstname(row.getString("firstname"))
        .lastname(row.getString("lastname"))
        .email(row.getString("email"))
        .password(row.getString("password"))
        .createdAt(row.getLocalDateTime("created_at"))
        .updatedAt(row.getLocalDateTime("updated_at"))
        .deletedAt(row.getLocalDateTime("deleted_at"))
        .build();
  }

  public static AuthUser fromRowsWithRoles(RowSet<Row> rows) {
    if (rows == null || !rows.iterator().hasNext()) {
      return null;
    }
    AuthUser user = fromRow(rows.iterator().next());
    user.setRoles(new ArrayList<>());

    for (Row row : rows) {
      String roleName = row.getString("role_name");
      if (roleName != null && !roleName.isBlank() && !user.getRoles().contains(roleName)) {
        user.getRoles().add(roleName);
      }
    }
    return user;
  }
}
