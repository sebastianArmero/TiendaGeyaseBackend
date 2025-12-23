package com.tienda.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteVentaResponse {

    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String tipoReporte; // DIARIO, MENSUAL, RANGO

    // Totales
    private Integer totalVentas;
    private BigDecimal totalIngresos;
    private BigDecimal totalDescuentos;
    private BigDecimal totalIva;
    private BigDecimal totalNeto;

    // Promedios
    private BigDecimal ticketPromedio;
    private Integer productosPromedioPorVenta;

    // Distribución
    private Map<String, BigDecimal> ventasPorFormaPago;
    private Map<String, Integer> ventasPorEstado;
    private Map<String, BigDecimal> ventasPorVendedor;

    // Top
    private List<Map<String, Object>> topProductos;
    private List<Map<String, Object>> topClientes;

    // Detalle diario (para reportes por rango)
    private List<Map<String, Object>> detalleDiario;

    // Estadísticas adicionales
    private Integer clientesAtendidos;
    private Integer vendedoresActivos;
    private BigDecimal crecimientoVsPeriodoAnterior;

    private LocalDate fechaGeneracion;
}