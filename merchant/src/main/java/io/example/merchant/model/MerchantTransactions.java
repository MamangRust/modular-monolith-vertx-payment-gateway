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
public class MerchantTransactions {
  private Integer transactionId;
  private String cardNumber;
  private Long amount;
  private String paymentMethod;
  private Integer merchantId;
  private String merchantName;
  private Timestamp transactionTime;
  private Timestamp createdAt;
  private Timestamp updatedAt;
  private Timestamp deletedAt;

  public JsonObject toJson() {
    JsonObject json = new JsonObject()
        .put("transaction_id", transactionId)
        .put("card_number", cardNumber)
        .put("amount", amount)
        .put("payment_method", paymentMethod)
        .put("merchant_id", merchantId)
        .put("merchant_name", merchantName);

    if (transactionTime != null) {
      json.put("transaction_time", transactionTime.toInstant().toString());
    }
    if (createdAt != null) {
      json.put("created_at", createdAt.toInstant().toString());
    }
    if (updatedAt != null) {
      json.put("updated_at", updatedAt.toInstant().toString());
    }
    if (deletedAt != null) {
      json.put("deleted_at", deletedAt.toInstant().toString());
    }

    return json;
  }

  public static MerchantTransactions fromJson(JsonObject json) {
    if (json == null) {
      return null;
    }

    return MerchantTransactions.builder()
        .transactionId(json.getInteger("transaction_id"))
        .cardNumber(json.getString("card_number"))
        .amount(json.getLong("amount"))
        .paymentMethod(json.getString("payment_method"))
        .merchantId(json.getInteger("merchant_id"))
        .merchantName(json.getString("merchant_name"))
        .transactionTime(parseTimestamp(json, "transaction_time"))
        .createdAt(parseTimestamp(json, "created_at"))
        .updatedAt(parseTimestamp(json, "updated_at"))
        .deletedAt(parseTimestamp(json, "deleted_at"))
        .build();
  }

  public static MerchantTransactions fromRow(Row row) {
    if (row == null)
      return null;

    Integer transactionId = row.getInteger("transaction_id");
    String cardNumber = row.getString("card_number");
    Long amount = row.getLong("amount");
    String paymentMethod = row.getString("payment_method");
    Integer merchantId = row.getInteger("merchant_id");
    String merchantName = row.getString("merchant_name");

    Timestamp transactionTime = null;
    LocalDateTime transactionTimeLocal = row.get(LocalDateTime.class, "transaction_time");
    if (transactionTimeLocal != null) {
      transactionTime = Timestamp.valueOf(transactionTimeLocal);
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

    return MerchantTransactions.builder()
        .transactionId(transactionId)
        .cardNumber(cardNumber)
        .amount(amount)
        .paymentMethod(paymentMethod)
        .merchantId(merchantId)
        .merchantName(merchantName)
        .transactionTime(transactionTime)
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
