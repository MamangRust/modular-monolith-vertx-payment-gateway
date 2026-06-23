package io.example.transfer.domain.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateTransferRequest {
  private Integer transferId;
  private String transferFrom;
  private String transferTo;
  private int transferAmount;
}
