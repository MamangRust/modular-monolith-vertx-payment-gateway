package io.example.auth.repository.impl;

import io.example.auth.model.Role;
import io.example.auth.repository.RoleRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;

public class RoleRepositoryImpl implements RoleRepository {
    private final Pool pool;

    public RoleRepositoryImpl(Pool pool) {
        this.pool = pool;
    }

    @Override
    public Future<Role> findById(Integer id) {
        return pool.preparedQuery("SELECT * FROM roles WHERE role_id = $1 AND deleted_at IS NULL")
                .execute(Tuple.of(id))
                .map(rows -> rows.iterator().hasNext() ? Role.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Role> findByName(String name) {
        return pool.preparedQuery("SELECT * FROM roles WHERE role_name = $1 AND deleted_at IS NULL")
                .execute(Tuple.of(name))
                .map(rows -> rows.iterator().hasNext() ? Role.fromRow(rows.iterator().next()) : null);
    }
}
