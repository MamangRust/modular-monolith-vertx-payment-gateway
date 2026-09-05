package io.example.transaction.model;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
  private Integer id;
  private String transactionNo;
  private String cardNumber;
  private Long amount;
  private String paymentMethod;
  private Integer merchantId;
  private String status;
  private String idempotencyKey;

  private OffsetDateTime transactionTime;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;
  private OffsetDateTime deletedAt;

  public JsonObject toJson() {
    JsonObject json = new JsonObject()
        .put("id", id)
        .put("transaction_no", transactionNo)
        .put("card_number", cardNumber)
        .put("amount", amount)
        .put("payment_method", paymentMethod)
        .put("merchant_id", merchantId)
        .put("status", status)
        .put("idempotency_key", idempotencyKey);

    if (transactionTime != null) json.put("transaction_time", transactionTime.toString());
    if (createdAt != null) json.put("created_at", createdAt.toString());
    if (updatedAt != null) json.put("updated_at", updatedAt.toString());
    if (deletedAt != null) json.put("deleted_at", deletedAt.toString());

    return json;
  }

  public static Transaction fromJson(JsonObject json) {
    if (json == null) return null;

    return Transaction.builder()
        .id(json.getInteger("id"))
        .transactionNo(json.getString("transaction_no"))
        .cardNumber(json.getString("card_number"))
        .amount(json.getLong("amount"))
        .paymentMethod(json.getString("payment_method"))
        .merchantId(json.getInteger("merchant_id"))
        .status(json.getString("status"))
        .idempotencyKey(json.getString("idempotency_key"))
        .transactionTime(parseTime(json.getString("transaction_time")))
        .createdAt(parseTime(json.getString("created_at")))
        .updatedAt(parseTime(json.getString("updated_at")))
        .deletedAt(parseTime(json.getString("deleted_at")))
        .build();
  }

  public static Transaction fromRow(Row row) {
    if (row == null) return null;

    Integer rowId = row.getInteger("transaction_id");
    if (rowId == null) {
      try { rowId = row.getInteger("id"); } catch (Exception ignored) {}
    }

    return Transaction.builder()
        .id(rowId)
        .transactionNo(row.getUUID("transaction_no") != null ? row.getUUID("transaction_no").toString() : row.getString("transaction_no"))
        .cardNumber(row.getString("card_number"))
        .amount(row.getLong("amount"))
        .paymentMethod(row.getString("payment_method"))
        .merchantId(row.getInteger("merchant_id"))
        .status(row.getString("status"))
        .idempotencyKey(row.getString("idempotency_key"))
        .transactionTime(toOffsetDateTime(row, "transaction_time"))
        .createdAt(toOffsetDateTime(row, "created_at"))
        .updatedAt(toOffsetDateTime(row, "updated_at"))
        .deletedAt(toOffsetDateTime(row, "deleted_at"))
        .build();
  }

  private static OffsetDateTime toOffsetDateTime(Row row, String col) {
    try {
      LocalDateTime ldt = row.getLocalDateTime(col);
      if (ldt != null) return ldt.atOffset(ZoneOffset.UTC);
    } catch (Exception e) {
      try {
        return row.getOffsetDateTime(col);
      } catch (Exception ignored) {}
    }
    return null;
  }

  private static OffsetDateTime parseTime(String s) {
    if (s == null || s.isBlank()) return null;
    try { return OffsetDateTime.parse(s); } catch (Exception e) { return null; }
  }
}
