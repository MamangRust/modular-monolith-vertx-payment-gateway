package io.example.topup.domain.requests.topup;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateTopupRequest {
  private int topupId;
  private String cardNumber;
  private int topupAmount;
  private String topupMethod;
}
