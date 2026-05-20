package io.example.auth.model;

import io.vertx.sqlclient.Row;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRole {
    private Integer userId;
    private Integer roleId;

    public static UserRole fromRow(Row row) {
        if (row == null) return null;
        return UserRole.builder()
            .userId(row.getInteger("user_id"))
            .roleId(row.getInteger("role_id"))
            .build();
    }
}
