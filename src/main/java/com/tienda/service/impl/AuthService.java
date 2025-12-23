package com.tienda.service;

import com.tienda.dto.request.LoginRequest;
import com.tienda.dto.request.ResetPasswordRequest;
import com.tienda.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest loginRequest);
    void logout(String token);
    LoginResponse refreshToken(String refreshToken);
    void requestPasswordReset(String email);
    void resetPassword(ResetPasswordRequest request);
    void cambiarPassword(Long usuarioId, String oldPassword, String newPassword);
}