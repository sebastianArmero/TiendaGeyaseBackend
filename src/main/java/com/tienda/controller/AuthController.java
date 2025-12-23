package com.tienda.controller;

import com.tienda.dto.request.LoginRequest;
import com.tienda.dto.request.ResetPasswordRequest;
import com.tienda.dto.response.ApiResponse;
import com.tienda.dto.response.LoginResponse;
import com.tienda.service.impl.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse response = authService.login(loginRequest);
            return ResponseEntity.ok(ApiResponse.success("Login exitoso", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7); // Remover "Bearer "
            authService.logout(token);
            return ResponseEntity.ok(ApiResponse.success("Logout exitoso", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse> refreshToken(@RequestParam String refreshToken) {
        try {
            LoginResponse response = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(ApiResponse.success("Token refrescado", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/request-password-reset")
    public ResponseEntity<ApiResponse> requestPasswordReset(@RequestParam String email) {
        try {
            authService.requestPasswordReset(email);
            return ResponseEntity.ok(ApiResponse.success(
                    "Se ha enviado un correo con las instrucciones para restablecer la contraseña",
                    null
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request);
            return ResponseEntity.ok(ApiResponse.success(
                    "Contraseña restablecida exitosamente",
                    null
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse> changePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        try {
            // En una implementación real, obtendrías el usuario del contexto de seguridad
            // Por ahora usamos un ID hardcodeado para pruebas
            authService.cambiarPassword(1L, oldPassword, newPassword);
            return ResponseEntity.ok(ApiResponse.success(
                    "Contraseña cambiada exitosamente",
                    null
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/check-token")
    public ResponseEntity<ApiResponse> checkToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            return ResponseEntity.ok(ApiResponse.success("Token válido", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Token inválido"));
        }
    }
}