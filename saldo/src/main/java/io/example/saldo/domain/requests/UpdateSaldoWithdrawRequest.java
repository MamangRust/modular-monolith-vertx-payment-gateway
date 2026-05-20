package io.example.saldo.domain.requests;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSaldoWithdrawRequest {
  private String cardNumber;
  private Long withdrawAmount;
  private LocalDateTime withdrawTime;
}
