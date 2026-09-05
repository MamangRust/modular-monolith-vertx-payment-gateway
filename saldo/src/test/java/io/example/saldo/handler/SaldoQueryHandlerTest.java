package io.example.saldo.handler;

import io.example.common.domain.PagedResult;
import io.example.saldo.model.SaldoResponse;
import io.example.saldo.model.SaldoResponseDeleteAt;
import io.example.saldo.service.SaldoQueryService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pb.card.Card.FindByCardNumberRequest;
import pb.saldo.Saldo.FindAllSaldoRequest;
import pb.saldo.Saldo.FindByIdSaldoRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class SaldoQueryHandlerTest {

  @Mock
  private SaldoQueryService service;

  private SaldoQueryHandler handler;

  @BeforeEach
  void setUp() {
    handler = new SaldoQueryHandler(service);
  }

  private static SaldoResponse aResp(int id, String cardNumber) {
    return SaldoResponse.builder()
        .id(id)
        .cardNumber(cardNumber)
        .totalBalance(1_000_000L)
        .createdAt("2026-06-26T10:00:00Z")
        .updatedAt("2026-06-26T10:00:00Z")
        .build();
  }

  private static SaldoResponseDeleteAt aRespDeleteAt(int id, String cardNumber) {
    return SaldoResponseDeleteAt.builder()
        .id(id)
        .cardNumber(cardNumber)
        .totalBalance(1_000_000L)
        .createdAt("2026-06-26T10:00:00Z")
        .updatedAt("2026-06-26T10:00:00Z")
        .build();
  }

  @Test
  @DisplayName("findAllSaldo returns paginated response")
  void findAllSaldo(VertxTestContext ctx) {
    var data = List.of(aResp(1, "4111111111111111"), aResp(2, "4111111111111112"));
    when(service.getAllSaldos(any())).thenReturn(Future.succeededFuture(new PagedResult<>(data, 2)));

    var req = FindAllSaldoRequest.newBuilder().setPage(1).setPageSize(10).build();
    handler.findAllSaldo(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(2);
          assertThat(resp.getData(0).getCardNumber()).isEqualTo("4111111111111111");
          assertThat(resp.getPaginationMeta().getTotalRecords()).isEqualTo(2);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByIdSaldo returns saldo response")
  void findByIdSaldo(VertxTestContext ctx) {
    when(service.getSaldoById(1)).thenReturn(Future.succeededFuture(aResp(1, "4111111111111111")));

    var req = FindByIdSaldoRequest.newBuilder().setSaldoId(1).build();
    handler.findByIdSaldo(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByCardNumber returns saldo response")
  void findByCardNumber(VertxTestContext ctx) {
    when(service.getSaldoByCardNumber("4111111111111111"))
        .thenReturn(Future.succeededFuture(aResp(1, "4111111111111111")));

    var req = FindByCardNumberRequest.newBuilder().setCardNumber("4111111111111111").build();
    handler.findByCardNumber(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getSaldoId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByActive returns paginated active saldos")
  void findByActive(VertxTestContext ctx) {
    var data = List.of(aRespDeleteAt(1, "4111111111111111"));
    when(service.getActiveSaldos(any())).thenReturn(Future.succeededFuture(new PagedResult<>(data, 1)));

    var req = FindAllSaldoRequest.newBuilder().setPage(1).setPageSize(10).build();
    handler.findByActive(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByTrashed returns paginated trashed saldos")
  void findByTrashed(VertxTestContext ctx) {
    var data = List.of(aRespDeleteAt(1, "4111111111111111"));
    when(service.getTrashedSaldos(any())).thenReturn(Future.succeededFuture(new PagedResult<>(data, 1)));

    var req = FindAllSaldoRequest.newBuilder().setPage(1).setPageSize(10).build();
    handler.findByTrashed(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }
}
