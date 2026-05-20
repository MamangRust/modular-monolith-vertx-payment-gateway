package io.example.auth.service;

import io.example.auth.model.AuthUser;
import io.example.auth.model.RegisterRequest;
import io.example.auth.model.Role;
import io.example.auth.repository.RoleRepository;
import io.example.auth.repository.UserRepository;
import io.example.auth.repository.UserRoleRepository;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.common.service.KafkaService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import at.favre.lib.crypto.bcrypt.BCrypt;

import java.time.Duration;
import java.util.UUID;

public class RegisterService {
    private static final Logger logger = LoggerFactory.getLogger(RegisterService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;
    private final KafkaService kafkaService;

    public RegisterService(UserRepository userRepository,
                           RoleRepository roleRepository,
                           UserRoleRepository userRoleRepository,
                           RedisService redisService,
                           TracingMetrics tracingMetrics,
                           KafkaService kafkaService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
        this.kafkaService = kafkaService;
    }

    public Future<AuthUser> register(RegisterRequest request) {
        String method = "Register";
        TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(method);

        return userRepository.findByEmail(request.getEmail())
            .compose(existingUser -> {
                if (existingUser != null) {
                    return Future.failedFuture("User with this email already exists");
                }

                // Hash password
                String passwordHash = BCrypt.withDefaults().hashToString(12, request.getPassword().toCharArray());
                request.setPassword(passwordHash);

                // Default role
                String defaultRoleName = "ROLE_ADMIN"; // As per Go code
                return roleRepository.findByName(defaultRoleName);
            })
            .compose(role -> {
                if (role == null) {
                    return Future.failedFuture("Default role not found: " + "ROLE_ADMIN");
                }

                // Generate verification code
                String verificationCode = UUID.randomUUID().toString().substring(0, 10);
                request.setVerifiedCode(verificationCode);
                request.setVerified(false);

                return userRepository.createUser(
                    request.getFirstName(),
                    request.getLastName(),
                    request.getEmail(),
                    request.getPassword(),
                    request.getVerifiedCode()
                ).map(user -> new Object[] { user, role });
            })
            .compose(data -> {
                AuthUser newUser = (AuthUser) data[0];
                Role role = (Role) data[1];

                // Assign role
                return userRoleRepository.assignRoleToUser(newUser.getUserId(), role.getRoleId())
                    .compose(ur -> {
                        // Cache verification code
                        return redisService.set("verification:" + request.getEmail(), request.getVerifiedCode(), Duration.ofMinutes(15));
                    })
                    .compose(v -> sendWelcomeEmail(newUser, request.getVerifiedCode())
                        .recover(err -> {
                            logger.warn("Failed to send welcome email, but continuing registration", err);
                            return Future.succeededFuture();
                        }))
                    .map(v -> newUser);
            })
            .onSuccess(user -> {
                tracingMetrics.completeSpanSuccess(tracingContext, method, "User registered successfully");
                logger.info("User registered successfully: {}", request.getEmail());
            })
            .onFailure(err -> {
                tracingMetrics.completeSpanError(tracingContext, method, err.getMessage());
                logger.error("Registration failed for {}: {}", request.getEmail(), err.getMessage());
            });
    }

    private Future<Void> sendWelcomeEmail(AuthUser user, String verificationCode) {
        if (kafkaService == null) {
            logger.warn("Kafka service not initialized, skipping welcome email for {}", user.getEmail());
            return Future.succeededFuture();
        }

        JsonObject emailPayload = new JsonObject()
            .put("email", user.getEmail())
            .put("subject", "Welcome to SanEdge")
            .put("body", "Your account has been successfully created. Link: https://sanedge.example.com/login?verify_code=" + verificationCode);

        return kafkaService.sendMessage("email-service-topic-auth-register", user.getUserId().toString(), emailPayload);
    }
}
