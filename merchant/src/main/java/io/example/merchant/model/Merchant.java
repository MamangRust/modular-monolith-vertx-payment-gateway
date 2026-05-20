package io.example.merchant.model;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

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
public class Merchant {
  private Integer id;
  private String name;
  private String apiKey;
  private Integer userId;
  private String status;
  private Timestamp createdAt;
  private Timestamp updatedAt;
  private Timestamp deletedAt;

  public JsonObject toJson() {
    JsonObject json = new JsonObject()
        .put("id", id)
        .put("name", name)
        .put("api_key", apiKey)
        .put("user_id", userId)
        .put("status", status);

    if (createdAt != null) {
      json.put("created_at", createdAt.toString());
    }
    if (updatedAt != null) {
      json.put("updated_at", updatedAt.toString());
    }
    if (deletedAt != null) {
      json.put("deleted_at", deletedAt.toString());
    }

    return json;
  }

  public static Merchant fromJson(JsonObject json) {
    if (json == null) {
      return null;
    }

    Merchant merchant = new Merchant();
    merchant.setId(json.getInteger("id"));
    merchant.setName(json.getString("name"));
    merchant.setApiKey(json.getString("api_key"));
    merchant.setUserId(json.getInteger("user_id"));
    merchant.setStatus(json.getString("status"));

    merchant.setCreatedAt(parseTimestamp(json, "created_at"));
    merchant.setUpdatedAt(parseTimestamp(json, "updated_at"));
    merchant.setDeletedAt(parseTimestamp(json, "deleted_at"));

    return merchant;
  }

  public static Merchant fromRow(Row row) {
    if (row == null)
      return null;

    Integer id = row.getInteger("id");
    if (id == null) {
      id = row.getInteger("merchant_id");
    }
    String name = row.getString("name");
    String apiKey = row.getString("api_key");
    Integer userId = row.getInteger("user_id");
    String status = row.getString("status");

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

    return Merchant.builder()
        .id(id)
        .name(name)
        .apiKey(apiKey)
        .userId(userId)
        .status(status)
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
