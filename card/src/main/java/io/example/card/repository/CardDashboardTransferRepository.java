package io.example.card.repository;

import io.vertx.core.Future;

public interface CardDashboardTransferRepository {
  Future<Long> getTotalTransferAmount();
  Future<Long> getTotalTransferAmountBySender(String senderCardNumber);
  Future<Long> getTotalTransferAmountByReceiver(String receiverCardNumber);
}
