package io.example.merchant.repository;

import io.example.merchant.model.MerchantTransactions;
import io.example.common.domain.PagedResult;
import io.vertx.core.Future;
import pb.merchant.Merchant.FindAllMerchantTransaction;
import pb.merchant.Merchant.FindAllMerchantTransactionApikey;
import pb.merchant.Merchant.FindAllMerchantTransactionId;

public interface MerchantTransactionRepository {
  Future<PagedResult<MerchantTransactions>> findAllTransactionMerchant(FindAllMerchantTransaction request);

  Future<PagedResult<MerchantTransactions>> findAllTransactionByMerchant(FindAllMerchantTransactionId request);

  Future<PagedResult<MerchantTransactions>> findAllTransactionByApikey(FindAllMerchantTransactionApikey request);
}
