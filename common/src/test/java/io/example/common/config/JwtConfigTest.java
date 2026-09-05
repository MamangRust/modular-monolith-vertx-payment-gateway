package io.example.common.config;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtConfigTest {

  private static final String VALID = "0123456789012345678901234567890123456789";

  @Test
  @DisplayName("resolveSecret prefers JWT_SECRET")
  void prefersJwtSecret() {
    String secret = JwtConfig.resolveSecret(Map.of(
        "JWT_SECRET", VALID,
        "SECRET_KEY", VALID + "-legacy"));
    assertThat(secret).isEqualTo(VALID);
  }

  @Test
  @DisplayName("resolveSecret falls back to legacy SECRET_KEY")
  void fallsBackToLegacy() {
    String secret = JwtConfig.resolveSecret(Map.of("SECRET_KEY", VALID));
    assertThat(secret).isEqualTo(VALID);
  }

  @Test
  @DisplayName("resolveSecret fails when nothing is configured")
  void failsWhenMissing() {
    assertThatThrownBy(() -> JwtConfig.resolveSecret(Map.of()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("JWT signing secret is not configured");
  }

  @Test
  @DisplayName("resolveSecret fails on blank value")
  void failsWhenBlank() {
    assertThatThrownBy(() -> JwtConfig.resolveSecret(Map.of("JWT_SECRET", "   ")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("not configured");
  }

  @Test
  @DisplayName("resolveSecret rejects keys shorter than 32 chars (HS256 minimum)")
  void rejectsShortSecret() {
    assertThatThrownBy(() -> JwtConfig.resolveSecret(Map.of("JWT_SECRET", "my-secret-key")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("at least 32 characters");
  }

  @Test
  @DisplayName("resolveSecret rejects a short legacy SECRET_KEY too")
  void rejectsShortLegacySecret() {
    assertThatThrownBy(() -> JwtConfig.resolveSecret(Map.of("SECRET_KEY", "yantopedia")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("at least 32 characters");
  }
}
