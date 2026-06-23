package io.example.merchant.domain.requests.merchant_documents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateMerchantDocumentStatusRequest {
  private Integer documentId;
  private int merchantId;
  private String status;
  private String note;
}
