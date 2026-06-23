package io.example.merchant.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.merchant.service.MerchantQueryService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.merchant.Merchant.ApiResponseMerchant;
import pb.merchant.Merchant.ApiResponsesMerchant;
import pb.merchant.Merchant.FindAllMerchantRequest;
import pb.merchant.Merchant.FindByApiKeyRequest;
import pb.merchant.Merchant.FindByIdMerchantRequest;
import pb.merchant.Merchant.FindByMerchantUserIdRequest;
import pb.merchant.MerchantQuery.ApiResponsePaginationMerchant;
import pb.merchant.MerchantQuery.ApiResponsePaginationMerchantDeleteAt;

@RequiredArgsConstructor
public class MerchantQueryHandler implements pb.merchant.VertxMerchantQueryServiceGrpcServer.MerchantQueryServiceApi {

  private final MerchantQueryService service;

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
  public Future<ApiResponsePaginationMerchant> findAllMerchant(FindAllMerchantRequest req) {
    return service.findAll(req)
        .map(resp -> {
          var builder = ApiResponsePaginationMerchant.newBuilder()
              .setStatus("success")
              .setMessage("OK")
              .setPaginationMeta(buildPaginationMeta(req.getPage(), req.getPageSize(), resp.getTotalRecords()));
          resp.getData().stream().map(ProtoConverter::fromMerchantResponse).forEach(builder::addData);
          return builder.build();
        })
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseMerchant> findByIdMerchant(FindByIdMerchantRequest req) {
    return service.findById(req.getMerchantId())
        .map(data -> ApiResponseMerchant.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromMerchantResponse(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseMerchant> findByApiKey(FindByApiKeyRequest req) {
    return service.findByApiKey(req.getApiKey())
        .map(data -> ApiResponseMerchant.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromMerchantResponse(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponsesMerchant> findByMerchantUserId(FindByMerchantUserIdRequest req) {
    return service.findByMerchantUserId(req.getUserId())
        .map(data -> {
          var builder = ApiResponsesMerchant.newBuilder()
              .setStatus("success")
              .setMessage("OK");
          data.stream().map(ProtoConverter::fromMerchantResponse).forEach(builder::addData);
          return builder.build();
        })
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponsePaginationMerchantDeleteAt> findByActive(FindAllMerchantRequest req) {
    return service.findByActive(req)
        .map(resp -> {
          var builder = ApiResponsePaginationMerchantDeleteAt.newBuilder()
              .setStatus("success")
              .setMessage("OK")
              .setPaginationMeta(buildPaginationMeta(req.getPage(), req.getPageSize(), resp.getTotalRecords()));
          resp.getData().stream().map(ProtoConverter::fromMerchantResponseDeleteAt).forEach(builder::addData);
          return builder.build();
        })
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponsePaginationMerchantDeleteAt> findByTrashed(FindAllMerchantRequest req) {
    return service.findByTrashed(req)
        .map(resp -> {
          var builder = ApiResponsePaginationMerchantDeleteAt.newBuilder()
              .setStatus("success")
              .setMessage("OK")
              .setPaginationMeta(buildPaginationMeta(req.getPage(), req.getPageSize(), resp.getTotalRecords()));
          resp.getData().stream().map(ProtoConverter::fromMerchantResponseDeleteAt).forEach(builder::addData);
          return builder.build();
        })
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}