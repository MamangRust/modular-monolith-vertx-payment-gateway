package io.example.merchant.handler;

import io.example.merchant.service.MerchantTransactionService;
import io.vertx.core.Future;
import pb.merchant.Merchant.FindAllMerchantTransaction;
import pb.merchant.Merchant.FindAllMerchantTransactionApikey;
import pb.merchant.Merchant.FindAllMerchantTransactionId;
import pb.merchant.MerchantTransaction.ApiResponsePaginationMerchantTransaction;

public class MerchantStatsTransactionHandler
    implements pb.merchant.VertxMerchantTransactionServiceGrpcServer.MerchantTransactionServiceApi {
  private final MerchantTransactionService service;

  public MerchantStatsTransactionHandler(MerchantTransactionService service) {
    this.service = service;
  }

  private pb.common.PaginationMeta toMeta(io.example.common.model.PaginationMeta meta) {
    if (meta == null) return pb.common.PaginationMeta.getDefaultInstance();
    return pb.common.PaginationMeta.newBuilder()
        .setCurrentPage(meta.currentPage())
        .setPageSize(meta.pageSize())
        .setTotalPages(meta.totalPages())
        .setTotalRecords(meta.totalRecords())
        .build();
  }

  @Override
  public Future<ApiResponsePaginationMerchantTransaction> findAllTransactionMerchant(FindAllMerchantTransaction req) {
    return service.getTransactions(req)
        .map(resp -> ApiResponsePaginationMerchantTransaction.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .addAllData(resp.data().stream().map(ProtoConverter::toTxnResponse).toList())
            .setPaginationMeta(toMeta(resp.pagination()))
            .build());
  }

  @Override
  public Future<ApiResponsePaginationMerchantTransaction> findAllTransactionByApikey(FindAllMerchantTransactionApikey req) {
    return service.getTransactionsByApiKey(req)
        .map(resp -> ApiResponsePaginationMerchantTransaction.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .addAllData(resp.data().stream().map(ProtoConverter::toTxnResponse).toList())
            .setPaginationMeta(toMeta(resp.pagination()))
            .build());
  }

  @Override
  public Future<ApiResponsePaginationMerchantTransaction> findAllTransactionByMerchant(FindAllMerchantTransactionId req) {
    return service.getTransactionsByMerchantId(req)
        .map(resp -> ApiResponsePaginationMerchantTransaction.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .addAllData(resp.data().stream().map(ProtoConverter::toTxnResponse).toList())
            .setPaginationMeta(toMeta(resp.pagination()))
            .build());
  }
}
