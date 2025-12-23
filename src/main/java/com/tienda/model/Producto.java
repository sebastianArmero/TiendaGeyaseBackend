package com.tienda.model;

import lombok.*;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto extends BaseEntity {

    @Column(unique = true, nullable = false, length = 50)
    private String codigo;

    @Column(length = 100)
    private String codigoBarras;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(length = 100)
    private String subcategoria;

    @Column(length = 100)
    private String marca;

    @Column(length = 100)
    private String modelo;

    // ============ STOCK CON ESTADOS ============
    @Column(name = "stock_actual", precision = 15, scale = 3)
    @Builder.Default
    private BigDecimal stockActual = BigDecimal.ZERO;

    @Column(name = "stock_disponible", precision = 15, scale = 3)
    @Builder.Default
    private BigDecimal stockDisponible = BigDecimal.ZERO;

    @Column(name = "stock_reservado", precision = 15, scale = 3)
    @Builder.Default
    private BigDecimal stockReservado = BigDecimal.ZERO;

    @Column(name = "stock_minimo", precision = 15, scale = 3)
    @Builder.Default
    private BigDecimal stockMinimo = new BigDecimal("5");

    @Column(name = "stock_maximo", precision = 15, scale = 3)
    private BigDecimal stockMaximo;

    // ============ PRECIOS ============
    @Column(name = "costo_promedio", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal costoPromedio = BigDecimal.ZERO;

    @Column(name = "costo_ultimo", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal costoUltimo = BigDecimal.ZERO;

    @Column(name = "precio_venta", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal precioVenta = BigDecimal.ZERO;

    @Column(name = "precio_venta2", precision = 15, scale = 2)
    private BigDecimal precioVenta2;

    @Column(name = "precio_venta3", precision = 15, scale = 2)
    private BigDecimal precioVenta3;

    @Column(name = "margen_ganancia", precision = 5, scale = 2)
    private BigDecimal margenGanancia;

    // ============ UNIDADES ============
    @Column(name = "unidad_medida", length = 50)
    @Builder.Default
    private String unidadMedida = "UNIDAD";

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_producto", length = 20)
    @Builder.Default
    private TipoProducto tipoProducto = TipoProducto.NORMAL;

    @Column(name = "peso_unitario", precision = 10, scale = 3)
    @Builder.Default
    private BigDecimal pesoUnitario = BigDecimal.ONE;

    @Column(name = "volumen_unitario", precision = 10, scale = 3)
    private BigDecimal volumenUnitario;

    @Column(name = "permite_decimal")
    @Builder.Default
    private Boolean permiteDecimal = false;

    // ============ PROVEEDOR ============
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @Column(length = 100)
    private String ubicacion;

    // ============ ESTADOS ============
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private EstadoProducto estado = EstadoProducto.ACTIVO;

    @Enumerated(EnumType.STRING)
    @Column(name = "alerta_stock", length = 20)
    @Builder.Default
    private AlertaStock alertaStock = AlertaStock.NORMAL;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(length = 100)
    private String lote;

    // ============ MÉTODOS DE NEGOCIO ============
    @PreUpdate
    @PrePersist
    public void calcularEstadoStock() {
        // 1. Calcular stock disponible
        this.stockDisponible = this.stockActual.subtract(
                this.stockReservado != null ? this.stockReservado : BigDecimal.ZERO
        );

        // 2. Calcular alerta de stock
        if (this.stockActual.compareTo(BigDecimal.ZERO) <= 0) {
            this.alertaStock = AlertaStock.AGOTADO;
        } else if (this.stockActual.compareTo(
                this.stockMinimo.multiply(new BigDecimal("0.3"))) <= 0) {
            this.alertaStock = AlertaStock.CRITICO;
        } else if (this.stockActual.compareTo(this.stockMinimo) <= 0) {
            this.alertaStock = AlertaStock.BAJO;
        } else if (this.stockMaximo != null &&
                this.stockActual.compareTo(this.stockMaximo) > 0) {
            this.alertaStock = AlertaStock.SOBRE;
        } else {
            this.alertaStock = AlertaStock.NORMAL;
        }

        // 3. Calcular margen de ganancia si hay costo
        if (this.costoPromedio != null &&
                this.costoPromedio.compareTo(BigDecimal.ZERO) > 0 &&
                this.precioVenta != null &&
                this.precioVenta.compareTo(BigDecimal.ZERO) > 0) {

            BigDecimal utilidad = this.precioVenta.subtract(this.costoPromedio);
            this.margenGanancia = utilidad
                    .divide(this.costoPromedio, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
    }

    public boolean tieneStockSuficiente(BigDecimal cantidad) {
        return this.stockDisponible.compareTo(cantidad) >= 0;
    }

    public void reservarStock(BigDecimal cantidad) {
        this.stockReservado = this.stockReservado.add(cantidad);
        calcularEstadoStock();
    }

    public void liberarStock(BigDecimal cantidad) {
        this.stockReservado = this.stockReservado.subtract(cantidad);
        calcularEstadoStock();
    }

    public BigDecimal getValorCosto() {
        return this.stockActual.multiply(this.costoPromedio);
    }

    public BigDecimal getValorVenta() {
        return this.stockActual.multiply(this.precioVenta);
    }

    public BigDecimal getUtilidadPotencial() {
        return getValorVenta().subtract(getValorCosto());
    }

    // ============ ENUMS ============
    public enum TipoProducto {
        NORMAL, PESO, MEDIDA, SERIALIZADO, KIT
    }

    public enum EstadoProducto {
        ACTIVO, INACTIVO, DESCONTINUADO, BLOQUEADO
    }

    public enum AlertaStock {
        NORMAL, BAJO, CRITICO, SOBRE, AGOTADO
    }
}