package io.example.card.model;

import java.sql.Timestamp;
import java.time.LocalDate;
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
public class CardReward {
  private Integer rewardId;
  private String cardNumber;
  private UUID txnId;
  private String rewardType;
  private Long amount;
  private String description;
  private LocalDate expiresAt;
  private Timestamp createdAt;

  public static CardReward fromRow(Row row) {
    if (row == null) return null;

    UUID txnId = row.getUUID("txn_id");
    if (txnId == null) {
      String uid = row.getString("txn_id");
      if (uid != null) txnId = UUID.fromString(uid);
    }

    return CardReward.builder()
        .rewardId(row.getInteger("reward_id"))
        .cardNumber(row.getString("card_number"))
        .txnId(txnId)
        .rewardType(row.getString("reward_type"))
        .amount(row.getLong("amount"))
        .description(row.getString("description"))
        .expiresAt(row.getLocalDate("expires_at"))
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
        .put("reward_id", rewardId)
        .put("card_number", cardNumber)
        .put("txn_id", txnId != null ? txnId.toString() : null)
        .put("reward_type", rewardType)
        .put("amount", amount)
        .put("description", description)
        .put("expires_at", expiresAt != null ? expiresAt.toString() : null)
        .put("created_at", createdAt != null ? createdAt.toString() : null);
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}
