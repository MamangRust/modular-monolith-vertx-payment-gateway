package io.example.card.repository;

import io.example.card.model.CardEventLog;
import io.vertx.core.Future;

public interface CardEventLogRepository {
  Future<CardEventLog> insert(CardEventLog eventLog);
}
