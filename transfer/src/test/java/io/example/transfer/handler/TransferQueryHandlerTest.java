package io.example.transfer.handler;

import io.example.common.domain.PagedResult;
import io.example.transfer.model.TransferResponse;
import io.example.transfer.model.TransferResponseDeleteAt;
import io.example.transfer.service.TransferQueryService;
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
class TransferQueryHandlerTest {

  @Mock
  private TransferQueryService service;

  private TransferQueryHandler handler;

  @BeforeEach
  void setUp() {
    handler = new TransferQueryHandler(service);
  }

  private static TransferResponse aResp(int id, String cardNumber) {
    return TransferResponse.builder().id(id).transferNo("TRF" + id)
        .transferFrom("4111111111111111").transferTo("5555555555554444")
        .transferAmount(500_000L).status("success")
        .createdAt("2026-06-26T10:00:00Z").updatedAt("2026-06-26T10:00:00Z")
        .build();
  }

  private static TransferResponseDeleteAt aRespDeleteAt(int id, String cardNumber) {
    return TransferResponseDeleteAt.builder().id(id).transferNo("TRF" + id)
        .transferFrom("4111111111111111").transferTo("5555555555554444")
        .transferAmount(500_000L).status("success")
        .createdAt("2026-06-26T10:00:00Z").updatedAt("2026-06-26T10:00:00Z")
        .build();
  }

  @Test
  @DisplayName("findAllTransfer returns paginated response")
  void findAllTransfer(VertxTestContext ctx) {
    var data = List.of(aResp(1, "4111111111111111"), aResp(2, "5555555555554444"));
    when(service.getAllTransfers(any())).thenReturn(Future.succeededFuture(new PagedResult<>(data, 2)));

    var req = pb.transfer.Transfer.FindAllTransferRequest.newBuilder().setPage(1).setPageSize(10).build();
    handler.findAllTransfer(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(2);
          assertThat(resp.getData(0).getTransferFrom()).isEqualTo("4111111111111111");
          assertThat(resp.getPaginationMeta().getTotalRecords()).isEqualTo(2);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByIdTransfer returns transfer response")
  void findByIdTransfer(VertxTestContext ctx) {
    when(service.getTransferById(1)).thenReturn(Future.succeededFuture(aResp(1, "4111111111111111")));

    var req = pb.transfer.Transfer.FindByIdTransferRequest.newBuilder().setTransferId(1).build();
    handler.findByIdTransfer(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getData().getTransferFrom()).isEqualTo("4111111111111111");
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findTransferByTransferFrom returns list of transfers")
  void findTransferByTransferFrom(VertxTestContext ctx) {
    when(service.getTransfersAsSender("4111111111111111"))
        .thenReturn(Future.succeededFuture(List.of(aResp(1, "4111111111111111"))));

    var req = pb.transfer.Transfer.FindTransferByTransferFromRequest.newBuilder()
        .setTransferFrom("4111111111111111").build();
    handler.findTransferByTransferFrom(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findTransferByTransferTo returns list of transfers")
  void findTransferByTransferTo(VertxTestContext ctx) {
    when(service.getTransfersAsReceiver("5555555555554444"))
        .thenReturn(Future.succeededFuture(List.of(aResp(1, "5555555555554444"))));

    var req = pb.transfer.Transfer.FindTransferByTransferToRequest.newBuilder()
        .setTransferTo("5555555555554444").build();
    handler.findTransferByTransferTo(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByActiveTransfer returns paginated active transfers")
  void findByActiveTransfer(VertxTestContext ctx) {
    var data = List.of(aRespDeleteAt(1, "4111111111111111"));
    when(service.getActiveTransfers(any())).thenReturn(Future.succeededFuture(new PagedResult<>(data, 1)));

    var req = pb.transfer.Transfer.FindAllTransferRequest.newBuilder().setPage(1).setPageSize(10).build();
    handler.findByActiveTransfer(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }

  @Test
  @DisplayName("findByTrashedTransfer returns paginated trashed transfers")
  void findByTrashedTransfer(VertxTestContext ctx) {
    var data = List.of(aRespDeleteAt(1, "4111111111111111"));
    when(service.getTrashedTransfers(any())).thenReturn(Future.succeededFuture(new PagedResult<>(data, 1)));

    var req = pb.transfer.Transfer.FindAllTransferRequest.newBuilder().setPage(1).setPageSize(10).build();
    handler.findByTrashedTransfer(req)
        .onComplete(ctx.succeeding(resp -> ctx.verify(() -> {
          assertThat(resp.getStatus()).isEqualTo("success");
          assertThat(resp.getDataCount()).isEqualTo(1);
          ctx.completeNow();
        })));
  }
}
