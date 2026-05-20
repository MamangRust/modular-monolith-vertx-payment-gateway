package io.example.card.repository;

import io.vertx.core.Future;

public interface UserClientRepository {
  Future<Object> getUserById(Integer userId);
}
