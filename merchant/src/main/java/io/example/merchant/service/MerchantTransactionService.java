package io.example.merchant.service;

import io.example.common.domain.PagedResult;
import io.example.merchant.model.MerchantTransactions;
import io.vertx.core.Future;
import pb.merchant.Merchant.FindAllMerchantTransaction;
import pb.merchant.Merchant.FindAllMerchantTransactionApikey;
import pb.merchant.Merchant.FindAllMerchantTransactionId;

public interface MerchantTransactionService {
        Future<PagedResult<MerchantTransactions>> getTransactions(FindAllMerchantTransaction req);

        Future<PagedResult<MerchantTransactions>> getTransactionsByApiKey(FindAllMerchantTransactionApikey req);

        Future<PagedResult<MerchantTransactions>> getTransactionsByMerchantId(FindAllMerchantTransactionId req);
}