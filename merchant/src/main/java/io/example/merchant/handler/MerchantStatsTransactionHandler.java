package io.example.merchant.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.merchant.service.MerchantTransactionService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.merchant.Merchant.FindAllMerchantTransaction;
import pb.merchant.Merchant.FindAllMerchantTransactionApikey;
import pb.merchant.Merchant.FindAllMerchantTransactionId;
import pb.merchant.MerchantTransaction.ApiResponsePaginationMerchantTransaction;

@RequiredArgsConstructor
public class MerchantStatsTransactionHandler
    implements pb.merchant.VertxMerchantTransactionServiceGrpcServer.MerchantTransactionServiceApi {
  private final MerchantTransactionService service;

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
  public Future<ApiResponsePaginationMerchantTransaction> findAllTransactionMerchant(FindAllMerchantTransaction req) {
    return service.getTransactions(req)
        .map(resp -> {
          var builder = ApiResponsePaginationMerchantTransaction.newBuilder()
              .setStatus("success")
              .setMessage("OK")
              .setPaginationMeta(buildPaginationMeta(req.getPage(), req.getPageSize(), resp.getTotalRecords()));
          resp.getData().stream().map(ProtoConverter::toTxnResponse).forEach(builder::addData);
          return builder.build();
        })
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponsePaginationMerchantTransaction> findAllTransactionByApikey(
      FindAllMerchantTransactionApikey req) {
    return service.getTransactionsByApiKey(req)
        .map(resp -> {
          var builder = ApiResponsePaginationMerchantTransaction.newBuilder()
              .setStatus("success")
              .setMessage("OK")
              .setPaginationMeta(buildPaginationMeta(req.getPage(), req.getPageSize(), resp.getTotalRecords()));
          resp.getData().stream().map(ProtoConverter::toTxnResponse).forEach(builder::addData);
          return builder.build();
        })
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponsePaginationMerchantTransaction> findAllTransactionByMerchant(
      FindAllMerchantTransactionId req) {
    return service.getTransactionsByMerchantId(req)
        .map(resp -> {
          var builder = ApiResponsePaginationMerchantTransaction.newBuilder()
              .setStatus("success")
              .setMessage("OK")
              .setPaginationMeta(buildPaginationMeta(req.getPage(), req.getPageSize(), resp.getTotalRecords()));
          resp.getData().stream().map(ProtoConverter::toTxnResponse).forEach(builder::addData);
          return builder.build();
        })
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}