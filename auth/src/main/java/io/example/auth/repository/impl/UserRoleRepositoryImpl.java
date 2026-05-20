package io.example.auth.repository.impl;

import io.example.auth.model.UserRole;
import io.example.auth.repository.UserRoleRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;

public class UserRoleRepositoryImpl implements UserRoleRepository {
    private final Pool pool;

    public UserRoleRepositoryImpl(Pool pool) {
        this.pool = pool;
    }

    @Override
    public Future<UserRole> assignRoleToUser(Integer userId, Integer roleId) {
        return pool.preparedQuery("""
                INSERT INTO user_roles (user_id, role_id, created_at, updated_at)
                VALUES ($1, $2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (user_id, role_id) DO NOTHING
                RETURNING user_id, role_id
                """)
                .execute(Tuple.of(userId, roleId))
                .map(rows -> rows.iterator().hasNext() ? UserRole.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Void> removeRoleFromUser(Integer userId, Integer roleId) {
        return pool.preparedQuery("DELETE FROM user_roles WHERE user_id = $1 AND role_id = $2")
                .execute(Tuple.of(userId, roleId))
                .mapEmpty();
    }
}
