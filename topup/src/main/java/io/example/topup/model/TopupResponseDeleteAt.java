package io.example.topup.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TopupResponseDeleteAt {
  private Integer id;
  private String cardNumber;
  private Integer amount;
  private String status;
  private String createdAt;
  private String updatedAt;
  private String deletedAt;

  public static TopupResponseDeleteAt from(Topup t) {
    if (t == null)
      return null;
    return TopupResponseDeleteAt.builder()
        .id(t.getId())
        .cardNumber(t.getCardNumber())
        .amount(t.getTopupAmount().intValue())
        .createdAt(t.getCreatedAt() != null ? t.getCreatedAt().toInstant().toString() : "")
        .updatedAt(t.getUpdatedAt() != null ? t.getUpdatedAt().toInstant().toString() : "")
        .deletedAt(t.getDeletedAt() != null ? t.getDeletedAt().toInstant().toString() : null)
        .build();
  }
}
