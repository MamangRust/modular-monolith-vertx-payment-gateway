package io.example.saldo.model;

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
public class Saldo {
  private Integer id;
  private String cardNumber;
  private Long totalBalance;
  private Long withdrawAmount;
  private Timestamp withdrawTime;
  private Timestamp createdAt;
  private Timestamp updatedAt;
  private Timestamp deletedAt;

  public JsonObject toJson() {
    JsonObject json = new JsonObject()
        .put("id", id)
        .put("card_number", cardNumber)
        .put("total_balance", totalBalance)
        .put("withdraw_amount", withdrawAmount);

    if (withdrawTime != null) json.put("withdraw_time", withdrawTime.toString());
    if (createdAt != null) json.put("created_at", createdAt.toString());
    if (updatedAt != null) json.put("updated_at", updatedAt.toString());
    if (deletedAt != null) json.put("deleted_at", deletedAt.toString());

    return json;
  }

  public static Saldo fromJson(JsonObject json) {
    if (json == null) {
      return null;
    }
    return Saldo.builder()
        .id(json.getInteger("id"))
        .cardNumber(json.getString("card_number"))
        .totalBalance(json.getLong("total_balance"))
        .withdrawAmount(json.getLong("withdraw_amount"))
        .withdrawTime(parseTimestamp(json, "withdraw_time"))
        .createdAt(parseTimestamp(json, "created_at"))
        .updatedAt(parseTimestamp(json, "updated_at"))
        .deletedAt(parseTimestamp(json, "deleted_at"))
        .build();
  }

  public static Saldo fromRow(Row row) {
    if (row == null) return null;

    Integer id = row.getInteger("id");
    String cardNumber = row.getString("card_number");
    Long totalBalance = row.getLong("total_balance");
    Long withdrawAmount = row.getLong("withdraw_amount");

    Timestamp withdrawTime = null;
    LocalDateTime withdrawTimeLocal = row.get(LocalDateTime.class, "withdraw_time");
    if (withdrawTimeLocal != null) {
      withdrawTime = Timestamp.valueOf(withdrawTimeLocal);
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

    return Saldo.builder()
        .id(id)
        .cardNumber(cardNumber)
        .totalBalance(totalBalance)
        .withdrawAmount(withdrawAmount)
        .withdrawTime(withdrawTime)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .deletedAt(deletedAt)
        .build();
  }

  private static Timestamp parseTimestamp(JsonObject json, String field) {
    Object value = json.getValue(field);
    if (value == null) return null;
    if (value instanceof Timestamp ts) return ts;
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
}
