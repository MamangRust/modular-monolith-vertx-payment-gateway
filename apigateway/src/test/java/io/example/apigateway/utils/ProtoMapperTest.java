package io.example.apigateway.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import pb.Auth;
import pb.common.ErrorResponse;

import static org.assertj.core.api.Assertions.assertThat;

class ProtoMapperTest {

  @Test
  @DisplayName("toJson serializes a protobuf message including default values")
  void toJsonIncludesDefaultValues() {
    var msg = Auth.ApiResponseLogin.newBuilder()
        .setStatus("success")
        .setMessage("login ok")
        .setData(Auth.TokenResponse.newBuilder()
            .setAccessToken("at-abc")
            .setRefreshToken("rt-xyz")
            .build())
        .build();

    var json = ProtoMapper.toJson(msg);

    assertThat(json.getString("status")).isEqualTo("success");
    assertThat(json.getString("message")).isEqualTo("login ok");
    assertThat(json.getJsonObject("data")).isNotNull();
    assertThat(json.getJsonObject("data").getString("access_token")).isEqualTo("at-abc");
    assertThat(json.getJsonObject("data").getString("refresh_token")).isEqualTo("rt-xyz");
  }

  @Test
  @DisplayName("toJson returns empty JsonObject for null input")
  void toJsonNullInput() {
    var json = ProtoMapper.toJson(null);
    assertThat(json).isNotNull();
    assertThat(json.isEmpty()).isTrue();
  }

  @Test
  @DisplayName("toJson serializes ErrorResponse with all fields")
  void toJsonErrorResponse() {
    var msg = ErrorResponse.newBuilder()
        .setStatus("error")
        .setMessage("not found")
        .setCode(404)
        .build();

    var json = ProtoMapper.toJson(msg);

    assertThat(json.getString("status")).isEqualTo("error");
    assertThat(json.getString("message")).isEqualTo("not found");
    assertThat(json.getInteger("code")).isEqualTo(404);
  }

  @Test
  @DisplayName("toJson preserves proto field names (snake_case)")
  void toJsonPreservesProtoFieldNames() {
    var msg = Auth.TokenResponse.newBuilder()
        .setAccessToken("at-test")
        .setRefreshToken("rt-test")
        .build();

    var json = ProtoMapper.toJson(msg);

    // Proto field names are access_token and refresh_token (snake_case)
    assertThat(json.containsKey("access_token")).isTrue();
    assertThat(json.containsKey("refresh_token")).isTrue();
    assertThat(json.containsKey("accessToken")).isFalse();
    assertThat(json.containsKey("refreshToken")).isFalse();
  }
}
