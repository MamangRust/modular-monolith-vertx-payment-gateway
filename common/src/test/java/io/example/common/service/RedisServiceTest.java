package io.example.common.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.vertx.core.Future;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.Response;

@ExtendWith(MockitoExtension.class)
class RedisServiceTest {

  @Mock
  private RedisAPI redisAPI;

  private RedisService service;

  @BeforeEach
  void setUp() {
    service = new RedisService(redisAPI, OpenTelemetrySdk.builder().build());
  }

  @Test
  void setIfAbsentReturnsTrueWhenKeyWasSet() {
    when(redisAPI.set(anyList())).thenReturn(Future.succeededFuture(mock(Response.class)));

    assertThat(service.setIfAbsent("k", "v", Duration.ofSeconds(60)).result()).isTrue();
    verify(redisAPI).set(List.of("k", "v", "NX", "EX", "60"));
  }

  @Test
  void setIfAbsentReturnsFalseWhenKeyAlreadyExists() {
    // SET NX on an existing key replies NIL, which the client surfaces as a null Response.
    when(redisAPI.set(anyList())).thenReturn(Future.succeededFuture(null));

    assertThat(service.setIfAbsent("k", "v", Duration.ofSeconds(60)).result()).isFalse();
    verify(redisAPI).set(List.of("k", "v", "NX", "EX", "60"));
  }

  @Test
  void ttlReturnsRemainingSeconds() {
    Response resp = mock(Response.class);
    when(resp.toLong()).thenReturn(42L);
    when(redisAPI.ttl("k")).thenReturn(Future.succeededFuture(resp));

    assertThat(service.ttl("k").result()).isEqualTo(42L);
  }

  @Test
  void ttlReturnsNegativeOneWhenNoExpiry() {
    Response resp = mock(Response.class);
    when(resp.toLong()).thenReturn(-1L);
    when(redisAPI.ttl("k")).thenReturn(Future.succeededFuture(resp));

    assertThat(service.ttl("k").result()).isEqualTo(-1L);
  }
}
