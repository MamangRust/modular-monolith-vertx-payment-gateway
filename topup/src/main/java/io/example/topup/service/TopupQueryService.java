package io.example.topup.service;

import java.util.List;
import io.example.common.model.ApiResponse;
import io.example.common.model.ApiResponsePagination;
import io.example.topup.model.TopupResponse;
import io.example.topup.model.TopupResponseDeleteAt;
import io.vertx.core.Future;
import pb.topup.TopupQuery.FindAllTopupRequest;

public interface TopupQueryService {
  Future<ApiResponsePagination<List<TopupResponse>>> getTopups(FindAllTopupRequest req);
  Future<ApiResponsePagination<List<TopupResponse>>> getActiveTopups(FindAllTopupRequest req);
  Future<ApiResponsePagination<List<TopupResponseDeleteAt>>> getTrashedTopups(FindAllTopupRequest req);
  Future<ApiResponse<TopupResponse>> getTopupById(Integer topupId);
}
