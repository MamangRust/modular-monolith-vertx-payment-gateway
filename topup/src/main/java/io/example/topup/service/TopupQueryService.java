package io.example.topup.service;

import io.example.common.domain.PagedResult;
import io.example.topup.domain.requests.topup.FindAllTopups;
import io.example.topup.domain.requests.topup.FindAllTopupsByCardNumber;
import io.example.topup.model.TopupResponse;
import io.example.topup.model.TopupResponseDeleteAt;
import io.vertx.core.Future;

public interface TopupQueryService {
  Future<PagedResult<TopupResponse>> getTopups(FindAllTopups req);

  Future<PagedResult<TopupResponse>> getTopupsByCardNumber(FindAllTopupsByCardNumber req);

  Future<PagedResult<TopupResponseDeleteAt>> getActiveTopups(FindAllTopups req);

  Future<PagedResult<TopupResponseDeleteAt>> getTrashedTopups(FindAllTopups req);

  Future<TopupResponse> getTopupById(Integer topupId);

  Future<TopupResponse> getTopupByCardNumber(String cardNumber);
}