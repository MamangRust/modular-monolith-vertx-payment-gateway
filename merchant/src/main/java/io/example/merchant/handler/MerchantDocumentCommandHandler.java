package io.example.merchant.handler;

import com.google.protobuf.Empty;
import io.example.common.grpc.GrpcExceptionMapper;
import io.example.merchant.service.MerchantDocumentCommandService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.merchant_document.MerchantDocumentCommand.ApiResponseMerchantDocumentAll;
import pb.merchant_document.MerchantDocumentCommand.ApiResponseMerchantDocumentDelete;
import pb.merchant_document.MerchantDocumentCommand.CreateMerchantDocumentRequest;
import pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentRequest;
import pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentStatusRequest;
import pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocument;
import pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocumentDeleteAt;
import pb.merchant_document.MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest;

@RequiredArgsConstructor
public class MerchantDocumentCommandHandler
    implements pb.merchant_document.VertxMerchantDocumentCommandServiceGrpcServer.MerchantDocumentCommandServiceApi {
  private final MerchantDocumentCommandService service;

  @Override
  public Future<ApiResponseMerchantDocument> create(CreateMerchantDocumentRequest req) {
    return service.createMerchantDocument(req)
        .map(data -> ApiResponseMerchantDocument.newBuilder().setStatus("success").setMessage("OK")
            .setData(ProtoConverter.fromDocumentResponse(data)).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseMerchantDocument> update(UpdateMerchantDocumentRequest req) {
    return service.updateMerchantDocument(req)
        .map(data -> ApiResponseMerchantDocument.newBuilder().setStatus("success").setMessage("OK")
            .setData(ProtoConverter.fromDocumentResponse(data)).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseMerchantDocument> updateStatus(UpdateMerchantDocumentStatusRequest req) {
    return service.updateMerchantDocumentStatus(req)
        .map(data -> ApiResponseMerchantDocument.newBuilder().setStatus("success").setMessage("OK")
            .setData(ProtoConverter.fromDocumentResponse(data)).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseMerchantDocumentDeleteAt> trashed(FindMerchantDocumentByIdRequest req) {
    return service.trashedMerchantDocument((int) req.getDocumentId())
        .map(data -> ApiResponseMerchantDocumentDeleteAt.newBuilder().setStatus("success").setMessage("OK")
            .setData(ProtoConverter.fromDocumentResponseAt(data)).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseMerchantDocumentDeleteAt> restore(FindMerchantDocumentByIdRequest req) {
    return service.restoreMerchantDocument((int) req.getDocumentId())
        .map(data -> ApiResponseMerchantDocumentDeleteAt.newBuilder().setStatus("success").setMessage("OK")
            .setData(ProtoConverter.fromDocumentResponseAt(data)).build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseMerchantDocumentDelete> deletePermanent(FindMerchantDocumentByIdRequest req) {
    return service.deleteMerchantDocumentPermanent((int) req.getDocumentId())
        .map(v -> ApiResponseMerchantDocumentDelete.newBuilder().setStatus("success").setMessage("OK").build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseMerchantDocumentAll> restoreAll(Empty req) {
    return service.restoreAllMerchantDocument()
        .map(v -> ApiResponseMerchantDocumentAll.newBuilder().setStatus("success").setMessage("OK").build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseMerchantDocumentAll> deleteAllPermanent(Empty req) {
    return service.deleteAllMerchantDocumentPermanent()
        .map(v -> ApiResponseMerchantDocumentAll.newBuilder().setStatus("success").setMessage("OK").build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}