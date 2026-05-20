package io.example.topup.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TopupResponse {
  private Integer id;
  private String cardNumber;
  private Integer amount;
  private String status;
  private String createdAt;
  private String updatedAt;

  public static TopupResponse from(Topup t) {
    if (t == null)
      return null;
    return TopupResponse.builder()
        .id(t.getId())
        .cardNumber(t.getCardNumber())
        .amount(t.getTopupAmount().intValue())
        .createdAt(t.getCreatedAt() != null ? t.getCreatedAt().toInstant().toString() : "")
        .updatedAt(t.getUpdatedAt() != null ? t.getUpdatedAt().toInstant().toString() : "")
        .build();
  }
}
