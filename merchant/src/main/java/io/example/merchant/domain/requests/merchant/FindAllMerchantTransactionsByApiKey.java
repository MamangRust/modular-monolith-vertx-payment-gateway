package io.example.merchant.domain.requests.merchant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FindAllMerchantTransactionsByApiKey {
  private String apiKey;
  @Builder.Default
  private Integer page = 1;
  @Builder.Default
  private Integer pageSize = 10;
  @Builder.Default
  private String search = "";
}
