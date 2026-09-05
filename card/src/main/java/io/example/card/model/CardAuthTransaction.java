package io.example.card.model;

import java.sql.Timestamp;
import java.time.LocalDateTime;
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
public class CardAuthTransaction {
  private UUID txnId;
  private String cardNumber;
  private Integer merchantId;
  private Long amount;
  private String currency;
  private String status;
  private String declineCode;
  private String authCode;
  private String posEntryMode;
  private String mcc;
  private Integer riskScore;
  private String idempotencyKey;
  private Timestamp txnTime;
  private Timestamp settledAt;
  private Timestamp createdAt;

  public static CardAuthTransaction fromRow(Row row) {
    if (row == null) return null;

    UUID txnId = row.getUUID("txn_id");
    if (txnId == null) {
      String uid = row.getString("txn_id");
      if (uid != null) txnId = UUID.fromString(uid);
    }

    return CardAuthTransaction.builder()
        .txnId(txnId)
        .cardNumber(row.getString("card_number"))
        .merchantId(row.getInteger("merchant_id"))
        .amount(row.getLong("amount"))
        .currency(row.getString("currency"))
        .status(row.getString("status"))
        .declineCode(row.getString("decline_code"))
        .authCode(row.getString("auth_code"))
        .posEntryMode(row.getString("pos_entry_mode"))
        .mcc(row.getString("mcc"))
        .riskScore(row.getInteger("risk_score"))
        .idempotencyKey(row.getString("idempotency_key"))
        .txnTime(toTimestamp(row.getLocalDateTime("txn_time")))
        .settledAt(toTimestamp(row.getLocalDateTime("settled_at")))
        .createdAt(toTimestamp(row.getLocalDateTime("created_at")))
        .build();
  }

  /**
   * The Vert.x pg client does not support {@code row.get(Timestamp.class, ...)}
   * (throws UnsupportedOperationException); timestamps must be read as
   * {@link LocalDateTime} and converted.
   */
  private static Timestamp toTimestamp(LocalDateTime localDateTime) {
    return localDateTime != null ? Timestamp.valueOf(localDateTime) : null;
  }

  public JsonObject toJson() {
    return new JsonObject()
        .put("txn_id", txnId != null ? txnId.toString() : null)
        .put("card_number", cardNumber)
        .put("merchant_id", merchantId)
        .put("amount", amount)
        .put("currency", currency)
        .put("status", status)
        .put("decline_code", declineCode)
        .put("auth_code", authCode)
        .put("pos_entry_mode", posEntryMode)
        .put("mcc", mcc)
        .put("risk_score", riskScore)
        .put("idempotency_key", idempotencyKey)
        .put("txn_time", txnTime != null ? txnTime.toString() : null)
        .put("settled_at", settledAt != null ? settledAt.toString() : null)
        .put("created_at", createdAt != null ? createdAt.toString() : null);
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
