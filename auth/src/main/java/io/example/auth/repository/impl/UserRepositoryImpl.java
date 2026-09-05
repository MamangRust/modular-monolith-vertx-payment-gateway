package io.example.auth.repository.impl;

import io.example.auth.domain.requests.CreateUserRequest;
import io.example.auth.model.AuthUser;
import io.example.auth.repository.UserRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final Pool pool;

    @Override
    public Future<AuthUser> findByEmail(String email) {
        return pool.preparedQuery("SELECT * FROM users WHERE email = $1 AND deleted_at IS NULL")
                .execute(Tuple.of(email))
                .map(rows -> rows.iterator().hasNext() ? AuthUser.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<AuthUser> findByEmailAndVerify(String email) {
        return pool.preparedQuery("""
                SELECT u.*, r.role_name
                FROM users u
                JOIN user_roles ur ON ur.user_id = u.user_id AND ur.deleted_at IS NULL
                JOIN roles r ON r.role_id = ur.role_id AND r.deleted_at IS NULL
                WHERE u.email = $1 AND u.is_verified = true AND u.deleted_at IS NULL
                """)
                .execute(Tuple.of(email))
                .map(AuthUser::fromRowsWithRoles);
    }

    @Override
    public Future<AuthUser> findById(Integer userId) {
        return pool.preparedQuery("SELECT * FROM users WHERE user_id = $1 AND deleted_at IS NULL")
                .execute(Tuple.of(userId))
                .map(rows -> rows.iterator().hasNext() ? AuthUser.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<AuthUser> createUser(CreateUserRequest request) {
        return pool
                .preparedQuery(
                        """
                                INSERT INTO users (firstname, lastname, email, password, verification_code, is_verified, created_at, updated_at)
                                VALUES ($1, $2, $3, $4, $5, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                                RETURNING *
                                """)
                .execute(Tuple.of(
                        request.getFirstName(),
                        request.getLastName(),
                        request.getEmail(),
                        request.getPassword(),
                        request.getVerificationCode()))
                .map(rows -> AuthUser.fromRow(rows.iterator().next()));
    }

    @Override
    public Future<AuthUser> updateUserIsVerified(Integer userId, boolean isVerified) {
        return pool.preparedQuery("""
                UPDATE users SET is_verified = $1, updated_at = CURRENT_TIMESTAMP
                WHERE user_id = $2 AND deleted_at IS NULL
                RETURNING *
                """)
                .execute(Tuple.of(isVerified, userId))
                .map(rows -> rows.iterator().hasNext() ? AuthUser.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<AuthUser> updateUserPassword(Integer userId, String password) {
        return pool.preparedQuery("""
                UPDATE users SET password = $1, updated_at = CURRENT_TIMESTAMP
                WHERE user_id = $2 AND deleted_at IS NULL
                RETURNING *
                """)
                .execute(Tuple.of(password, userId))
                .map(rows -> rows.iterator().hasNext() ? AuthUser.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<AuthUser> findByVerificationCode(String verificationCode) {
        return pool.preparedQuery("SELECT * FROM users WHERE verification_code = $1 AND deleted_at IS NULL")
                .execute(Tuple.of(verificationCode))
                .map(rows -> rows.iterator().hasNext() ? AuthUser.fromRow(rows.iterator().next()) : null);
    }
}
