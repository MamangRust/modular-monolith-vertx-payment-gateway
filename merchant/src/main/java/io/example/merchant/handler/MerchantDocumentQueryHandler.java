package io.example.merchant.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.merchant.service.MerchantDocumentQueryService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocument;
import pb.merchant_document.MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest;
import pb.merchant_document.MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest;
import pb.merchant_document.MerchantDocumentQuery.ApiResponsePaginationMerchantDocument;
import pb.merchant_document.MerchantDocumentQuery.ApiResponsePaginationMerchantDocumentAt;

@RequiredArgsConstructor
public class MerchantDocumentQueryHandler implements
    pb.merchant_document.VertxMerchantDocumentQueryServiceGrpcServer.MerchantDocumentQueryServiceApi {

  private final MerchantDocumentQueryService service;

  private pb.common.PaginationMeta buildPaginationMeta(int page, int pageSize, int totalRecords) {
    int safePage = page > 0 ? page : 1;
    int safePageSize = pageSize > 0 ? pageSize : 10;
    int totalPages = (int) Math.ceil((double) totalRecords / safePageSize);
    return pb.common.PaginationMeta.newBuilder()
        .setCurrentPage(safePage)
        .setPageSize(safePageSize)
        .setTotalPages(totalPages)
        .setTotalRecords(totalRecords)
        .build();
  }

  @Override
  public Future<ApiResponsePaginationMerchantDocument> findAll(FindAllMerchantDocumentsRequest req) {
    return service.findAll(req)
        .map(resp -> {
          var builder = ApiResponsePaginationMerchantDocument.newBuilder()
              .setStatus("success")
              .setMessage("OK")
              .setPaginationMeta(buildPaginationMeta(req.getPage(), req.getPageSize(), resp.getTotalRecords()));
          resp.getData().stream().map(ProtoConverter::fromDocumentResponse).forEach(builder::addData);
          return builder.build();
        })
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponsePaginationMerchantDocumentAt> findAllActive(FindAllMerchantDocumentsRequest req) {
    return service.findByActive(req)
        .map(resp -> {
          var builder = ApiResponsePaginationMerchantDocumentAt.newBuilder()
              .setStatus("success")
              .setMessage("OK")
              .setPaginationMeta(buildPaginationMeta(req.getPage(), req.getPageSize(), resp.getTotalRecords()));
          resp.getData().stream().map(ProtoConverter::fromDocumentResponseAt).forEach(builder::addData);
          return builder.build();
        })
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponsePaginationMerchantDocumentAt> findAllTrashed(FindAllMerchantDocumentsRequest req) {
    return service.findByTrashed(req)
        .map(resp -> {
          var builder = ApiResponsePaginationMerchantDocumentAt.newBuilder()
              .setStatus("success")
              .setMessage("OK")
              .setPaginationMeta(buildPaginationMeta(req.getPage(), req.getPageSize(), resp.getTotalRecords()));
          resp.getData().stream().map(ProtoConverter::fromDocumentResponseAt).forEach(builder::addData);
          return builder.build();
        })
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseMerchantDocument> findById(FindMerchantDocumentByIdRequest req) {
    return service.findById((int) req.getDocumentId())
        .map(data -> ApiResponseMerchantDocument.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromDocumentResponse(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}