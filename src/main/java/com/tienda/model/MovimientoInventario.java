package com.tienda.model;

import lombok.*;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_inventario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "tipo_movimiento", nullable = false, length = 50)
    private String tipoMovimiento;

    @Column(length = 50)
    private String subtipo;

    @Column(name = "documento_id")
    private Long documentoId;

    @Column(name = "documento_numero", length = 100)
    private String documentoNumero;

    @Column(name = "fecha_movimiento")
    private LocalDateTime fechaMovimiento;

    @Column(precision = 15, scale = 3)
    private BigDecimal cantidad;

    @Column(name = "cantidad_anterior", precision = 15, scale = 3)
    private BigDecimal cantidadAnterior;

    @Column(name = "cantidad_nueva", precision = 15, scale = 3)
    private BigDecimal cantidadNueva;

    @Column(name = "costo_unitario", precision = 15, scale = 2)
    private BigDecimal costoUnitario;

    @Column(name = "precio_unitario", precision = 15, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "ubicacion_origen", length = 100)
    private String ubicacionOrigen;

    @Column(name = "ubicacion_destino", length = 100)
    private String ubicacionDestino;

    @Column(length = 500)
    private String motivo;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        fechaMovimiento = LocalDateTime.now();
        creadoEn = LocalDateTime.now();
    }

    public enum TipoMovimiento {
        ENTRADA, SALIDA, AJUSTE, TRANSFERENCIA, DEVOLUCION
    }
}