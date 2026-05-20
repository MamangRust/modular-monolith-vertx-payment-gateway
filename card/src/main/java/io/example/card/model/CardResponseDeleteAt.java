package io.example.card.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CardResponseDeleteAt {
  private Integer id;
  private Integer userId;
  private String cardNumber;
  private String cardType;
  private String expireDate;
  private String cvv;
  private String cardProvider;
  private String createdAt;
  private String updatedAt;
  private String deletedAt;

  public static CardResponseDeleteAt from(Card card) {
    if (card == null) return null;
    return CardResponseDeleteAt.builder()
        .id(card.getId())
        .userId(card.getUserId())
        .cardNumber(card.getCardNumber())
        .cardType(card.getCardType())
        .expireDate(card.getExpireDate())
        .cvv(card.getCvv())
        .cardProvider(card.getCardProvider())
        .createdAt(card.getCreatedAt() != null ? card.getCreatedAt().toString() : null)
        .updatedAt(card.getUpdatedAt() != null ? card.getUpdatedAt().toString() : null)
        .deletedAt(card.getDeletedAt() != null ? card.getDeletedAt().toString() : null)
        .build();
  }
}
