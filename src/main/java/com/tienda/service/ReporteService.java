package com.tienda.service;

import com.tienda.dto.response.CierreCajaResponse;
import com.tienda.dto.response.ReporteInventarioResponse;
import com.tienda.dto.response.ReporteVentaResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ReporteService {

    // ============ CIERRE DE CAJA ============
    CierreCajaResponse iniciarCierreCaja(Long cajaId, Long usuarioId, BigDecimal saldoInicial);
    CierreCajaResponse finalizarCierreCaja(Long id, BigDecimal saldoFinalReal, String observaciones);
    CierreCajaResponse obtenerCierreCajaPorId(Long id);
    CierreCajaResponse obtenerCierreCajaHoy(Long cajaId);
    List<CierreCajaResponse> obtenerCierresCaja(Long cajaId, LocalDate fechaDesde, LocalDate fechaHasta, String estado);
    CierreCajaResponse conciliarCierreCaja(Long id, Long usuarioId, String observaciones);

    // ============ REPORTES DE VENTAS ============
    ReporteVentaResponse generarReporteVentasDiario(LocalDate fecha);
    ReporteVentaResponse generarReporteVentasMensual(int mes, int año);
    ReporteVentaResponse generarReporteVentasRango(LocalDate fechaDesde, LocalDate fechaHasta);
    List<Map<String, Object>> generarReporteVentasPorVendedor(LocalDate fechaDesde, LocalDate fechaHasta);
    List<Map<String, Object>> generarReporteVentasPorProducto(LocalDate fechaDesde, LocalDate fechaHasta);
    List<Map<String, Object>> generarReporteVentasPorCliente(LocalDate fechaDesde, LocalDate fechaHasta);

    // ============ REPORTES DE INVENTARIO ============
    ReporteInventarioResponse generarReporteInventarioEstado();
    List<Map<String, Object>> generarReporteMovimientosInventario(LocalDate fechaDesde, LocalDate fechaHasta, Long productoId, String tipoMovimiento);
    Map<String, Object> generarReporteValoracionInventario();
    List<Map<String, Object>> generarReporteAlertasStock();
    List<Map<String, Object>> generarReporteRotacionProductos(LocalDate fechaDesde, LocalDate fechaHasta);

    // ============ REPORTES FINANCIEROS ============
    Map<String, Object> generarReporteUtilidades(LocalDate fechaDesde, LocalDate fechaHasta);
    List<Map<String, Object>> generarReporteCompras(LocalDate fechaDesde, LocalDate fechaHasta);
    List<Map<String, Object>> generarReporteGastos(LocalDate fechaDesde, LocalDate fechaHasta);

    // ============ EXPORTACIÓN ============
    byte[] exportarReporteVentasExcel(LocalDate fechaDesde, LocalDate fechaHasta);
    byte[] exportarReporteInventarioExcel();
    byte[] exportarCierreCajaPDF(Long id);
    byte[] exportarFacturaPDF(Long ventaId);

    // ============ DASHBOARD Y ESTADÍSTICAS ============
    Map<String, Object> obtenerDashboardPrincipal();
    Map<String, Object> obtenerDashboardVentas(LocalDate fechaDesde, LocalDate fechaHasta);
    Map<String, Object> obtenerDashboardInventario();
    Map<String, Object> obtenerEstadisticasResumen(LocalDate fechaDesde, LocalDate fechaHasta);
    Map<String, Object> obtenerEstadisticasTiempoReal();
}