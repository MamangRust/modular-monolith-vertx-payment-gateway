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
public class CardPayment {
  private UUID paymentId;
  private String referenceId;
  private String cardNumber;
  private Long amount;
  private String paymentChannel;
  private Timestamp paymentTime;
  private String status;
  private Integer statementId;
  private Timestamp createdAt;

  public static CardPayment fromRow(Row row) {
    if (row == null) return null;

    UUID paymentId = row.getUUID("payment_id");
    if (paymentId == null) {
      String uid = row.getString("payment_id");
      if (uid != null) paymentId = UUID.fromString(uid);
    }

    return CardPayment.builder()
        .paymentId(paymentId)
        .referenceId(row.getString("reference_id"))
        .cardNumber(row.getString("card_number"))
        .amount(row.getLong("amount"))
        .paymentChannel(row.getString("payment_channel"))
        .paymentTime(toTimestamp(row.getLocalDateTime("payment_time")))
        .status(row.getString("status"))
        .statementId(row.getInteger("statement_id"))
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
        .put("payment_id", paymentId != null ? paymentId.toString() : null)
        .put("reference_id", referenceId)
        .put("card_number", cardNumber)
        .put("amount", amount)
        .put("payment_channel", paymentChannel)
        .put("payment_time", paymentTime != null ? paymentTime.toString() : null)
        .put("status", status)
        .put("statement_id", statementId)
        .put("created_at", createdAt != null ? createdAt.toString() : null);
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
