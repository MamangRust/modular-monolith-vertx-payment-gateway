package io.example.merchant.service;

import java.util.List;
import io.example.common.model.ApiResponsePagination;
import io.example.merchant.model.MerchantTransactions;
import io.vertx.core.Future;
import pb.merchant.Merchant.FindAllMerchantTransaction;
import pb.merchant.Merchant.FindAllMerchantTransactionId;
import pb.merchant.Merchant.FindAllMerchantTransactionApikey;

public interface MerchantTransactionService {
  Future<ApiResponsePagination<List<MerchantTransactions>>> getTransactions(FindAllMerchantTransaction req);

  Future<ApiResponsePagination<List<MerchantTransactions>>> getTransactionsByApiKey(
      FindAllMerchantTransactionApikey req);

  Future<ApiResponsePagination<List<MerchantTransactions>>> getTransactionsByMerchantId(
      FindAllMerchantTransactionId req);
}
