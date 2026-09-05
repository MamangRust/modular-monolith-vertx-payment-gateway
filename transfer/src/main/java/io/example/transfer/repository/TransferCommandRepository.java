package io.example.transfer.repository;

import io.example.transfer.model.Transfer;
import io.vertx.core.Future;

public interface TransferCommandRepository {
  Future<Transfer> createTransfer(String from, String to, long amount, String idempotencyKey);

  Future<Transfer> findByIdempotencyKey(String idempotencyKey);

  Future<Transfer> updateTransfer(int id, String from, String to, long amount);

  Future<Transfer> updateTransferAmount(int id, long amount);

  Future<Transfer> updateTransferStatus(int id, String status);

  Future<Transfer> trashTransfer(int id);

  Future<Transfer> restoreTransfer(int id);

  Future<Boolean> deleteTransferPermanently(int id);

  Future<Integer> restoreAllTransfers();

  Future<Integer> deleteAllPermanentTransfers();
}
