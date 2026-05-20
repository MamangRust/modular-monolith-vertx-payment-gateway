package io.example.saldo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SaldoResponseDeleteAt {
  private Integer id;
  private String cardNumber;
  private Long totalBalance;
  private String createdAt;
  private String updatedAt;
  private String deletedAt;

  public static SaldoResponseDeleteAt from(Saldo s) {
    if (s == null) return null;
    return SaldoResponseDeleteAt.builder()
        .id(s.getId())
        .cardNumber(s.getCardNumber())
        .totalBalance(s.getTotalBalance())
        .createdAt(s.getCreatedAt() != null ? s.getCreatedAt().toInstant().toString() : "")
        .updatedAt(s.getUpdatedAt() != null ? s.getUpdatedAt().toInstant().toString() : "")
        .deletedAt(s.getDeletedAt() != null ? s.getDeletedAt().toInstant().toString() : null)
        .build();
  }
}
