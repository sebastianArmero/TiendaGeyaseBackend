package com.tienda.model;

import lombok.*;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "cierres_caja")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CierreCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caja_id", nullable = false)
    private Caja caja;

    @Column(name = "fecha_cierre", nullable = false)
    private LocalDate fechaCierre;

    @Column(name = "hora_apertura")
    private LocalTime horaApertura;

    @Column(name = "hora_cierre")
    private LocalTime horaCierre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "saldo_inicial", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal saldoInicial = BigDecimal.ZERO;

    @Column(name = "saldo_final_teorico", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal saldoFinalTeorico = BigDecimal.ZERO;

    @Column(name = "saldo_final_real", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal saldoFinalReal = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal diferencia;

    @Column(name = "total_ventas", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalVentas = BigDecimal.ZERO;

    @Column(name = "total_compras", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalCompras = BigDecimal.ZERO;

    @Column(name = "total_gastos", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalGastos = BigDecimal.ZERO;

    @Column(name = "total_ingresos", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalIngresos = BigDecimal.ZERO;

    @Column(name = "total_egresos", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalEgresos = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal efectivo = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal tarjetas = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal transferencias = BigDecimal.ZERO;

    @Column(name = "otros_medios", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal otrosMedios = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @Builder.Default
    private EstadoCierre estado = EstadoCierre.PENDIENTE;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conciliado_por")
    private Usuario conciliadoPor;

    @Column(name = "fecha_conciliacion")
    private LocalDateTime fechaConciliacion;

    @PrePersist
    protected void onCreate() {
        creadoEn = LocalDateTime.now();
        calcularDiferencia();
    }

    @PreUpdate
    protected void onUpdate() {
        calcularDiferencia();
    }

    private void calcularDiferencia() {
        if (saldoFinalReal != null && saldoFinalTeorico != null) {
            diferencia = saldoFinalReal.subtract(saldoFinalTeorico);
        }
    }

    public BigDecimal getDiferencia() {
        calcularDiferencia();
        return diferencia;
    }

    public enum EstadoCierre {
        PENDIENTE, CONCILIADO, APROBADO, RECHAZADO
    }
}