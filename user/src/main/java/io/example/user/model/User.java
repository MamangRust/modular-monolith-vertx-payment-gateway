package io.example.user.model;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
  private Integer userId;
  private String firstname;
  private String lastname;
  private String email;
  private String password;
  private Timestamp createdAt;
  private Timestamp updatedAt;
  private Timestamp deletedAt;

  public JsonObject toJson() {
    JsonObject json = new JsonObject()
        .put("userId", userId)
        .put("firstname", firstname)
        .put("lastname", lastname)
        .put("email", email);

    if (createdAt != null) {
      json.put("createdAt", createdAt.toString());
    }
    if (updatedAt != null) {
      json.put("updatedAt", updatedAt.toString());
    }
    if (deletedAt != null) {
      json.put("deletedAt", deletedAt.toString());
    }

    return json;
  }

  public static User fromJson(JsonObject json) {
    if (json == null) {
      return null;
    }

    User user = new User();
    user.setUserId(json.getInteger("userId"));
    user.setFirstname(json.getString("firstname"));
    user.setLastname(json.getString("lastname"));
    user.setEmail(json.getString("email"));

    user.setCreatedAt(parseTimestamp(json, "createdAt"));
    user.setUpdatedAt(parseTimestamp(json, "updatedAt"));
    user.setDeletedAt(parseTimestamp(json, "deletedAt"));

    return user;
  }

  public static User fromRow(Row row) {
    if (row == null)
      return null;

    Integer userId = row.getInteger("user_id");
    if (userId == null)
      userId = row.getInteger("userId");

    String firstname = row.getString("firstname");
    String lastname = row.getString("lastname");
    String email = row.getString("email");
    String password = row.getString("password");

    Timestamp createdAt = null;
    LocalDateTime createdAtLocal = row.get(LocalDateTime.class, "created_at");
    if (createdAtLocal != null) {
      createdAt = Timestamp.valueOf(createdAtLocal);
    }

    Timestamp updatedAt = null;
    LocalDateTime updatedAtLocal = row.get(LocalDateTime.class, "updated_at");
    if (updatedAtLocal != null) {
      updatedAt = Timestamp.valueOf(updatedAtLocal);
    }

    Timestamp deletedAt = null;
    LocalDateTime deletedAtLocal = row.get(LocalDateTime.class, "deleted_at");
    if (deletedAtLocal != null) {
      deletedAt = Timestamp.valueOf(deletedAtLocal);
    }

    return User.builder()
        .userId(userId)
        .firstname(firstname)
        .lastname(lastname)
        .email(email)
        .password(password)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .deletedAt(deletedAt)
        .build();
  }

  private static Timestamp parseTimestamp(JsonObject json, String field) {
    Object value = json.getValue(field);

    if (value == null) {
      return null;
    }

    if (value instanceof Timestamp ts) {
      return ts;
    }

    if (value instanceof String str && !str.isBlank()) {
      try {
        return Timestamp.from(Instant.parse(str));
      } catch (DateTimeParseException e) {
        return null;
      }
    }

    if (value instanceof Number num) {
      return new Timestamp(num.longValue());
    }

    return null;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
