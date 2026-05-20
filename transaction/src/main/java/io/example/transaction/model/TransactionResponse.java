package io.example.transaction.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResponse {
  private Integer id;
  private String cardNumber;
  private Integer amount;
  private String paymentMethod;
  private Integer merchantId;
  private String status;
  private String transactionTime;
  private String createdAt;
  private String updatedAt;

  public static TransactionResponse from(Transaction t) {
    if (t == null) return null;
    return TransactionResponse.builder()
        .id(t.getId())
        .cardNumber(t.getCardNumber())
        .amount(t.getAmount().intValue())
        .paymentMethod(t.getPaymentMethod())
        .merchantId(t.getMerchantId())
        .status(t.getStatus())
        .transactionTime(t.getTransactionTime() != null ? t.getTransactionTime().toInstant().toString() : "")
        .createdAt(t.getCreatedAt() != null ? t.getCreatedAt().toInstant().toString() : "")
        .updatedAt(t.getUpdatedAt() != null ? t.getUpdatedAt().toInstant().toString() : "")
        .build();
  } 
}
