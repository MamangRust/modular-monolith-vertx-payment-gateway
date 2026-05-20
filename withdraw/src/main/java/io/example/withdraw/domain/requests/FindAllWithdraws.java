package io.example.withdraw.domain.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindAllWithdraws {
  @Builder.Default
  private Integer page = 1;
  @Builder.Default
  private Integer pageSize = 10;
  @Builder.Default
  private String search = "";
}
