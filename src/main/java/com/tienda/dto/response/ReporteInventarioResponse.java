package com.tienda.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteInventarioResponse {

    private String tipoReporte;
    private LocalDateTime fechaGeneracion;

    // VALORACIÓN
    private BigDecimal valorTotalCosto;
    private BigDecimal valorTotalVenta;
    private BigDecimal utilidadPotencial;
    private BigDecimal margenPromedio;

    // ESTADÍSTICAS
    private Integer totalProductos;
    private Integer productosActivos;
    private Integer productosInactivos;
    private Integer productosConStock;
    private Integer productosSinStock;

    // ALERTAS DE STOCK
    private Integer productosAgotados;
    private Integer productosCriticos;
    private Integer productosBajoStock;
    private Integer productosSobreStock;
    private Integer productosParaReorden;

    // ROTACIÓN
    private BigDecimal rotacionPromedio;
    private Integer diasInventarioPromedio;
    private Map<String, BigDecimal> rotacionPorCategoria;

    // TOP PRODUCTOS
    private Map<String, Object> topProductosValor;
    private Map<String, Object> topProductosRotacion;
    private Map<String, Object> topProductosUtilidad;

    // DETALLES
    private Map<String, Integer> productosPorCategoria;
    private Map<String, BigDecimal> valorPorCategoria;
    private Map<String, Integer> alertasPorTipo;
}