package io.example.auth.model;

import java.time.LocalDateTime;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefreshToken {
  private Integer refreshTokenId;
  private Integer userId;
  private String token;
  private LocalDateTime expiration;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime deletedAt;

  public static RefreshToken fromRow(Row row) {
    if (row == null) {
      return null;
    }

    Integer refreshTokenId = row.getInteger("refresh_token_id");
    if (refreshTokenId == null) {
      refreshTokenId = row.getInteger("refreshTokenId");
    }

    Integer userId = row.getInteger("user_id");
    if (userId == null) {
      userId = row.getInteger("userId");
    }

    return RefreshToken.builder()
        .refreshTokenId(refreshTokenId)
        .userId(userId)
        .token(row.getString("token"))
        .expiration(row.getLocalDateTime("expiration"))
        .createdAt(row.getLocalDateTime("created_at"))
        .updatedAt(row.getLocalDateTime("updated_at"))
        .deletedAt(row.getLocalDateTime("deleted_at"))
        .build();
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject()
        .put("refreshTokenId", refreshTokenId)
        .put("userId", userId)
        .put("token", token);

    if (expiration != null) json.put("expiration", expiration.toString());
    if (createdAt != null) json.put("createdAt", createdAt.toString());
    if (updatedAt != null) json.put("updatedAt", updatedAt.toString());
    if (deletedAt != null) json.put("deletedAt", deletedAt.toString());

    return json;
  }
}
