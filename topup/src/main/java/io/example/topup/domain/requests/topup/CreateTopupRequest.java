package io.example.topup.domain.requests.topup;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateTopupRequest {
  private String cardNumber;
  private String topupNo;
  private int topupAmount;
  private String topupMethod;
}
