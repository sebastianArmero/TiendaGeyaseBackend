package com.tienda.model;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sesiones_usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SesionUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 500)
    private String token;

    @Column(name = "refresh_token", length = 500)
    private String refreshToken;

    @Column(length = 200)
    private String dispositivo;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "fecha_ultima_actividad")
    private LocalDateTime fechaUltimaActividad;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private EstadoSesion estado = EstadoSesion.ACTIVA;

    @Column(name = "motivo_revocacion", columnDefinition = "TEXT")
    private String motivoRevocacion;

    @PrePersist
    protected void onCreate() {
        fechaInicio = LocalDateTime.now();
        fechaUltimaActividad = LocalDateTime.now();
    }

    // ============ MÉTODOS NUEVOS ============

    public boolean isActiva() {
        return estado == EstadoSesion.ACTIVA &&
                fechaExpiracion.isAfter(LocalDateTime.now());
    }

    public void actualizarActividad() {
        fechaUltimaActividad = LocalDateTime.now();
    }

    // Getters y Setters
    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public EstadoSesion getEstado() {
        return estado;
    }

    public void setEstado(EstadoSesion estado) {
        this.estado = estado;
    }

    public void setFechaExpiracion(LocalDateTime fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }

    public enum EstadoSesion {
        ACTIVA, EXPIRADA, REVOCADA, CERRADA
    }
}