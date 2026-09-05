package io.example.auth.service;

import io.example.auth.domain.requests.RegisterRequest;
import io.example.auth.model.AuthUser;
import io.example.auth.model.Role;
import io.example.auth.model.UserRole;
import io.example.auth.repository.RoleRepository;
import io.example.auth.repository.UserRepository;
import io.example.auth.repository.UserRoleRepository;
import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, VertxExtension.class})
class RegisterServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private RoleRepository roleRepository;

  @Mock
  private UserRoleRepository userRoleRepository;

  @Mock
  private RedisService redisService;

  @Mock
  private TracingMetrics tracingMetrics;

  @Mock
  private KafkaService kafkaService;

  private RegisterService service;

  @BeforeEach
  void setUp() {
    service = new RegisterService(userRepository, roleRepository, userRoleRepository,
        redisService, tracingMetrics, kafkaService);
  }

  private void mockTracing() {
    when(tracingMetrics.startSpan(anyString())).thenReturn(new TracingContext(null, null));
  }

  private AuthUser aUser() {
    return AuthUser.builder()
        .userId(1)
        .firstname("Alice")
        .lastname("Wonderland")
        .email("alice@example.com")
        .password("hashed-bcrypt")
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  private Role aRole() {
    return Role.builder()
        .roleId(10)
        .roleName("ROLE_USER")
        .build();
  }

  private RegisterRequest aRegisterRequest() {
    return RegisterRequest.builder()
        .firstName("Alice")
        .lastName("Wonderland")
        .email("alice@example.com")
        .password("plain-password")
        .build();
  }

  /* ─── register success ─── */

  @Test
  @DisplayName("register creates user, assigns role, caches code, sends email")
  void registerSuccess(VertxTestContext ctx) {
    mockTracing();
    var request = aRegisterRequest();
    var user = aUser();
    var role = aRole();

    when(userRepository.findByEmail(request.getEmail())).thenReturn(Future.succeededFuture(null));
    when(roleRepository.findByName("ROLE_USER")).thenReturn(Future.succeededFuture(role));
    when(userRepository.createUser(any())).thenReturn(Future.succeededFuture(user));
    when(userRoleRepository.assignRoleToUser(1, 10)).thenReturn(Future.succeededFuture(new UserRole(1, 10)));
    when(redisService.set(anyString(), anyString(), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));
    when(kafkaService.sendMessage(anyString(), anyString(), any())).thenReturn(Future.succeededFuture());

    service.register(request)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getUserId()).isEqualTo(1);
          assertThat(result.getEmail()).isEqualTo("alice@example.com");

          verify(kafkaService).sendMessage(eq("email-service-topic-auth-register"), eq("1"), any());
          ctx.completeNow();
        })));
  }

  /* ─── duplicate email ─── */

  @Test
  @DisplayName("register fails when email already exists")
  void registerDuplicateEmail(VertxTestContext ctx) {
    mockTracing();
    var request = aRegisterRequest();
    var existing = aUser();

    when(userRepository.findByEmail(request.getEmail())).thenReturn(Future.succeededFuture(existing));

    service.register(request)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(BadRequestException.class)
              .hasMessage("User with this email already exists");
          ctx.completeNow();
        })));
  }

  /* ─── role not found ─── */

  @Test
  @DisplayName("register fails when default role not found")
  void registerRoleNotFound(VertxTestContext ctx) {
    mockTracing();
    var request = aRegisterRequest();

    when(userRepository.findByEmail(request.getEmail())).thenReturn(Future.succeededFuture(null));
    when(roleRepository.findByName("ROLE_USER")).thenReturn(Future.succeededFuture(null));

    service.register(request)
        .onComplete(ctx.failing(err -> ctx.verify(() -> {
          assertThat(err).isInstanceOf(NotFoundException.class)
              .hasMessage("Default role not found: ROLE_USER");
          ctx.completeNow();
        })));
  }

  /* ─── email failure is tolerated ─── */

  @Test
  @DisplayName("register succeeds even when email fails")
  void registerEmailFailureTolerated(VertxTestContext ctx) {
    mockTracing();
    var request = aRegisterRequest();
    var user = aUser();
    var role = aRole();

    when(userRepository.findByEmail(request.getEmail())).thenReturn(Future.succeededFuture(null));
    when(roleRepository.findByName("ROLE_USER")).thenReturn(Future.succeededFuture(role));
    when(userRepository.createUser(any())).thenReturn(Future.succeededFuture(user));
    when(userRoleRepository.assignRoleToUser(1, 10)).thenReturn(Future.succeededFuture(new UserRole(1, 10)));
    when(redisService.set(anyString(), anyString(), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));
    when(kafkaService.sendMessage(anyString(), anyString(), any())).thenReturn(Future.failedFuture("Kafka down"));

    service.register(request)
        .onComplete(ctx.succeeding(result -> ctx.verify(() -> {
          assertThat(result).isNotNull();
          assertThat(result.getUserId()).isEqualTo(1);
          ctx.completeNow();
        })));
  }
}
