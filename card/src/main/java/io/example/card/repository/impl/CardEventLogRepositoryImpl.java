package io.example.card.repository.impl;

import io.example.card.model.CardEventLog;
import io.example.card.repository.CardEventLogRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CardEventLogRepositoryImpl implements CardEventLogRepository {
  private final Pool pool;

  @Override
  public Future<CardEventLog> insert(CardEventLog eventLog) {
    return pool
        .preparedQuery(
            """
                INSERT INTO card_event_logs (topic, event_type, card_number, reference_id, payload)
                VALUES ($1, $2, $3, $4, $5::jsonb)
                ON CONFLICT DO NOTHING
                RETURNING *
                """)
        .execute(Tuple.of(
            eventLog.getTopic(),
            eventLog.getEventType(),
            eventLog.getCardNumber(),
            eventLog.getReferenceId(),
            eventLog.getPayload() != null ? eventLog.getPayload().encode() : "{}"))
        .map(this::mapSingleOrNull);
  }

  private CardEventLog mapSingleOrNull(RowSet<Row> rows) {
    return rows.iterator().hasNext() ? CardEventLog.fromRow(rows.iterator().next()) : null;
  }
}
