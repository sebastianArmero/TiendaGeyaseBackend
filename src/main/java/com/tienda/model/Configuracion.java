package com.tienda.model;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "configuraciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Configuracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String clave;

    @Column(columnDefinition = "TEXT")
    private String valor;

    @Column(length = 50)
    @Builder.Default
    private String tipo = "TEXTO";

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(length = 50)
    private String categoria;

    @Column
    @Builder.Default
    private Boolean editable = true;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @PrePersist
    protected void onCreate() {
        creadoEn = LocalDateTime.now();
        actualizadoEn = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = LocalDateTime.now();
    }

    // Tipos de configuración
    public enum TipoConfiguracion {
        TEXTO, NUMERO, BOOLEANO, JSON, FECHA
    }
}