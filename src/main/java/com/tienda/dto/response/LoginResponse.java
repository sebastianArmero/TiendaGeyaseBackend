package com.tienda.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String refreshToken;
    private String tipoToken = "Bearer";
    private Long id;
    private String username;
    private String email;
    private String nombreCompleto;
    private String rol;
    private Boolean requiereCambioPassword;
    private Long expiresIn;
}