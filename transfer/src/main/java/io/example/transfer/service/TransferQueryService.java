package io.example.transfer.service;

import io.example.common.model.ApiResponse;
import io.example.common.model.ApiResponsePagination;
import io.example.transfer.model.TransferResponse;
import io.example.transfer.model.TransferResponseDeleteAt;
import io.vertx.core.Future;
import java.util.List;
import pb.transfer.Transfer.FindAllTransferRequest;

public interface TransferQueryService {
  Future<ApiResponsePagination<List<TransferResponse>>> getAllTransfers(FindAllTransferRequest req);

  Future<ApiResponsePagination<List<TransferResponseDeleteAt>>> getActiveTransfers(FindAllTransferRequest req);

  Future<ApiResponsePagination<List<TransferResponseDeleteAt>>> getTrashedTransfers(FindAllTransferRequest req);

  Future<ApiResponse<TransferResponse>> getTransferById(Integer transferId);

  Future<ApiResponse<List<TransferResponse>>> getTransfersByCardNumber(String cardNumber);

  Future<ApiResponse<List<TransferResponse>>> getTransfersAsSender(String cardNumber);

  Future<ApiResponse<List<TransferResponse>>> getTransfersAsReceiver(String cardNumber);
}
