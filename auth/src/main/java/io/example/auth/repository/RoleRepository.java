package io.example.auth.repository;

import io.example.auth.model.Role;
import io.vertx.core.Future;

public interface RoleRepository {
    Future<Role> findById(Integer id);
    Future<Role> findByName(String name);
}
