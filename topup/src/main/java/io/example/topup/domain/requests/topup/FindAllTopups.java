package io.example.topup.domain.requests.topup;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindAllTopups {
  @Builder.Default
  private int page = 1;

  @Builder.Default
  private int pageSize = 10;

  @Builder.Default
  private String search = "";
}
