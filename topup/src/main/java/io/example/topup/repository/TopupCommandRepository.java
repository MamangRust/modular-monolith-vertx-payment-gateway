package io.example.topup.repository;

import io.example.topup.model.Topup;
import io.vertx.core.Future;
import io.example.topup.domain.requests.topup.CreateTopupRequest;
import io.example.topup.domain.requests.topup.UpdateTopupAmount;
import io.example.topup.domain.requests.topup.UpdateTopupRequest;
import io.example.topup.domain.requests.topup.UpdateTopupStatus;

public interface TopupCommandRepository {
  Future<Topup> createTopup(CreateTopupRequest req);
  Future<Topup> updateTopup(UpdateTopupRequest req);
  Future<Topup> updateTopupAmount(UpdateTopupAmount req);
  Future<Topup> updateTopupStatus(UpdateTopupStatus req);
  Future<Topup> trashTopup(int id);
  Future<Topup> restoreTopup(int id);
  Future<Void> deleteTopupPermanently(int id);
  Future<Void> restoreAllTopups();
  Future<Void> deleteAllPermanentTopups();
}
