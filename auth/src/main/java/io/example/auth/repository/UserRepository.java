package io.example.auth.repository;

import io.example.auth.domain.requests.CreateUserRequest;
import io.example.auth.model.AuthUser;
import io.vertx.core.Future;

public interface UserRepository {
    Future<AuthUser> findByEmail(String email);
    Future<AuthUser> findByEmailAndVerify(String email);
    Future<AuthUser> findById(Integer userId);
    Future<AuthUser> createUser(CreateUserRequest request);
    Future<AuthUser> updateUserIsVerified(Integer userId, boolean isVerified);
    Future<AuthUser> updateUserPassword(Integer userId, String password);
    Future<AuthUser> findByVerificationCode(String verificationCode);
}
