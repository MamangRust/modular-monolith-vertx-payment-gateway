package io.example.merchant.domain.requests.merchant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MonthYearAmountMerchant {
  private int merchantId;
  private int year;
}
