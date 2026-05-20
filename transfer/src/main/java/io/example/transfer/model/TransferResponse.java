package io.example.transfer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransferResponse {
  private Integer id;
  private String transferNo;
  private String transferFrom;
  private String transferTo;
  private Long transferAmount;
  private String status;
  private String transferTime;
  private String createdAt;
  private String updatedAt;

  public static TransferResponse from(Transfer t) {
    if (t == null)
      return null;
    return TransferResponse.builder()
        .id(t.getId())
        .transferNo(t.getTransferNo())
        .transferFrom(t.getTransferFrom())
        .transferTo(t.getTransferTo())
        .transferAmount(t.getTransferAmount())
        .status(t.getStatus())
        .transferTime(t.getTransferTime() != null ? t.getTransferTime().toInstant().toString() : "")
        .createdAt(t.getCreatedAt() != null ? t.getCreatedAt().toInstant().toString() : "")
        .updatedAt(t.getUpdatedAt() != null ? t.getUpdatedAt().toInstant().toString() : "")
        .build();
  }
}
