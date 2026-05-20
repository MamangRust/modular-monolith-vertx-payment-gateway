package io.example.withdraw.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WithdrawResponse {
  private Integer id;
  private String cardNumber;
  private Integer withdrawAmount;
  private String status;
  private String withdrawTime;
  private String createdAt;
  private String updatedAt;

  public static WithdrawResponse from(Withdraw w) {
    if (w == null)
      return null;
    return WithdrawResponse.builder()
        .id(w.getId())
        .cardNumber(w.getCardNumber())
        .withdrawAmount(w.getWithdrawAmount().intValue())
        .status(w.getStatus())
        .withdrawTime(w.getWithdrawTime() != null ? w.getWithdrawTime().toInstant().toString() : "")
        .createdAt(w.getCreatedAt() != null ? w.getCreatedAt().toInstant().toString() : "")
        .updatedAt(w.getUpdatedAt() != null ? w.getUpdatedAt().toInstant().toString() : "")
        .build();
  }
}
