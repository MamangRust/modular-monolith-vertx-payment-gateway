package io.example.apigateway.middleware;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import pb.merchant.Merchant;
import pb.merchant.VertxMerchantQueryServiceGrpcClient;

public final class ApiKeyMiddleware {
  private ApiKeyMiddleware() {}

  public static Handler<RoutingContext> requireApiKey(VertxMerchantQueryServiceGrpcClient merchantClient) {
    return ctx -> {
      String apiKey = ctx.request().getHeader("X-Api-Key");

      if (apiKey == null || apiKey.isEmpty()) {
        ctx.response().setStatusCode(401).end("API Key is required");
        return;
      }

      var req = Merchant.FindByApiKeyRequest.newBuilder().setApiKey(apiKey).build();
      merchantClient.findByApiKey(req)
          .onSuccess(resp -> {
            if ("200".equals(resp.getStatus()) && resp.hasData()) {
              ctx.put("apiKey", apiKey);
              ctx.put("merchant", resp.getData());
              ctx.next();
            } else {
              ctx.response().setStatusCode(401).end("Invalid API Key");
            }
          })
          .onFailure(err -> {
            ctx.response().setStatusCode(401).end("Invalid API Key");
          });
    };
  }
}
