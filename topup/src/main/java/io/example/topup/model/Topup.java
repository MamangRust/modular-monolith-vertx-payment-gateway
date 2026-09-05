package io.example.topup.model;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.NoSuchElementException;
import java.util.UUID;

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
public class Topup {
  private Integer id;
  private String cardNumber;
  private String topupNo;
  private Long topupAmount;
  private String topupMethod;
  private String status;
  private String idempotencyKey;
  private Timestamp topupTime;
  private Timestamp createdAt;
  private Timestamp updatedAt;
  private Timestamp deletedAt;

  public JsonObject toJson() {
    JsonObject json = new JsonObject()
        .put("id", id)
        .put("card_number", cardNumber)
        .put("topup_no", topupNo)
        .put("topup_amount", topupAmount)
        .put("topup_method", topupMethod)
        .put("status", status);

    if (topupTime != null) {
      json.put("topup_time", topupTime.toString());
    }
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

  public static Topup fromJson(JsonObject json) {
    if (json == null) {
      return null;
    }

    return Topup.builder()
        .id(json.getInteger("id"))
        .cardNumber(json.getString("card_number"))
        .topupNo(json.getString("topup_no"))
        .topupAmount(json.getLong("topup_amount"))
        .topupMethod(json.getString("topup_method"))
        .status(json.getString("status"))
        .idempotencyKey(json.getString("idempotency_key"))
        .topupTime(parseTimestamp(json, "topup_time"))
        .createdAt(parseTimestamp(json, "created_at"))
        .updatedAt(parseTimestamp(json, "updated_at"))
        .deletedAt(parseTimestamp(json, "deleted_at"))
        .build();
  }

  public static Topup fromRow(Row row) {
    if (row == null)
      return null;

    // PK column in the "topups" table is "topup_id" (see V12 migration).
    // Read it first; fall back to "id" only when a query aliases topup_id AS id.
    // Row.getInteger throws NoSuchElementException for a missing column, not null.
    Integer id = readId(row);
    String cardNumber = row.getString("card_number");
    // "topup_no" is UUID in the topups table (V12 migration), so getString would
    // throw ClassCastException. Read as UUID and fall back to text for aliased/cast queries.
    String topupNo = readTopupNo(row);
    Long topupAmount = row.getLong("topup_amount");
    String topupMethod = row.getString("topup_method");
    String status = row.getString("status");
    String idempotencyKey = row.getString("idempotency_key");

    Timestamp topupTime = null;
    LocalDateTime topupTimeLocal = row.get(LocalDateTime.class, "topup_time");
    if (topupTimeLocal != null) {
      topupTime = Timestamp.valueOf(topupTimeLocal);
    }

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

    return Topup.builder()
        .id(id)
        .cardNumber(cardNumber)
        .topupNo(topupNo)
        .topupAmount(topupAmount)
        .topupMethod(topupMethod)
        .status(status)
        .idempotencyKey(idempotencyKey)
        .topupTime(topupTime)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .deletedAt(deletedAt)
        .build();
  }

  static String readTopupNo(Row row) {
    try {
      UUID uuid = row.getUUID("topup_no");
      if (uuid != null) {
        return uuid.toString();
      }
    } catch (ClassCastException | IllegalArgumentException ignored) {
      // column already text/varchar, fall through to getString below
    }
    return row.getString("topup_no");
  }

  static Integer readId(Row row) {
    try {
      Integer id = row.getInteger("topup_id");
      if (id != null) {
        return id;
      }
    } catch (NoSuchElementException ignored) {
      // query aliased topup_id AS id
    }
    try {
      return row.getInteger("id");
    } catch (NoSuchElementException ignored) {
      return null;
    }
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
