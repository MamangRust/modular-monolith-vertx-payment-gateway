package io.example.topup.handler;

import io.example.common.domain.PagedResult;
import io.example.topup.model.TopupResponse;
import io.example.topup.model.TopupResponseDeleteAt;
import io.example.topup.service.TopupQueryService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class TopupQueryHandlerTest {

  @Mock
  private TopupQueryService service;

  private TopupQueryHandler handler;

  @BeforeEach
  void setUp() {
    handler = new TopupQueryHandler(service);
  }

  private static TopupResponse aResp(int id, String cardNumber, int amount) {
    return TopupResponse.builder()
        .id(id).cardNumber(cardNumber).amount(amount).status("success")
        .createdAt("2026-06-26T10:00:00Z").updatedAt("2026-06-26T10:00:00Z")
        .build();
  }

  private static TopupResponseDeleteAt aRespDel(int id, String cardNumber, int amount) {
    return TopupResponseDeleteAt.builder()
        .id(id).cardNumber(cardNumber).amount(amount).status("success")
        .createdAt("2026-06-26T10:00:00Z").updatedAt("2026-06-26T10:00:00Z")
        .build();
  }

  /* ─── findAllTopup ─── */

  @Test
  @DisplayName("findAllTopup returns paginated response")
  void findAllTopup(VertxTestContext ctx) {
    var data = List.of(aResp(1, "4111111111111111", 50000), aResp(2, "4222222222222222", 75000));
    when(service.getTopups(any())).thenReturn(Future.succeededFuture(new PagedResult<>(data, 2)));

    var req = pb.topup.TopupQuery.FindAllTopupRequest.newBuilder().setPage(1).setPageSize(10).build();
    handler.findAllTopup(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(2);
          assertThat(resp.getData(0).getCardNumber()).isEqualTo("4111111111111111");
          assertThat(resp.getPaginationMeta().getTotalRecords()).isEqualTo(2);
          ctx.completeNow();
        })));
  }

  /* ─── findAllTopupByCardNumber ─── */

  @Test
  @DisplayName("findAllTopupByCardNumber returns paginated response")
  void findAllTopupByCardNumber(VertxTestContext ctx) {
    var data = List.of(aResp(1, "4111111111111111", 50000));
    when(service.getTopupsByCardNumber(any())).thenReturn(Future.succeededFuture(new PagedResult<>(data, 1)));

    var req = pb.topup.TopupQuery.FindAllTopupByCardNumberRequest.newBuilder()
        .setCardNumber("4111111111111111").setPage(1).setPageSize(10).build();
    handler.findAllTopupByCardNumber(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(1);
          assertThat(resp.getData(0).getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  /* ─── findByIdTopup ─── */

  @Test
  @DisplayName("findByIdTopup delegates and returns response")
  void findByIdTopup(VertxTestContext ctx) {
    when(service.getTopupById(1)).thenReturn(Future.succeededFuture(aResp(1, "4111111111111111", 50000)));

    var req = pb.topup.Topup.FindByIdTopupRequest.newBuilder().setTopupId(1).build();
    handler.findByIdTopup(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getId()).isEqualTo(1);
          assertThat(resp.getData().getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  /* ─── findByCardNumberTopup ─── */

  @Test
  @DisplayName("findByCardNumberTopup delegates and returns response")
  void findByCardNumberTopup(VertxTestContext ctx) {
    when(service.getTopupByCardNumber("4111111111111111"))
        .thenReturn(Future.succeededFuture(aResp(1, "4111111111111111", 50000)));

    var req = pb.topup.Topup.FindByCardNumberTopupRequest.newBuilder().setCardNumber("4111111111111111").build();
    handler.findByCardNumberTopup(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getCardNumber()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  /* ─── findByActive ─── */

  @Test
  @DisplayName("findByActive returns paginated active topups")
  void findByActive(VertxTestContext ctx) {
    var data = List.of(aRespDel(1, "4111111111111111", 50000));
    when(service.getActiveTopups(any())).thenReturn(Future.succeededFuture(new PagedResult<>(data, 1)));

    var req = pb.topup.TopupQuery.FindAllTopupRequest.newBuilder().setPage(1).setPageSize(10).build();
    handler.findByActive(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  /* ─── findByTrashed ─── */

  @Test
  @DisplayName("findByTrashed returns paginated trashed topups")
  void findByTrashed(VertxTestContext ctx) {
    var data = List.of(aRespDel(1, "4111111111111111", 50000));
    when(service.getTrashedTopups(any())).thenReturn(Future.succeededFuture(new PagedResult<>(data, 1)));

    var req = pb.topup.TopupQuery.FindAllTopupRequest.newBuilder().setPage(1).setPageSize(10).build();
    handler.findByTrashed(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }
}
