package io.example.merchant.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MerchantResponse {
  private Integer id;
  private String name;
  private String apiKey;
  private String status;
  private Integer userId;
  private String createdAt;
  private String updatedAt;

  public static MerchantResponse from(Merchant m) {
    if (m == null) return null;
    return MerchantResponse.builder()
        .id(m.getId())
        .name(m.getName())
        .apiKey(m.getApiKey())
        .status(m.getStatus())
        .userId(m.getUserId())
        .createdAt(m.getCreatedAt() != null ? m.getCreatedAt().toInstant().toString() : "")
        .updatedAt(m.getUpdatedAt() != null ? m.getUpdatedAt().toInstant().toString() : "")
        .build();
  }
}

