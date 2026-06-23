package io.example.transfer.service;

import io.example.transfer.model.TransferResponse;
import io.example.transfer.model.TransferResponseDeleteAt;
import io.vertx.core.Future;
import pb.transfer.TransferCommand.CreateTransferRequest;
import pb.transfer.TransferCommand.UpdateTransferRequest;

public interface TransferCommandService {
  Future<TransferResponse> createTransfer(CreateTransferRequest req);

  Future<TransferResponse> updateTransfer(UpdateTransferRequest req);

  Future<TransferResponseDeleteAt> trashTransfer(Integer transferId);

  Future<TransferResponseDeleteAt> restoreTransfer(Integer transferId);

  Future<Void> deleteTransferPermanently(Integer transferId);

  Future<Void> restoreAllTransfers();

  Future<Void> deleteAllPermanentTransfers();
}