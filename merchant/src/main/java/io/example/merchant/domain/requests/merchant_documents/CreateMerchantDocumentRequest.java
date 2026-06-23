package io.example.merchant.domain.requests.merchant_documents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateMerchantDocumentRequest {
  private int merchantId;
  private String documentType;
  private String documentUrl;
}
