package io.example.withdraw.domain.requests;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateWithdrawRequest {
    private String cardNumber;
    private int withdrawAmount;
    private OffsetDateTime withdrawTime;
    private String idempotencyKey;
}
