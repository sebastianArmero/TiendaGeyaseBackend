package com.tienda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadisticasDTO {

    // ✅ AGREGAR ESTOS CAMPOS:
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String periodo;

    // Ventas
    private BigDecimal ventasTotales;
    private Integer cantidadVentas;
    private BigDecimal ventasEfectivo;
    private BigDecimal ventasTarjeta;
    private BigDecimal ventasTransferencia;
    private BigDecimal ventasCredito;

    // Productos
    private Integer productosVendidos;
    private Integer productosDiferentesVendidos;
    private BigDecimal costoVentas;
    private BigDecimal utilidadBruta;

    // Clientes
    private Integer clientesAtendidos;
    private Integer nuevosClientes;
    private BigDecimal ticketPromedio;

    // Inventario
    private Integer productosBajoStock;
    private Integer productosAgotados;
    private BigDecimal valorInventario;

    // Métricas calculadas
    private BigDecimal margenUtilidad;
    private BigDecimal crecimientoVentas;
    private BigDecimal crecimientoClientes;

    // Para comparativas
    private BigDecimal ventasPeriodoAnterior;
    private BigDecimal crecimientoPorcentual;

    public BigDecimal calcularMargenUtilidad() {
        if (ventasTotales != null && ventasTotales.compareTo(BigDecimal.ZERO) > 0 &&
                costoVentas != null) {
            BigDecimal utilidad = ventasTotales.subtract(costoVentas);
            return utilidad.divide(ventasTotales, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
        return BigDecimal.ZERO;
    }
}