package io.example.merchant.domain.requests.merchant_documents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateMerchantDocumentRequest {
  private Integer documentId;
  private int merchantId;
  private String documentType;
  private String documentUrl;
  private String status;
  private String note;
}
