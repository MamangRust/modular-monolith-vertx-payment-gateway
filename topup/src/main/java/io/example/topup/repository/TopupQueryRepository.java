package io.example.topup.repository;

import io.example.common.domain.PagedResult;
import io.example.topup.model.Topup;
import io.vertx.core.Future;
import io.example.topup.domain.requests.topup.FindAllTopups;

public interface TopupQueryRepository {
  Future<PagedResult<Topup>> getTopups(FindAllTopups req);
  Future<PagedResult<Topup>> getActiveTopups(FindAllTopups req);
  Future<PagedResult<Topup>> getTrashedTopups(FindAllTopups req);
  Future<Topup> getTopupById(int id);
}
