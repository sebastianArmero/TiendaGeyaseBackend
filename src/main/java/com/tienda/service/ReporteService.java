package com.tienda.service;

import com.tienda.dto.request.FiltroReporteRequest;
import com.tienda.dto.response.*;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ReporteService {

    // ============ CIERRE DIARIO ============
    CierreCajaResponse generarCierreDiario(Long cajaId, LocalDate fecha);
    CierreCajaResponse obtenerCierreDiario(Long cajaId, LocalDate fecha);
    List<CierreCajaResponse> obtenerHistorialCierres(LocalDate fechaInicio, LocalDate fechaFin);
    byte[] exportarCierreDiarioPDF(Long cierreId);
    byte[] exportarCierreDiarioExcel(Long cierreId);

    // ============ REPORTES DE VENTAS ============
    ReporteVentaResponse generarReporteVentas(FiltroReporteRequest filtro);
    Map<String, Object> generarEstadisticasVentas(LocalDate fechaInicio, LocalDate fechaFin);
    List<ReporteVentaResponse> generarReporteVentasPorVendedor(LocalDate fechaInicio, LocalDate fechaFin);
    List<ReporteVentaResponse> generarReporteVentasPorProducto(LocalDate fechaInicio, LocalDate fechaFin);
    List<ReporteVentaResponse> generarReporteVentasPorCategoria(LocalDate fechaInicio, LocalDate fechaFin);

    // ============ REPORTES DE INVENTARIO ============
    ReporteInventarioResponse generarReporteInventario();
    Map<String, Object> generarReporteMovimientosInventario(LocalDateTime fechaInicio, LocalDateTime fechaFin);
    List<ReporteInventarioResponse> generarReporteProductosBajoStock();
    List<ReporteInventarioResponse> generarReporteProductosAgotados();
    Map<String, Object> generarReporteRotacionProductos();

    // ============ REPORTES DE UTILIDADES ============
    Map<String, Object> generarReporteUtilidades(LocalDate fechaInicio, LocalDate fechaFin);
    Map<String, Object> generarReporteMargenGanancia();
    Map<String, Object> generarReporteTopProductosRentables();

    // ============ REPORTES DE CLIENTES ============
    Map<String, Object> generarReporteClientes();
    List<Map<String, Object>> generarReporteTopClientes(Integer limite);
    Map<String, Object> generarReporteFrecuenciaClientes();

    // ============ REPORTES FINANCIEROS ============
    Map<String, Object> generarBalanceGeneral(LocalDate fecha);
    Map<String, Object> generarEstadoResultados(LocalDate fechaInicio, LocalDate fechaFin);
    Map<String, Object> generarFlujoCaja(LocalDate fechaInicio, LocalDate fechaFin);

    // ============ REPORTES PERSONALIZADOS ============
    Map<String, Object> generarReportePersonalizado(FiltroReporteRequest filtro);
    PaginacionResponse<Map<String, Object>> generarReportePaginado(FiltroReporteRequest filtro, Pageable pageable);

    // ============ EXPORTACIÓN ============
    byte[] exportarReporteExcel(Map<String, Object> datos, String tipoReporte);
    byte[] exportarReportePDF(Map<String, Object> datos, String tipoReporte);
    byte[] exportarReporteCSV(Map<String, Object> datos, String tipoReporte);
}