package io.example.transaction.domain.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MonthStatusTransactionCardNumber {
    private String cardNumber;
    private int year;
    private int month;
    private String status;
}
