package io.example.auth.repository;

import io.example.auth.repository.impl.*;
import io.vertx.sqlclient.Pool;
import lombok.Getter;

@Getter
public class Repositories {
    private final UserRepository user;
    private final RefreshTokenRepository refreshToken;
    private final UserRoleRepository userRole;
    private final RoleRepository role;
    private final ResetTokenRepository resetToken;

    public Repositories(Pool pool) {
        this.user = new UserRepositoryImpl(pool);
        this.userRole = new UserRoleRepositoryImpl(pool);
        this.refreshToken = new RefreshTokenRepositoryImpl(pool);
        this.role = new RoleRepositoryImpl(pool);
        this.resetToken = new ResetTokenRepositoryImpl(pool);
    }
}
