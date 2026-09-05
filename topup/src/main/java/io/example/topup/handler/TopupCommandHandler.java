package io.example.topup.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.topup.service.TopupCommandService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.topup.Topup.ApiResponseTopup;
import pb.topup.Topup.ApiResponseTopupDeleteAt;
import pb.topup.Topup.FindByIdTopupRequest;
import pb.topup.TopupCommand.ApiResponseTopupAll;
import pb.topup.TopupCommand.ApiResponseTopupDelete;
import pb.topup.TopupCommand.CreateTopupRequest;
import pb.topup.TopupCommand.UpdateTopupRequest;

@RequiredArgsConstructor
public class TopupCommandHandler implements pb.topup.VertxTopupCommandServiceGrpcServer.TopupCommandServiceApi {
  private final TopupCommandService service;

  @Override
  public Future<ApiResponseTopup> createTopup(CreateTopupRequest req) {
    var domainReq = io.example.topup.domain.requests.topup.CreateTopupRequest.builder()
        .cardNumber(req.getCardNumber())
        .topupNo(req.getTopupNo())
        .topupAmount(req.getTopupAmount())
        .topupMethod(req.getTopupMethod())
        .idempotencyKey(req.getIdempotencyKey())
        .build();

    return service.createTopup(domainReq)
        .map(data -> ApiResponseTopup.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromTopupResponse(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTopup> updateTopup(UpdateTopupRequest req) {
    var domainReq = io.example.topup.domain.requests.topup.UpdateTopupRequest.builder()
        .topupId(req.getTopupId())
        .cardNumber(req.getCardNumber())
        .topupAmount(req.getTopupAmount())
        .topupMethod(req.getTopupMethod())
        .build();

    return service.updateTopup(domainReq)
        .map(data -> ApiResponseTopup.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromTopupResponse(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTopupDeleteAt> trashedTopup(FindByIdTopupRequest req) {
    return service.trashTopup(req.getTopupId())
        .map(data -> ApiResponseTopupDeleteAt.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromTopupResponseDeleteAt(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTopupDeleteAt> restoreTopup(FindByIdTopupRequest req) {
    return service.restoreTopup(req.getTopupId())
        .map(data -> ApiResponseTopupDeleteAt.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromTopupResponseDeleteAt(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTopupDelete> deleteTopupPermanent(FindByIdTopupRequest req) {
    return service.deleteTopupPermanently(req.getTopupId())
        .map(v -> ApiResponseTopupDelete.newBuilder()
            .setStatus("success")
            .setMessage("Topup deleted permanently")
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTopupAll> restoreAllTopup(com.google.protobuf.Empty req) {
    return service.restoreAllTopups()
        .map(v -> ApiResponseTopupAll.newBuilder()
            .setStatus("success")
            .setMessage("All topups restored successfully")
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseTopupAll> deleteAllTopupPermanent(com.google.protobuf.Empty req) {
    return service.deleteAllPermanentTopups()
        .map(v -> ApiResponseTopupAll.newBuilder()
            .setStatus("success")
            .setMessage("All topups permanently deleted")
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}