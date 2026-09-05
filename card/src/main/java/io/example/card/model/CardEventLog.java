package io.example.card.model;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CardEventLog {
  private final Long eventId;
  private final String topic;
  private final String eventType;
  private final String cardNumber;
  private final String referenceId;
  private final JsonObject payload;
  private final LocalDateTime receivedAt;

  public static CardEventLog fromRow(Row row) {
    return CardEventLog.builder()
        .eventId(row.getLong("event_id"))
        .topic(row.getString("topic"))
        .eventType(row.getString("event_type"))
        .cardNumber(row.getString("card_number"))
        .referenceId(row.getString("reference_id"))
        .payload(row.get(JsonObject.class, "payload"))
        .receivedAt(row.getLocalDateTime("received_at"))
        .build();
  }
}
