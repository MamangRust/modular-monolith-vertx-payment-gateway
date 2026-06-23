package io.example.withdraw.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.withdraw.model.WithdrawResponse;
import io.example.withdraw.model.WithdrawResponseDeleteAt;
import io.example.withdraw.service.WithdrawCommandService;
import io.vertx.core.Future;
import pb.withdraw.Withdraw.*;
import pb.withdraw.WithdrawCommand.*;

public class WithdrawCommandHandler
    implements pb.withdraw.VertxWithdrawCommandServiceGrpcServer.WithdrawCommandServiceApi {

  private final WithdrawCommandService service;

  public WithdrawCommandHandler(WithdrawCommandService service) {
    this.service = service;
  }

  @Override
  public Future<ApiResponseWithdraw> createWithdraw(CreateWithdrawRequest req) {
    var domainReq = io.example.withdraw.domain.requests.CreateWithdrawRequest.builder()
        .cardNumber(req.getCardNumber())
        .withdrawAmount(req.getWithdrawAmount())
        .build();
    return service.createWithdraw(domainReq)
        .map(this::toSuccessResponse)
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseWithdraw> updateWithdraw(UpdateWithdrawRequest req) {
    var domainReq = io.example.withdraw.domain.requests.UpdateWithdrawRequest.builder()
        .withdrawId(req.getWithdrawId())
        .cardNumber(req.getCardNumber())
        .withdrawAmount(req.getWithdrawAmount())
        .build();
    return service.updateWithdraw(domainReq)
        .map(this::toSuccessResponse)
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApIResponseWithdrawDeleteAt> trashedWithdraw(FindByIdWithdrawRequest req) {
    return service.trashWithdraw(req.getWithdrawId())
        .map(this::toSuccessDeleteAtResponse)
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApIResponseWithdrawDeleteAt> restoreWithdraw(FindByIdWithdrawRequest req) {
    return service.restoreWithdraw(req.getWithdrawId())
        .map(this::toSuccessDeleteAtResponse)
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseWithdrawDelete> deleteWithdrawPermanent(FindByIdWithdrawRequest req) {
    return service.deleteWithdrawPermanently(req.getWithdrawId())
        .map(v -> ApiResponseWithdrawDelete.newBuilder()
            .setStatus("success")
            .setMessage("Withdrawal deleted permanently")
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseWithdrawAll> restoreAllWithdraw(com.google.protobuf.Empty req) {
    return service.restoreAllWithdraws()
        .map(v -> ApiResponseWithdrawAll.newBuilder()
            .setStatus("success")
            .setMessage("All withdrawals restored successfully")
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseWithdrawAll> deleteAllWithdrawPermanent(com.google.protobuf.Empty req) {
    return service.deleteAllPermanentWithdraws()
        .map(v -> ApiResponseWithdrawAll.newBuilder()
            .setStatus("success")
            .setMessage("All withdrawals permanently deleted")
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  private ApiResponseWithdraw toSuccessResponse(WithdrawResponse data) {
    return ApiResponseWithdraw.newBuilder()
        .setStatus("success")
        .setMessage("OK")
        .setData(ProtoConverter.fromWithdrawResponse(data))
        .build();
  }

  private ApIResponseWithdrawDeleteAt toSuccessDeleteAtResponse(WithdrawResponseDeleteAt data) {
    return ApIResponseWithdrawDeleteAt.newBuilder()
        .setStatus("success")
        .setMessage("OK")
        .setData(ProtoConverter.fromWithdrawResponseDeleteAt(data))
        .build();
  }
}