package io.example.transfer.service;

import io.example.common.domain.PagedResult;
import io.example.transfer.domain.requests.FindAllTransfers;
import io.example.transfer.model.TransferResponse;
import io.example.transfer.model.TransferResponseDeleteAt;
import io.vertx.core.Future;
import java.util.List;

public interface TransferQueryService {
  Future<PagedResult<TransferResponse>> getAllTransfers(FindAllTransfers req);

  Future<PagedResult<TransferResponseDeleteAt>> getActiveTransfers(FindAllTransfers req);

  Future<PagedResult<TransferResponseDeleteAt>> getTrashedTransfers(FindAllTransfers req);

  Future<TransferResponse> getTransferById(Integer transferId);

  Future<List<TransferResponse>> getTransfersByCardNumber(String cardNumber);

  Future<List<TransferResponse>> getTransfersAsSender(String cardNumber);

  Future<List<TransferResponse>> getTransfersAsReceiver(String cardNumber);
}