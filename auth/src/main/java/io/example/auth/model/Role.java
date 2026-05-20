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
public class Role {
    private Integer roleId;
    private String roleName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static Role fromRow(Row row) {
        if (row == null) return null;
        return Role.builder()
            .roleId(row.getInteger("role_id"))
            .roleName(row.getString("role_name"))
            .createdAt(row.getLocalDateTime("created_at"))
            .updatedAt(row.getLocalDateTime("updated_at"))
            .deletedAt(row.getLocalDateTime("deleted_at"))
            .build();
    }
}
