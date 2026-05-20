package io.example.merchant.handler;

import io.example.merchant.service.MerchantQueryService;
import io.vertx.core.Future;
import pb.merchant.Merchant.*;
import pb.merchant.MerchantQuery.*;

public class MerchantQueryHandler implements
    pb.merchant.VertxMerchantQueryServiceGrpcServer.MerchantQueryServiceApi {

  private final MerchantQueryService service;

  public MerchantQueryHandler(MerchantQueryService service) {
    this.service = service;
  }

  private pb.common.PaginationMeta toMeta(io.example.common.model.PaginationMeta meta) {
    if (meta == null)
      return pb.common.PaginationMeta.getDefaultInstance();
    return pb.common.PaginationMeta.newBuilder()
        .setCurrentPage(meta.currentPage())
        .setPageSize(meta.pageSize())
        .setTotalPages(meta.totalPages())
        .setTotalRecords(meta.totalRecords())
        .build();
  }

  @Override
  public Future<ApiResponsePaginationMerchant> findAllMerchant(FindAllMerchantRequest req) {
    return service.findAll(req)
        .map(resp -> ApiResponsePaginationMerchant.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .addAllData(resp.data().stream().map(ProtoConverter::fromMerchantResponse).toList())
            .setPaginationMeta(toMeta(resp.pagination()))
            .build());
  }

  @Override
  public Future<ApiResponseMerchant> findByIdMerchant(FindByIdMerchantRequest req) {
    return service.findById(req.getMerchantId())
        .map(resp -> {
          var builder = ApiResponseMerchant.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromMerchantResponse(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseMerchant> findByApiKey(FindByApiKeyRequest req) {
    return service.findByApiKey(req.getApiKey())
        .map(resp -> {
          var builder = ApiResponseMerchant.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromMerchantResponse(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponsesMerchant> findByMerchantUserId(FindByMerchantUserIdRequest req) {
    return service.findByMerchantUserId(req.getUserId())
        .map(resp -> {
          var builder = ApiResponsesMerchant.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.addAllData(resp.data().stream().map(ProtoConverter::fromMerchantResponse).toList());
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponsePaginationMerchantDeleteAt> findByActive(FindAllMerchantRequest req) {
    return service.findByActive(req)
        .map(resp -> ApiResponsePaginationMerchantDeleteAt.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .addAllData(resp.data().stream().map(ProtoConverter::fromMerchantResponseDeleteAt).toList())
            .setPaginationMeta(toMeta(resp.pagination()))
            .build());
  }

  @Override
  public Future<ApiResponsePaginationMerchantDeleteAt> findByTrashed(FindAllMerchantRequest req) {
    return service.findByTrashed(req)
        .map(resp -> ApiResponsePaginationMerchantDeleteAt.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .addAllData(resp.data().stream().map(ProtoConverter::fromMerchantResponseDeleteAt).toList())
            .setPaginationMeta(toMeta(resp.pagination()))
            .build());
  }
}
