package io.example.topup.domain.requests.topup;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MonthTopupStatusCardNumberRequest {
    private String cardNumber;
    private int year;
    private int month;
    private String status;
}
