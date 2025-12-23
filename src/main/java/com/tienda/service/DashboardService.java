package com.tienda.service;

import com.tienda.dto.response.DashboardResponse;
import java.time.LocalDate;
import java.util.Map;

public interface DashboardService {

    // Métricas principales
    DashboardResponse obtenerMetricasPrincipales();
    DashboardResponse obtenerMetricasPrincipalesPorFecha(LocalDate fechaInicio, LocalDate fechaFin);

    // Métricas específicas
    Map<String, Object> obtenerMetricasVentas(LocalDate fechaInicio, LocalDate fechaFin);
    Map<String, Object> obtenerMetricasInventario();
    Map<String, Object> obtenerMetricasClientes();
    Map<String, Object> obtenerMetricasFinancieras(LocalDate fechaInicio, LocalDate fechaFin);

    // Análisis temporales
    Map<String, Object> obtenerVentasPorPeriodo(String periodo, LocalDate fechaInicio, LocalDate fechaFin);
    Map<String, Object> obtenerTendenciaVentas(int dias);
    Map<String, Object> obtenerComparativaPeriodos(LocalDate periodoActualInicio, LocalDate periodoActualFin,
                                                   LocalDate periodoAnteriorInicio, LocalDate periodoAnteriorFin);

    // Top listas
    Map<String, Object> obtenerTopProductos(int limite);
    Map<String, Object> obtenerTopClientes(int limite);
    Map<String, Object> obtenerTopVendedores(int limite);
    Map<String, Object> obtenerProductosBajoStock(int limite);

    // Alertas y notificaciones
    Map<String, Object> obtenerAlertasSistema();
    Map<String, Object> obtenerNotificacionesPendientes();

    // Métricas para widgets específicos
    Map<String, Object> obtenerWidgetMetricasVentas();
    Map<String, Object> obtenerWidgetMetricasInventario();
    Map<String, Object> obtenerWidgetMetricasClientes();
    Map<String, Object> obtenerWidgetMetricasFinancieras();

    // Métricas en tiempo real (para pantallas)
    Map<String, Object> obtenerMetricasTiempoReal();



    Map<String, Object> obtenerDashboardCompleto();
    Map<String, Object> obtenerDashboardVentas();
    Map<String, Object> obtenerDashboardInventario();
    Map<String, Object> obtenerMetricasClave();
    Map<String, Object> obtenerVentasUltimos7Dias();
    Map<String, Object> obtenerVentasPorMes(int año);
    Map<String, Object> obtenerEstadisticasRapidas();
    Map<String, Object> obtenerRendimientoSistema();
}