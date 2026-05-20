package io.example.withdraw.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WithdrawResponseDeleteAt {
  private Integer id;
  private String cardNumber;
  private Integer withdrawAmount;
  private String status;
  private String withdrawTime;
  private String createdAt;
  private String updatedAt;
  private String deletedAt;

  public static WithdrawResponseDeleteAt from(Withdraw w) {
    if (w == null)
      return null;
    return WithdrawResponseDeleteAt.builder()
        .id(w.getId())
        .cardNumber(w.getCardNumber())
        .withdrawAmount(w.getWithdrawAmount().intValue())
        .status(w.getStatus())
        .withdrawTime(w.getWithdrawTime() != null ? w.getWithdrawTime().toInstant().toString() : "")
        .createdAt(w.getCreatedAt() != null ? w.getCreatedAt().toInstant().toString() : "")
        .updatedAt(w.getUpdatedAt() != null ? w.getUpdatedAt().toInstant().toString() : "")
        .deletedAt(w.getDeletedAt() != null ? w.getDeletedAt().toInstant().toString() : null)
        .build();
  }
}
