package io.example.card.domain.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateCardRequest {
    private Integer userId;
    private String cardType;
    private String expireDate;
    private String cvv;
    private String cardProvider;
}
