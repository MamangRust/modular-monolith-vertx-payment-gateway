package io.example.topup.model;

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
public class Topup {
  private Integer id;
  private String cardNumber;
  private String topupNo;
  private Long topupAmount;
  private String topupMethod;
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
        .put("topup_method", topupMethod);

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
        .topupTime(parseTimestamp(json, "topup_time"))
        .createdAt(parseTimestamp(json, "created_at"))
        .updatedAt(parseTimestamp(json, "updated_at"))
        .deletedAt(parseTimestamp(json, "deleted_at"))
        .build();
  }

  public static Topup fromRow(Row row) {
    if (row == null)
      return null;

    Integer id = row.getInteger("id");
    if (id == null) {
      id = row.getInteger("topup_id");
    }
    String cardNumber = row.getString("card_number");
    String topupNo = row.getString("topup_no");
    Long topupAmount = row.getLong("topup_amount");
    String topupMethod = row.getString("topup_method");

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
        .topupTime(topupTime)
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
