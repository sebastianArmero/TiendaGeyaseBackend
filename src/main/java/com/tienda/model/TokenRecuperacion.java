package com.tienda.model;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tokens_recuperacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenRecuperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(unique = true, nullable = false, length = 100)
    private String token;

    @Column(length = 50)
    @Builder.Default
    private String tipo = "RECUPERACION";

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column
    @Builder.Default
    private Boolean utilizado = false;

    @Column(name = "fecha_utilizacion")
    private LocalDateTime fechaUtilizacion;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
    }

    // ============ MÉTODOS NUEVOS ============

    public boolean isValido() {
        return !utilizado && fechaExpiracion.isAfter(LocalDateTime.now());
    }

    public void marcarComoUtilizado() {
        this.utilizado = true;
        this.fechaUtilizacion = LocalDateTime.now();
    }

    // Getters y Setters
    public Boolean getUtilizado() {
        return utilizado;
    }

    public void setUtilizado(Boolean utilizado) {
        this.utilizado = utilizado;
    }

    public void setFechaUtilizacion(LocalDateTime fechaUtilizacion) {
        this.fechaUtilizacion = fechaUtilizacion;
    }

    public LocalDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public void setFechaExpiracion(LocalDateTime fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }
}