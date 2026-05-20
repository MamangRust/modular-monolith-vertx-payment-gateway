package io.example.saldo.domain.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSaldoRequest {
  private Integer saldoId;
  private String cardNumber;
  private Long totalBalance;
}
