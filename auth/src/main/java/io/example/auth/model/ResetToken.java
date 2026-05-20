package io.example.auth.model;

import java.time.LocalDateTime;
import io.vertx.sqlclient.Row;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResetToken {
    private Integer userId;
    private String token;
    private LocalDateTime expiryDate;

    public static ResetToken fromRow(Row row) {
        if (row == null) return null;
        return ResetToken.builder()
            .userId(row.getInteger("user_id"))
            .token(row.getString("token"))
            .expiryDate(row.getLocalDateTime("expiry_date"))
            .build();
    }
}
