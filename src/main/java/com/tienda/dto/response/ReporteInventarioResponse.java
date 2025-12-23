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
public class ReporteInventarioResponse {

    private LocalDate fechaReporte;

    // Resumen general
    private Integer totalProductos;
    private Integer productosActivos;
    private Integer productosInactivos;

    // Stock
    private Integer productosConStock;
    private Integer productosAgotados;
    private Integer productosBajoStock;
    private Integer productosSobreStock;

    // Valoración
    private BigDecimal valorTotalCosto;
    private BigDecimal valorTotalVenta;
    private BigDecimal utilidadPotencial;
    private BigDecimal margenPromedio;

    // Alertas
    private List<Map<String, Object>> alertasCriticas;
    private List<Map<String, Object>> productosParaReorden;

    // Distribución
    private Map<String, Integer> productosPorCategoria;
    private Map<String, BigDecimal> valorPorCategoria;

    // Movimientos recientes
    private List<Map<String, Object>> ultimosMovimientos;

    // Productos más valiosos
    private List<Map<String, Object>> productosMasValiosos;

    // Rotación
    private Map<String, BigDecimal> rotacionPorCategoria;

    private LocalDate fechaGeneracion;
}