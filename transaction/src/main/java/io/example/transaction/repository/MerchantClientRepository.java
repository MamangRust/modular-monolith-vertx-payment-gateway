package io.example.transaction.repository;

import io.example.common.exception.api.NotFoundException;
import io.vertx.core.Future;
import pb.merchant.Merchant.ApiResponseMerchant;
import pb.merchant.Merchant.FindByApiKeyRequest;
import pb.merchant.VertxMerchantQueryServiceGrpcClient;

public class MerchantClientRepository {
  private final VertxMerchantQueryServiceGrpcClient merchantStub;

  public MerchantClientRepository(VertxMerchantQueryServiceGrpcClient merchantStub) {
    this.merchantStub = merchantStub;
  }

  public Future<ApiResponseMerchant> getMerchantByApiKey(String apiKey) {
    return merchantStub.findByApiKey(FindByApiKeyRequest.newBuilder().setApiKey(apiKey).build())
        .compose(resp -> {
          if (resp.getStatus().equals("error")) {
            return Future.failedFuture(new NotFoundException(resp.getMessage()));
          }
          return Future.succeededFuture(resp);
        });
  }
}
