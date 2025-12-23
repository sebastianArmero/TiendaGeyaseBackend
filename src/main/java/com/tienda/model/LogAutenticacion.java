package com.tienda.model;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "logs_autenticacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogAutenticacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "username", length = 50)
    private String username;

    @Column(name = "tipo_evento", nullable = false, length = 50)
    private String tipoEvento;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "detalles", columnDefinition = "TEXT")
    private String detalles;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        creadoEn = LocalDateTime.now();
    }

    // Enums para tipos de eventos
    public enum TipoEvento {
        LOGIN_EXITOSO,
        LOGIN_FALLIDO,
        LOGOUT,
        CAMBIO_PASSWORD,
        BLOQUEO_USUARIO,
        INTENTO_ACCESO_NO_AUTORIZADO
    }
}