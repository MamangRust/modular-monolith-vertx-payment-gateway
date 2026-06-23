package io.example.card.domain.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCardRequest {
    private Integer cardId;
    private Integer userId;
    private String cardType;
    private String expireDate;
    private String cvv;
    private String cardProvider;
}
