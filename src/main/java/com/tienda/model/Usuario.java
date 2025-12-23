package com.tienda.model;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario extends BaseEntity {

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "nombre_completo", nullable = false, length = 200)
    private String nombreCompleto;

    @Column(name = "documento_identidad", length = 50)
    private String documentoIdentidad;

    @Column(length = 50)
    private String telefono;

    @Column(columnDefinition = "TEXT")
    private String direccion;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private EstadoUsuario estado = EstadoUsuario.ACTIVO;

    @Column(name = "intentos_login")
    @Builder.Default
    private Integer intentosLogin = 0;

    @Column(name = "fecha_bloqueo")
    private LocalDateTime fechaBloqueo;

    @Column(name = "fecha_ultimo_login")
    private LocalDateTime fechaUltimoLogin;

    @Column(name = "fecha_ultimo_cambio_password")
    private LocalDateTime fechaUltimoCambioPassword;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id")
    private Rol rol;

    @Column(name = "es_super_admin")
    @Builder.Default
    private Boolean esSuperAdmin = false;

    @Column(name = "requiere_cambio_password")
    @Builder.Default
    private Boolean requiereCambioPassword = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caja_id")
    private Caja caja;

    @Column(name = "tema_preferido", length = 20)
    @Builder.Default
    private String temaPreferido = "claro";

    @Column(name = "idioma_preferido", length = 10)
    @Builder.Default
    private String idiomaPreferido = "es";

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<SesionUsuario> sesiones = new HashSet<>();

    // ============ MÉTODOS NUEVOS ============

    public boolean isActivo() {
        return estado == EstadoUsuario.ACTIVO;
    }

    public boolean isBloqueado() {
        return estado == EstadoUsuario.BLOQUEADO;
    }

    public void incrementarIntentosLogin() {
        this.intentosLogin++;
        if (this.intentosLogin >= 5) {
            this.estado = EstadoUsuario.BLOQUEADO;
            this.fechaBloqueo = LocalDateTime.now();
        }
    }

    public void resetearIntentosLogin() {
        this.intentosLogin = 0;
        if (this.estado == EstadoUsuario.BLOQUEADO) {
            this.estado = EstadoUsuario.ACTIVO;
            this.fechaBloqueo = null;
        }
    }

    // Getters y Setters adicionales (Lombok ya crea los básicos)
    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public LocalDateTime getFechaUltimoLogin() {
        return fechaUltimoLogin;
    }

    public void setFechaUltimoLogin(LocalDateTime fechaUltimoLogin) {
        this.fechaUltimoLogin = fechaUltimoLogin;
    }

    public LocalDateTime getFechaUltimoCambioPassword() {
        return fechaUltimoCambioPassword;
    }

    public void setFechaUltimoCambioPassword(LocalDateTime fechaUltimoCambioPassword) {
        this.fechaUltimoCambioPassword = fechaUltimoCambioPassword;
    }

    public Boolean getRequiereCambioPassword() {
        return requiereCambioPassword;
    }

    public void setRequiereCambioPassword(Boolean requiereCambioPassword) {
        this.requiereCambioPassword = requiereCambioPassword;
    }

    public Integer getIntentosLogin() {
        return intentosLogin;
    }

    public void setIntentosLogin(Integer intentosLogin) {
        this.intentosLogin = intentosLogin;
    }

    public enum EstadoUsuario {
        ACTIVO, INACTIVO, BLOQUEADO, ELIMINADO
    }
}