package com.tienda.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteVentaResponse {

    private String periodo;
    private LocalDate fecha;
    private LocalDateTime fechaHora;

    // TOTALES
    private BigDecimal totalVentas;
    private BigDecimal totalCosto;
    private BigDecimal totalUtilidad;
    private BigDecimal margenUtilidad;

    // ESTADÍSTICAS
    private Integer numeroVentas;
    private Integer numeroProductosVendidos;
    private Integer numeroClientes;
    private Integer numeroVendedores;

    // PROMEDIOS
    private BigDecimal ticketPromedio;
    private BigDecimal itemsPorVenta;
    private BigDecimal utilidadPromedio;

    // DESGLOSE
    private Map<String, BigDecimal> ventasPorFormaPago;
    private Map<String, BigDecimal> ventasPorVendedor;
    private Map<String, BigDecimal> ventasPorCategoria;
    private Map<String, BigDecimal> ventasPorHora;

    // TENDENCIAS
    private BigDecimal crecimientoVentas;
    private BigDecimal crecimientoUtilidad;
    private BigDecimal crecimientoClientes;

    // COMPARATIVAS
    private BigDecimal ventasPeriodoAnterior;
    private BigDecimal utilidadPeriodoAnterior;
    private BigDecimal variacionVentas;
    private BigDecimal variacionUtilidad;
}