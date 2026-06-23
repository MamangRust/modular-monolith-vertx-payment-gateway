package io.example.merchant.handler;

import com.google.protobuf.Empty;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.merchant.service.MerchantCommandService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.merchant.Merchant.ApiResponseMerchant;
import pb.merchant.Merchant.ApiResponseMerchantDeleteAt;
import pb.merchant.Merchant.FindByIdMerchantRequest;
import pb.merchant.MerchantCommand.ApiResponseMerchantAll;
import pb.merchant.MerchantCommand.ApiResponseMerchantDelete;
import pb.merchant.MerchantCommand.CreateMerchantRequest;
import pb.merchant.MerchantCommand.UpdateMerchantRequest;
import pb.merchant.MerchantCommand.UpdateMerchantStatusRequest;

@RequiredArgsConstructor
public class MerchantCommandHandler
    implements pb.merchant.VertxMerchantCommandServiceGrpcServer.MerchantCommandServiceApi {
  private final MerchantCommandService service;

  @Override
  public Future<ApiResponseMerchant> createMerchant(CreateMerchantRequest req) {
    return service.createMerchant(req)
        .map(data -> ApiResponseMerchant.newBuilder().setStatus("success").setMessage("OK")
            .setData(ProtoConverter.fromMerchantResponse(data)).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseMerchant> updateMerchant(UpdateMerchantRequest req) {
    return service.updateMerchant(req)
        .map(data -> ApiResponseMerchant.newBuilder().setStatus("success").setMessage("OK")
            .setData(ProtoConverter.fromMerchantResponse(data)).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseMerchant> updateMerchantStatus(UpdateMerchantStatusRequest req) {
    return service.updateMerchantStatus(req)
        .map(data -> ApiResponseMerchant.newBuilder().setStatus("success").setMessage("OK")
            .setData(ProtoConverter.fromMerchantResponse(data)).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseMerchantDeleteAt> trashedMerchant(FindByIdMerchantRequest req) {
    return service.trashedMerchant(req.getMerchantId())
        .map(data -> ApiResponseMerchantDeleteAt.newBuilder().setStatus("success").setMessage("OK")
            .setData(ProtoConverter.fromMerchantResponseDeleteAt(data)).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseMerchantDeleteAt> restoreMerchant(FindByIdMerchantRequest req) {
    return service.restoreMerchant(req.getMerchantId())
        .map(data -> ApiResponseMerchantDeleteAt.newBuilder().setStatus("success").setMessage("OK")
            .setData(ProtoConverter.fromMerchantResponseDeleteAt(data)).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseMerchantDelete> deleteMerchantPermanent(FindByIdMerchantRequest req) {
    return service.deleteMerchantPermanent(req.getMerchantId())
        .map(v -> ApiResponseMerchantDelete.newBuilder().setStatus("success").setMessage("OK").build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseMerchantAll> restoreAllMerchant(Empty req) {
    return service.restoreAllMerchant()
        .map(v -> ApiResponseMerchantAll.newBuilder().setStatus("success").setMessage("OK").build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseMerchantAll> deleteAllMerchantPermanent(Empty req) {
    return service.deleteAllMerchantPermanent()
        .map(v -> ApiResponseMerchantAll.newBuilder().setStatus("success").setMessage("OK").build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}