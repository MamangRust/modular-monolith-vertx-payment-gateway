package io.example.merchant.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MerchantDocumentResponseDeleteAt {
  private Integer id;
  private Integer merchantId;
  private String documentType;
  private String documentUrl;
  private String status;
  private String note;
  private String createdAt;
  private String updatedAt;
  private String deletedAt;

  public static MerchantDocumentResponseDeleteAt from(MerchantDocument doc) {
    if (doc == null) return null;
    return MerchantDocumentResponseDeleteAt.builder()
        .id(doc.getId())
        .merchantId(doc.getMerchantId())
        .documentType(doc.getDocumentType())
        .documentUrl(doc.getDocumentUrl())
        .status(doc.getStatus())
        .note(doc.getNote())
        .createdAt(doc.getCreatedAt() != null ? doc.getCreatedAt().toString() : null)
        .updatedAt(doc.getUpdatedAt() != null ? doc.getUpdatedAt().toString() : null)
        .deletedAt(doc.getDeletedAt() != null ? doc.getDeletedAt().toString() : null)
        .build();
  }
}
