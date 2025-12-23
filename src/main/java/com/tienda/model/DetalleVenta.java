package com.tienda.model;

import lombok.*;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "detalle_ventas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "codigo_producto", length = 50)
    private String codigoProducto;

    @Column(name = "nombre_producto", length = 200)
    private String nombreProducto;

    @Column(name = "unidad_medida", length = 50)
    private String unidadMedida;

    @Column(precision = 15, scale = 3)
    private BigDecimal cantidad;

    @Column(name = "cantidad_devuelta", precision = 15, scale = 3)
    @Builder.Default
    private BigDecimal cantidadDevuelta = BigDecimal.ZERO;

    @Column(name = "precio_unitario", precision = 15, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "costo_unitario", precision = 15, scale = 2)
    private BigDecimal costoUnitario;

    @Column(name = "descuento_unitario", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal descuentoUnitario = BigDecimal.ZERO;

    @Column(name = "descuento_porcentaje", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal descuentoPorcentaje = BigDecimal.ZERO;

    @Column(name = "iva_porcentaje", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal ivaPorcentaje = BigDecimal.ZERO;

    @Column(name = "iva_valor", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal ivaValor = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 15, scale = 2)
    private BigDecimal total;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        creadoEn = LocalDateTime.now();
        calcularTotales();

        // Guardar información del producto
        if (producto != null) {
            codigoProducto = producto.getCodigo();
            nombreProducto = producto.getNombre();
            unidadMedida = producto.getUnidadMedida();
            costoUnitario = producto.getCostoPromedio();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        calcularTotales();
    }

    public void calcularTotales() {
        // Calcular descuento
        if (descuentoPorcentaje.compareTo(BigDecimal.ZERO) > 0) {
            descuentoUnitario = precioUnitario
                    .multiply(descuentoPorcentaje)
                    .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        }

        // Calcular subtotal
        subtotal = cantidad.multiply(precioUnitario).subtract(descuentoUnitario);

        // Calcular IVA
        ivaValor = subtotal.multiply(ivaPorcentaje)
                .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);

        // Calcular total
        total = subtotal.add(ivaValor);
    }

    public BigDecimal getSubtotal() {
        calcularTotales();
        return subtotal;
    }

    public BigDecimal getTotal() {
        calcularTotales();
        return total;
    }
}