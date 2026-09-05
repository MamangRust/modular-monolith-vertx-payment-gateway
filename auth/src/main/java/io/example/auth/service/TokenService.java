package io.example.auth.service;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.jwt.JWTAuth;

public class TokenService {
    private final JWTAuth jwtProvider;

    public TokenService(JWTAuth jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    public String createAccessToken(Integer userId) {
        return createAccessToken(userId, java.util.List.of());
    }

    public String createAccessToken(Integer userId, java.util.List<String> roleNames) {
        JsonObject claims = tokenClaims(userId);
        if (roleNames != null && !roleNames.isEmpty()) {
            claims.put("roleNames", new io.vertx.core.json.JsonArray(roleNames));
        }
        return jwtProvider.generateToken(
            claims,
            new JWTOptions().setExpiresInMinutes(15)
        );
    }

    public String createRefreshToken(Integer userId) {
        return jwtProvider.generateToken(
            tokenClaims(userId),
            new JWTOptions().setExpiresInMinutes(1440) // 24 hours
        );
    }

    /**
     * Standard claims shared by both token kinds. {@code sub} keeps the subject for
     * standard consumers; {@code userId} is the numeric claim the API gateway reads
     * in JwtMiddleware/role checks and {@code /me}.
     */
    private static JsonObject tokenClaims(Integer userId) {
        return new JsonObject()
            .put("sub", userId.toString())
            .put("userId", userId);
    }
}
