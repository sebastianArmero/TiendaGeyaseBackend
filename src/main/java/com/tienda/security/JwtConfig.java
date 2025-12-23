package com.tienda.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtConfig {
    private String secret;
    private Long expiration = 86400000L; // 24 horas
    private Long refreshExpiration = 604800000L; // 7 días
}