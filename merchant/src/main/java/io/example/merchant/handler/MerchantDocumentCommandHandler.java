package io.example.merchant.handler;

import io.example.merchant.service.MerchantDocumentCommandService;
import io.vertx.core.Future;
import pb.merchant_document.MerchantDocumentCommand.ApiResponseMerchantDocumentAll;
import pb.merchant_document.MerchantDocumentCommand.ApiResponseMerchantDocumentDelete;
import pb.merchant_document.MerchantDocumentCommand.CreateMerchantDocumentRequest;
import pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentRequest;
import pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentStatusRequest;
import pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocument;
import pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocumentDeleteAt;
import pb.merchant_document.MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest;

public class MerchantDocumentCommandHandler implements
    pb.merchant_document.VertxMerchantDocumentCommandServiceGrpcServer.MerchantDocumentCommandServiceApi {

  private final MerchantDocumentCommandService service;

  public MerchantDocumentCommandHandler(MerchantDocumentCommandService service) {
    this.service = service;
  }

  @Override
  public Future<ApiResponseMerchantDocument> create(CreateMerchantDocumentRequest req) {
    return service.createMerchantDocument(req)
        .map(resp -> {
          var builder = ApiResponseMerchantDocument.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromDocumentResponse(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseMerchantDocument> update(UpdateMerchantDocumentRequest req) {
    return service.updateMerchantDocument(req)
        .map(resp -> {
          var builder = ApiResponseMerchantDocument.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromDocumentResponse(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseMerchantDocument> updateStatus(UpdateMerchantDocumentStatusRequest req) {
    return service.updateMerchantDocumentStatus(req)
        .map(resp -> {
          var builder = ApiResponseMerchantDocument.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromDocumentResponse(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseMerchantDocumentDeleteAt> trashed(FindMerchantDocumentByIdRequest req) {
    return service.trashedMerchantDocument((int) req.getDocumentId())
        .map(resp -> {
          var builder = ApiResponseMerchantDocumentDeleteAt.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromDocumentResponseAt(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseMerchantDocumentDeleteAt> restore(FindMerchantDocumentByIdRequest req) {
    return service.restoreMerchantDocument((int) req.getDocumentId())
        .map(resp -> {
          var builder = ApiResponseMerchantDocumentDeleteAt.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromDocumentResponseAt(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseMerchantDocumentDelete> deletePermanent(FindMerchantDocumentByIdRequest req) {
    return service.deleteMerchantDocumentPermanent((int) req.getDocumentId())
        .map(resp -> ApiResponseMerchantDocumentDelete.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .build());
  }

  @Override
  public Future<ApiResponseMerchantDocumentAll> restoreAll(com.google.protobuf.Empty req) {
    return service.restoreAllMerchantDocument()
        .map(resp -> ApiResponseMerchantDocumentAll.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .build());
  }

  @Override
  public Future<ApiResponseMerchantDocumentAll> deleteAllPermanent(com.google.protobuf.Empty req) {
    return service.deleteAllMerchantDocumentPermanent()
        .map(resp -> ApiResponseMerchantDocumentAll.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .build());
  }
}
