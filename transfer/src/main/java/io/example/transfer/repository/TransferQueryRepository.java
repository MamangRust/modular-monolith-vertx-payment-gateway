package io.example.transfer.repository;

import io.example.common.model.PagedResult;
import io.example.transfer.domain.requests.FindAllTransfers;
import io.example.transfer.model.Transfer;
import io.vertx.core.Future;
import java.util.List;

public interface TransferQueryRepository {
  Future<PagedResult<Transfer>> getTransfers(FindAllTransfers req);

  Future<PagedResult<Transfer>> getActiveTransfers(FindAllTransfers req);

  Future<PagedResult<Transfer>> getTrashedTransfers(FindAllTransfers req);

  Future<Transfer> getTransferById(int id);

  Future<List<Transfer>> getTransfersByCardNumber(String card);

  Future<List<Transfer>> getTransfersBySender(String card);

  Future<List<Transfer>> getTransfersByReceiver(String card);
}
