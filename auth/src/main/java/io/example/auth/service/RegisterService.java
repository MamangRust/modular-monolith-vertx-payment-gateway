package io.example.auth.service;

import java.time.Duration;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.example.auth.domain.requests.CreateUserRequest;
import io.example.auth.domain.requests.RegisterRequest;
import io.example.auth.model.AuthUser;
import io.example.auth.model.Role;
import io.example.auth.repository.RoleRepository;
import io.example.auth.repository.UserRepository;
import io.example.auth.repository.UserRoleRepository;
import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RegisterService {
    private static final Logger logger = LoggerFactory.getLogger(RegisterService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;
    private final KafkaService kafkaService;

    public Future<AuthUser> register(RegisterRequest request) {
        var ctx = tracingMetrics.startSpan("RegisterService.register");

        return userRepository.findByEmail(request.getEmail())
                .compose(existingUser -> {
                    if (existingUser != null) {
                        return Future.failedFuture(new BadRequestException("User with this email already exists"));
                    }

                    String passwordHash = BCrypt.withDefaults().hashToString(12, request.getPassword().toCharArray());
                    request.setPassword(passwordHash);

                    return roleRepository.findByName("ROLE_USER");
                })
                .compose(role -> {
                    if (role == null) {
                        return Future.failedFuture(new NotFoundException("Default role not found: ROLE_USER"));
                    }

                    String verificationCode = UUID.randomUUID().toString().substring(0, 10);
                    request.setVerifiedCode(verificationCode);
                    request.setVerified(false);

                    CreateUserRequest createUserReq = CreateUserRequest.builder()
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .email(request.getEmail())
                            .password(request.getPassword())
                            .verificationCode(request.getVerifiedCode())
                            .build();

                    // Create user then safely pass both user and role to the next step
                    return userRepository.createUser(createUserReq).compose(newUser -> assignRoleAndCache(newUser, role, request));
                })
                .onSuccess(user -> {
                    tracingMetrics.completeSpanSuccess(ctx, "register", "User registered successfully");
                    logger.info("User registered successfully: {}", request.getEmail());
                })
                .onFailure(err -> {
                    tracingMetrics.completeSpanError(ctx, "register", err.getMessage());
                    logger.error("Registration failed for {}: {}", request.getEmail(), err.getMessage());
                });
    }

    private Future<AuthUser> assignRoleAndCache(AuthUser newUser, Role role, RegisterRequest request) {
        return userRoleRepository.assignRoleToUser(newUser.getUserId(), role.getRoleId())
                .compose(ur -> redisService.set("verification:" + request.getEmail(), request.getVerifiedCode(),
                        Duration.ofMinutes(15)))
                .compose(v -> sendWelcomeEmail(newUser, request.getVerifiedCode())
                        .recover(err -> {
                            logger.warn("Failed to send welcome email, but continuing registration", err);
                            return Future.succeededFuture();
                        }))
                .map(v -> newUser);
    }

    private Future<Void> sendWelcomeEmail(AuthUser user, String verificationCode) {
        if (kafkaService == null)
            return Future.succeededFuture();

        JsonObject emailPayload = new JsonObject()
                .put("email", user.getEmail())
                .put("subject", "Welcome to SanEdge")
                .put("body",
                        "Your account has been successfully created. Link: https://sanedge.example.com/login?verify_code="
                                + verificationCode);

        return kafkaService.sendMessage("email-service-topic-auth-register", user.getUserId().toString(), emailPayload);
    }
}