package io.example.merchant.handler;

import io.example.common.domain.PagedResult;
import io.example.merchant.service.MerchantTransactionService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pb.merchant.Merchant.FindAllMerchantTransaction;
import pb.merchant.Merchant.FindAllMerchantTransactionApikey;
import pb.merchant.Merchant.FindAllMerchantTransactionId;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class MerchantStatsTransactionHandlerTest {

  @Mock
  private MerchantTransactionService service;

  private MerchantStatsTransactionHandler handler;

  @BeforeEach
  void setUp() {
    handler = new MerchantStatsTransactionHandler(service);
  }

  @Test
  @DisplayName("findAllTransactionMerchant success")
  void findAllTransactionMerchantSuccess(VertxTestContext ctx) {
    PagedResult<io.example.merchant.model.MerchantTransactions> paged = new PagedResult<>(List.of(), 0);
    when(service.getTransactions(any())).thenReturn(Future.succeededFuture(paged));

    var req = FindAllMerchantTransaction.newBuilder().setPage(1).setPageSize(10).build();
    handler.findAllTransactionMerchant(req)
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          assertThat(res.getDataCount()).isEqualTo(0);
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }

  @Test
  @DisplayName("findAllTransactionByApikey success")
  void findAllTransactionByApikeySuccess(VertxTestContext ctx) {
    PagedResult<io.example.merchant.model.MerchantTransactions> paged = new PagedResult<>(List.of(), 0);
    when(service.getTransactionsByApiKey(any())).thenReturn(Future.succeededFuture(paged));

    var req = FindAllMerchantTransactionApikey.newBuilder().setApiKey("key").setPage(1).setPageSize(10).build();
    handler.findAllTransactionByApikey(req)
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }

  @Test
  @DisplayName("findAllTransactionByMerchant success")
  void findAllTransactionByMerchantSuccess(VertxTestContext ctx) {
    PagedResult<io.example.merchant.model.MerchantTransactions> paged = new PagedResult<>(List.of(), 0);
    when(service.getTransactionsByMerchantId(any())).thenReturn(Future.succeededFuture(paged));

    var req = FindAllMerchantTransactionId.newBuilder().setId(10).setPage(1).setPageSize(10).build();
    handler.findAllTransactionByMerchant(req)
        .onSuccess(res -> ctx.verify(() -> {
          assertThat(res.getStatus()).isEqualTo("success");
          ctx.completeNow();
        }))
        .onFailure(ctx::failNow);
  }
}
