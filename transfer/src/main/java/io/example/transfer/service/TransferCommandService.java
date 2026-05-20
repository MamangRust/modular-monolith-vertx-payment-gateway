package io.example.transfer.service;

import io.example.common.model.ApiResponse;
import io.example.transfer.model.TransferResponse;
import io.example.transfer.model.TransferResponseDeleteAt;
import io.vertx.core.Future;
import pb.transfer.TransferCommand.CreateTransferRequest;
import pb.transfer.TransferCommand.UpdateTransferRequest;

public interface TransferCommandService {
  Future<ApiResponse<TransferResponse>> createTransfer(CreateTransferRequest req);

  Future<ApiResponse<TransferResponse>> updateTransfer(UpdateTransferRequest req);

  Future<ApiResponse<TransferResponseDeleteAt>> trashTransfer(int transferId);

  Future<ApiResponse<TransferResponseDeleteAt>> restoreTransfer(int transferId);

  Future<ApiResponse<Boolean>> deleteTransferPermanently(int transferId);

  Future<ApiResponse<Boolean>> restoreAllTransfers();

  Future<ApiResponse<Boolean>> deleteAllPermanentTransfers();
}
