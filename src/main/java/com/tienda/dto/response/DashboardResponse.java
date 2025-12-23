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
public class DashboardResponse {

    // Resumen general
    private ResumenGeneral resumen;

    // Métricas principales
    private MetricasPrincipales metricas;

    // Datos para gráficos
    private DatosGraficos graficos;

    // Top listas
    private TopListas tops;

    // Alertas
    private List<AlertaDashboard> alertas;

    // Tendencia
    private Tendencia tendencia;

    // @Nested Classes
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumenGeneral {
        private LocalDate fechaConsulta;
        private String periodo;
        private Integer totalVentasHoy;
        private BigDecimal ventasHoy;
        private Integer nuevosClientesHoy;
        private Integer productosVendidosHoy;
        private Integer alertasActivas;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricasPrincipales {
        // Ventas
        private BigDecimal ventasTotales;
        private BigDecimal ventasPromedioDiarias;
        private BigDecimal ticketPromedio;
        private Integer transaccionesTotales;

        // Inventario
        private Integer totalProductos;
        private Integer productosBajoStock;
        private Integer productosAgotados;
        private BigDecimal valorInventario;

        // Clientes
        private Integer totalClientes;
        private Integer nuevosClientes;
        private BigDecimal tasaRetencion;

        // Financiero
        private BigDecimal utilidadBruta;
        private BigDecimal margenUtilidad;
        private BigDecimal gastosTotales;
        private BigDecimal ingresosTotales;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatosGraficos {
        private List<Map<String, Object>> ventasPorDia;
        private List<Map<String, Object>> ventasPorCategoria;
        private List<Map<String, Object>> ventasPorFormaPago;
        private List<Map<String, Object>> tendenciaVentas;
        private List<Map<String, Object>> productosMasVendidos;
        private List<Map<String, Object>> estadoInventario;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopListas {
        private List<Map<String, Object>> topProductos;
        private List<Map<String, Object>> topClientes;
        private List<Map<String, Object>> topVendedores;
        private List<Map<String, Object>> productosParaReorden;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertaDashboard {
        private String tipo;
        private String titulo;
        private String mensaje;
        private String prioridad; // BAJA, MEDIA, ALTA, CRITICA
        private String fecha;
        private Boolean leida;
        private String accion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tendencia {
        private BigDecimal crecimientoVentas;
        private BigDecimal crecimientoClientes;
        private BigDecimal crecimientoUtilidad;
        private String tendenciaVentas; // POSITIVA, NEGATIVA, NEUTRAL
        private String tendenciaInventario;
        private String tendenciaClientes;
    }
}