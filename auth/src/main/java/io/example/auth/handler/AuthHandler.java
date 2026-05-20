package io.example.auth.handler;

import io.example.auth.service.AuthCommandService;
import io.example.auth.service.IdentityService;
import io.example.auth.service.LoginService;
import io.example.auth.service.PasswordResetService;
import io.example.auth.service.RegisterService;
import io.vertx.core.Future;
import pb.Auth.ApiResponseForgotPassword;
import pb.Auth.ApiResponseGetMe;
import pb.Auth.ApiResponseLogin;
import pb.Auth.ApiResponseRefreshToken;
import pb.Auth.ApiResponseRegister;
import pb.Auth.ApiResponseResetPassword;
import pb.Auth.ApiResponseVerifyCode;
import pb.Auth.ForgotPasswordRequest;
import pb.Auth.GetMeRequest;
import pb.Auth.LoginRequest;
import pb.Auth.RefreshTokenRequest;
import pb.Auth.RegisterRequest;
import pb.Auth.ResetPasswordRequest;
import pb.Auth.TokenResponse;
import pb.Auth.VerifyCodeRequest;

public class AuthHandler implements pb.VertxAuthServiceGrpcServer.AuthServiceApi {
  private final RegisterService registerService;
  private final IdentityService identityService;
  private final PasswordResetService passwordResetService;
  private final LoginService loginService;

  public AuthHandler(AuthCommandService commandService,
      RegisterService registerService,
      IdentityService identityService,
      PasswordResetService passwordResetService,
      LoginService loginService) {
    this.registerService = registerService;
    this.identityService = identityService;
    this.passwordResetService = passwordResetService;
    this.loginService = loginService;
  }

  @Override
  public Future<ApiResponseVerifyCode> verifyCode(VerifyCodeRequest request) {
    return passwordResetService.verifyCode(request.getCode())
        .map(v -> ApiResponseVerifyCode.newBuilder()
            .setStatus("success")
            .setMessage("Verification code verified")
            .build());
  }

  @Override
  public Future<ApiResponseForgotPassword> forgotPassword(ForgotPasswordRequest request) {
    return passwordResetService.forgotPassword(request.getEmail())
        .map(v -> ApiResponseForgotPassword.newBuilder()
            .setStatus("success")
            .setMessage("Forgot password link sent to " + request.getEmail())
            .build());
  }

  @Override
  public Future<ApiResponseResetPassword> resetPassword(ResetPasswordRequest request) {
    io.example.auth.model.ResetPasswordRequest domainReq = io.example.auth.model.ResetPasswordRequest.builder()
        .resetToken(request.getResetToken())
        .password(request.getPassword())
        .confirmPassword(request.getConfirmPassword())
        .build();

    return passwordResetService.resetPassword(domainReq)
        .map(v -> ApiResponseResetPassword.newBuilder()
            .setStatus("success")
            .setMessage("Password has been reset successfully")
            .build());
  }

  @Override
  public Future<ApiResponseRegister> registerUser(RegisterRequest request) {
    io.example.auth.model.RegisterRequest domainReq = io.example.auth.model.RegisterRequest.builder()
        .firstName(request.getFirstname())
        .lastName(request.getLastname())
        .email(request.getEmail())
        .password(request.getPassword())
        .build();

    return registerService.register(domainReq)
        .map(userModel -> ApiResponseRegister.newBuilder()
            .setStatus("success")
            .setMessage("User registered successfully")
            .setData(ProtoConverter.toUserResponse(userModel))
            .build());
  }

  @Override
  public Future<ApiResponseLogin> loginUser(LoginRequest request) {
    io.example.auth.model.AuthRequest domainReq = io.example.auth.model.AuthRequest.builder()
        .email(request.getEmail())
        .password(request.getPassword())
        .build();

    return loginService.login(domainReq)
        .map(resp -> ApiResponseLogin.newBuilder()
            .setStatus("success")
            .setMessage("Login successful")
            .setData(TokenResponse.newBuilder()
                .setAccessToken(resp.getAccessToken())
                .setRefreshToken(resp.getRefreshToken())
                .build())
            .build());
  }

  @Override
  public Future<ApiResponseRefreshToken> refreshToken(RefreshTokenRequest request) {
    return identityService.refreshToken(request.getRefreshToken())
        .map(resp -> ApiResponseRefreshToken.newBuilder()
            .setStatus("success")
            .setMessage("Token refreshed successfully")
            .setData(TokenResponse.newBuilder()
                .setAccessToken(resp.getAccessToken())
                .setRefreshToken(resp.getRefreshToken())
                .build())
            .build());
  }

  @Override
  public Future<ApiResponseGetMe> getMe(GetMeRequest request) {
    return identityService.getMe(request.getUserId())
        .map(userModel -> ApiResponseGetMe.newBuilder()
            .setStatus("success")
            .setMessage("Current user fetched successfully")
            .setData(ProtoConverter.toUserResponse(userModel))
            .build());
  }
}
