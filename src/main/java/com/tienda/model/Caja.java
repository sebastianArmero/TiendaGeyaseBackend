package com.tienda.model;

import lombok.*;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cajas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Caja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal saldoInicial = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal saldoActual = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private EstadoCaja estado = EstadoCaja.CERRADA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_asignado_id")
    private Usuario usuarioAsignado;

    @Column(name = "fecha_apertura")
    private LocalDateTime fechaApertura;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    // ✅ Añadir campos de auditoría
    @Column(name = "creado_en")
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @OneToMany(mappedBy = "caja", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Venta> ventas = new ArrayList<>();

    @OneToMany(mappedBy = "caja", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CierreCaja> cierres = new ArrayList<>();

    // ✅ Añadir métodos del ciclo de vida
    @PrePersist
    protected void onCreate() {
        this.creadoEn = LocalDateTime.now();
        this.actualizadoEn = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.actualizadoEn = LocalDateTime.now();
    }

    public boolean estaAbierta() {
        return estado == EstadoCaja.ABIERTA;
    }

    public boolean estaCerrada() {
        return estado == EstadoCaja.CERRADA;
    }

    public enum EstadoCaja {
        ABIERTA, CERRADA, BLOQUEADA, EN_AUDITORIA
    }
}