package io.example.merchant.handler;

import io.example.merchant.service.MerchantDocumentQueryService;
import io.vertx.core.Future;
import pb.merchant_document.MerchantDocumentOuterClass.*;
import pb.merchant_document.MerchantDocumentQuery.*;

public class MerchantDocumentQueryHandler implements
    pb.merchant_document.VertxMerchantDocumentQueryServiceGrpcServer.MerchantDocumentQueryServiceApi {

  private final MerchantDocumentQueryService service;

  public MerchantDocumentQueryHandler(MerchantDocumentQueryService service) {
    this.service = service;
  }

  private pb.common.PaginationMeta toMeta(int page, int limit, int total) {
    int totalPages = (limit > 0) ? (int) Math.ceil((double) total / limit) : 0;
    return pb.common.PaginationMeta.newBuilder()
        .setCurrentPage(page)
        .setPageSize(limit)
        .setTotalPages(totalPages)
        .setTotalRecords(total)
        .build();
  }

  @Override
  public Future<ApiResponsePaginationMerchantDocument> findAll(FindAllMerchantDocumentsRequest req) {
    return service.findAll(req)
        .map(res -> ApiResponsePaginationMerchantDocument.newBuilder()
            .setStatus(res.status())
            .setMessage(res.message())
            .addAllData(res.data().stream().map(ProtoConverter::fromDocumentResponse).toList())
            .setPaginationMeta(
                toMeta(res.pagination().currentPage(), res.pagination().pageSize(), res.pagination().totalRecords()))
            .build());
  }

  @Override
  public Future<ApiResponsePaginationMerchantDocumentAt> findAllActive(FindAllMerchantDocumentsRequest req) {
    return service.findByActive(req)
        .map(res -> ApiResponsePaginationMerchantDocumentAt.newBuilder()
            .setStatus(res.status())
            .setMessage(res.message())
            .addAllData(res.data().stream().map(ProtoConverter::fromDocumentResponseAt).toList())
            .setPaginationMeta(
                toMeta(res.pagination().currentPage(), res.pagination().pageSize(), res.pagination().totalRecords()))
            .build());
  }

  @Override
  public Future<ApiResponsePaginationMerchantDocumentAt> findAllTrashed(FindAllMerchantDocumentsRequest req) {
    return service.findByTrashed(req)
        .map(res -> ApiResponsePaginationMerchantDocumentAt.newBuilder()
            .setStatus(res.status())
            .setMessage(res.message())
            .addAllData(res.data().stream().map(ProtoConverter::fromDocumentResponseAt).toList())
            .setPaginationMeta(
                toMeta(res.pagination().currentPage(), res.pagination().pageSize(), res.pagination().totalRecords()))
            .build());
  }

  @Override
  public Future<ApiResponseMerchantDocument> findById(FindMerchantDocumentByIdRequest req) {
    return service.findById((int) req.getDocumentId())
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
}
