package io.example.topup.service;

import io.example.topup.domain.requests.topup.CreateTopupRequest;
import io.example.topup.domain.requests.topup.UpdateTopupRequest;
import io.example.topup.model.TopupResponse;
import io.example.topup.model.TopupResponseDeleteAt;
import io.vertx.core.Future;

public interface TopupCommandService {
  Future<TopupResponse> createTopup(CreateTopupRequest req);

  Future<TopupResponse> updateTopup(UpdateTopupRequest req);

  Future<TopupResponseDeleteAt> trashTopup(Integer topupId);

  Future<TopupResponseDeleteAt> restoreTopup(Integer topupId);

  Future<Void> deleteTopupPermanently(Integer topupId);

  Future<Void> restoreAllTopups();

  Future<Void> deleteAllPermanentTopups();
}