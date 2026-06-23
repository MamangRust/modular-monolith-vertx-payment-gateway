package io.example.auth.handler;

import io.example.auth.service.IdentityService;
import io.example.auth.service.LoginService;
import io.example.auth.service.PasswordResetService;
import io.example.auth.service.RegisterService;
import io.example.common.grpc.GrpcExceptionMapper;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.Auth.*;

@RequiredArgsConstructor
public class AuthHandler implements pb.VertxAuthServiceGrpcServer.AuthServiceApi {
    private final RegisterService registerService;
    private final IdentityService identityService;
    private final PasswordResetService passwordResetService;
    private final LoginService loginService;

    @Override
    public Future<ApiResponseVerifyCode> verifyCode(VerifyCodeRequest request) {
        return passwordResetService.verifyCode(request.getCode())
                .map(v -> ApiResponseVerifyCode.newBuilder()
                        .setStatus("success")
                        .setMessage("Verification code verified")
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseForgotPassword> forgotPassword(ForgotPasswordRequest request) {
        return passwordResetService.forgotPassword(request.getEmail())
                .map(v -> ApiResponseForgotPassword.newBuilder()
                        .setStatus("success")
                        .setMessage("Forgot password link sent to " + request.getEmail())
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseResetPassword> resetPassword(ResetPasswordRequest request) {
        io.example.auth.domain.requests.ResetPasswordRequest domainReq = io.example.auth.domain.requests.ResetPasswordRequest.builder()
                .resetToken(request.getResetToken())
                .password(request.getPassword())
                .confirmPassword(request.getConfirmPassword())
                .build();

        return passwordResetService.resetPassword(domainReq)
                .map(v -> ApiResponseResetPassword.newBuilder()
                        .setStatus("success")
                        .setMessage("Password has been reset successfully")
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseRegister> registerUser(RegisterRequest request) {
        io.example.auth.domain.requests.RegisterRequest domainReq = io.example.auth.domain.requests.RegisterRequest.builder()
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
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseLogin> loginUser(LoginRequest request) {
        io.example.auth.domain.requests.AuthRequest domainReq = io.example.auth.domain.requests.AuthRequest.builder()
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
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
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
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseGetMe> getMe(GetMeRequest request) {
        return identityService.getMe(request.getUserId())
                .map(userModel -> ApiResponseGetMe.newBuilder()
                        .setStatus("success")
                        .setMessage("Current user fetched successfully")
                        .setData(ProtoConverter.toUserResponse(userModel))
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }
}