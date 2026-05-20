package io.example.merchant.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MerchantResponseDeleteAt {
  private Integer id;
  private String name;
  private String apiKey;
  private String status;
  private Integer userId;
  private String createdAt;
  private String updatedAt;
  private String deletedAt;

  public static MerchantResponseDeleteAt from(Merchant m) {
    if (m == null) return null;
    return MerchantResponseDeleteAt.builder()
        .id(m.getId())
        .name(m.getName())
        .apiKey(m.getApiKey())
        .status(m.getStatus())
        .userId(m.getUserId())
        .createdAt(m.getCreatedAt() != null ? m.getCreatedAt().toInstant().toString() : "")
        .updatedAt(m.getUpdatedAt() != null ? m.getUpdatedAt().toInstant().toString() : "")
        .deletedAt(m.getDeletedAt() != null ? m.getDeletedAt().toInstant().toString() : null)
        .build();
  }
}

