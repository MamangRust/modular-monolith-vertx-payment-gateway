package io.example.apigateway.middleware;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pb.merchant.Merchant;
import pb.merchant.VertxMerchantQueryServiceGrpcClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyMiddlewareTest {

  @Mock
  private VertxMerchantQueryServiceGrpcClient merchantClient;

  @Mock
  private RoutingContext ctx;

  @Mock
  private HttpServerRequest request;

  @Mock
  private HttpServerResponse response;

  @BeforeEach
  void setUp() {
    lenient().when(ctx.request()).thenReturn(request);
    lenient().when(ctx.response()).thenReturn(response);
    lenient().when(response.setStatusCode(anyInt())).thenReturn(response);
  }

  @Test
  @DisplayName("returns 401 when X-Api-Key header is missing")
  void missingApiKeyReturns401() {
    when(request.getHeader("X-Api-Key")).thenReturn(null);

    ApiKeyMiddleware.requireApiKey(merchantClient).handle(ctx);

    verify(response).setStatusCode(401);
    verify(response).end("API Key is required");
    verifyNoInteractions(merchantClient);
  }

  @Test
  @DisplayName("returns 401 when X-Api-Key header is empty")
  void emptyApiKeyReturns401() {
    when(request.getHeader("X-Api-Key")).thenReturn("");

    ApiKeyMiddleware.requireApiKey(merchantClient).handle(ctx);

    verify(response).setStatusCode(401);
    verify(response).end("API Key is required");
    verifyNoInteractions(merchantClient);
  }

  @Test
  @DisplayName("returns 401 when merchant client returns invalid response")
  void invalidApiKeyReturns401() {
    String apiKey = "invalid-key";
    when(request.getHeader("X-Api-Key")).thenReturn(apiKey);

    Merchant.ApiResponseMerchant apiResponse = Merchant.ApiResponseMerchant.newBuilder()
        .setStatus("404")
        .build();

    when(merchantClient.findByApiKey(any(Merchant.FindByApiKeyRequest.class)))
        .thenReturn(Future.succeededFuture(apiResponse));

    ApiKeyMiddleware.requireApiKey(merchantClient).handle(ctx);

    verify(response).setStatusCode(401);
    verify(response).end("Invalid API Key");
  }

  @Test
  @DisplayName("returns 401 when merchant client call fails")
  void clientFailureReturns401() {
    String apiKey = "error-key";
    when(request.getHeader("X-Api-Key")).thenReturn(apiKey);

    when(merchantClient.findByApiKey(any(Merchant.FindByApiKeyRequest.class)))
        .thenReturn(Future.failedFuture(new RuntimeException("gRPC error")));

    ApiKeyMiddleware.requireApiKey(merchantClient).handle(ctx);

    verify(response).setStatusCode(401);
    verify(response).end("Invalid API Key");
  }

  @Test
  @DisplayName("proceeds and puts merchant in context when API key is valid")
  void validApiKeyProceeds() {
    String apiKey = "valid-key";
    when(request.getHeader("X-Api-Key")).thenReturn(apiKey);

    Merchant.MerchantResponse merchantData = Merchant.MerchantResponse.newBuilder()
        .setId(123)
        .setName("Test Merchant")
        .build();

    Merchant.ApiResponseMerchant apiResponse = Merchant.ApiResponseMerchant.newBuilder()
        .setStatus("200")
        .setData(merchantData)
        .build();

    when(merchantClient.findByApiKey(any(Merchant.FindByApiKeyRequest.class)))
        .thenReturn(Future.succeededFuture(apiResponse));

    ApiKeyMiddleware.requireApiKey(merchantClient).handle(ctx);

    verify(ctx).put("apiKey", apiKey);
    verify(ctx).put("merchant", merchantData);
    verify(ctx).next();
  }
}
