package io.example.common.config;

import io.vertx.core.Vertx;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;

import io.vertx.ext.auth.PubSecKeyOptions;

public class JwtConfig {

  /** Primary env var for the HS256 signing key. */
  private static final String JWT_SECRET_ENV = "JWT_SECRET";

  /** Legacy name already present in docker.env / app-secrets. */
  private static final String LEGACY_SECRET_ENV = "SECRET_KEY";

  /**
   * Minimum length for an HS256 key. RFC 7518 §3.2 requires a key of at least the
   * hash output size (256 bits = 32 bytes) for HMAC-SHA256.
   */
  private static final int MIN_SECRET_LENGTH = 32;

  private JwtConfig() {
  }

  /**
   * Resolves the JWT signing secret from the environment.
   *
   * <p>There is no default value on purpose: a compile-time constant would let every
   * holder of the source forge valid tokens for any deployment. Missing or too-short
   * configuration fails fast at startup instead of silently accepting a weak key.
   *
   * @throws IllegalStateException when neither env var is set, or the value is shorter
   *                               than {@value #MIN_SECRET_LENGTH} characters
   */
  public static String resolveSecret() {
    return resolveSecret(System.getenv());
  }

  /** Testable overload: same rules, explicit env map. */
  static String resolveSecret(java.util.Map<String, String> env) {
    String secret = env.get(JWT_SECRET_ENV);
    if (secret == null || secret.isBlank()) {
      secret = env.get(LEGACY_SECRET_ENV);
    }

    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException(
          "JWT signing secret is not configured. Set the " + JWT_SECRET_ENV
              + " environment variable (or " + LEGACY_SECRET_ENV + ") to a value of at least "
              + MIN_SECRET_LENGTH + " characters.");
    }

    if (secret.length() < MIN_SECRET_LENGTH) {
      throw new IllegalStateException(
          JWT_SECRET_ENV + " must be at least " + MIN_SECRET_LENGTH
              + " characters for HS256, got " + secret.length()
              + ". Generate one with: openssl rand -base64 48");
    }

    return secret;
  }

  public static JWTAuth createProvider(Vertx vertx) {
    JWTAuthOptions config = new JWTAuthOptions()
        .addPubSecKey(new PubSecKeyOptions()
            .setAlgorithm("HS256")
            .setBuffer(resolveSecret()));

    return JWTAuth.create(vertx, config);
  }
}
