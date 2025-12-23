package com.tienda.controller;

import com.tienda.dto.response.ApiResponse;
import com.tienda.dto.response.CierreCajaResponse;
import com.tienda.dto.response.ReporteInventarioResponse;
import com.tienda.dto.response.ReporteVentaResponse;
import com.tienda.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    // ============ CIERRE DE CAJA ============

    @PostMapping("/cierre-caja/iniciar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CAJERO')")
    public ResponseEntity<ApiResponse> iniciarCierreCaja(
            @RequestParam Long cajaId,
            @RequestParam Long usuarioId,
            @RequestParam BigDecimal saldoInicial) {

        try {
            CierreCajaResponse cierre = reporteService.iniciarCierreCaja(cajaId, usuarioId, saldoInicial);
            return ResponseEntity.ok(ApiResponse.success("Cierre de caja iniciado", cierre));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/cierre-caja/{id}/finalizar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CAJERO')")
    public ResponseEntity<ApiResponse> finalizarCierreCaja(
            @PathVariable Long id,
            @RequestParam BigDecimal saldoFinalReal,
            @RequestParam(required = false) String observaciones) {

        try {
            CierreCajaResponse cierre = reporteService.finalizarCierreCaja(id, saldoFinalReal, observaciones);
            return ResponseEntity.ok(ApiResponse.success("Cierre de caja finalizado", cierre));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/cierre-caja/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> obtenerCierreCaja(@PathVariable Long id) {
        try {
            CierreCajaResponse cierre = reporteService.obtenerCierreCajaPorId(id);
            return ResponseEntity.ok(ApiResponse.success("Cierre de caja obtenido", cierre));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/cierre-caja/hoy")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CAJERO')")
    public ResponseEntity<ApiResponse> obtenerCierreCajaHoy(@RequestParam Long cajaId) {
        try {
            CierreCajaResponse cierre = reporteService.obtenerCierreCajaHoy(cajaId);
            return ResponseEntity.ok(ApiResponse.success("Cierre de caja de hoy", cierre));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/cierre-caja")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> obtenerCierresCaja(
            @RequestParam(required = false) Long cajaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) String estado) {

        try {
            List<CierreCajaResponse> cierres = reporteService.obtenerCierresCaja(cajaId, fechaDesde, fechaHasta, estado);
            return ResponseEntity.ok(ApiResponse.success("Cierres de caja obtenidos", cierres));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/cierre-caja/{id}/conciliar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> conciliarCierreCaja(
            @PathVariable Long id,
            @RequestParam Long usuarioId,
            @RequestParam(required = false) String observaciones) {

        try {
            CierreCajaResponse cierre = reporteService.conciliarCierreCaja(id, usuarioId, observaciones);
            return ResponseEntity.ok(ApiResponse.success("Cierre de caja conciliado", cierre));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ============ REPORTES DE VENTAS ============

    @GetMapping("/ventas/diario")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> generarReporteVentasDiario(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        try {
            ReporteVentaResponse reporte = reporteService.generarReporteVentasDiario(fecha);
            return ResponseEntity.ok(ApiResponse.success("Reporte de ventas diario", reporte));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/ventas/mensual")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> generarReporteVentasMensual(
            @RequestParam int mes,
            @RequestParam int año) {

        try {
            ReporteVentaResponse reporte = reporteService.generarReporteVentasMensual(mes, año);
            return ResponseEntity.ok(ApiResponse.success("Reporte de ventas mensual", reporte));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/ventas/rango")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> generarReporteVentasRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {

        try {
            ReporteVentaResponse reporte = reporteService.generarReporteVentasRango(fechaDesde, fechaHasta);
            return ResponseEntity.ok(ApiResponse.success("Reporte de ventas por rango", reporte));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/ventas/vendedor")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> generarReporteVentasPorVendedor(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {

        try {
            List<Map<String, Object>> reporte = reporteService.generarReporteVentasPorVendedor(fechaDesde, fechaHasta);
            return ResponseEntity.ok(ApiResponse.success("Reporte de ventas por vendedor", reporte));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/ventas/producto")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> generarReporteVentasPorProducto(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {

        try {
            List<Map<String, Object>> reporte = reporteService.generarReporteVentasPorProducto(fechaDesde, fechaHasta);
            return ResponseEntity.ok(ApiResponse.success("Reporte de ventas por producto", reporte));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/ventas/cliente")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> generarReporteVentasPorCliente(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {

        try {
            List<Map<String, Object>> reporte = reporteService.generarReporteVentasPorCliente(fechaDesde, fechaHasta);
            return ResponseEntity.ok(ApiResponse.success("Reporte de ventas por cliente", reporte));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ============ REPORTES DE INVENTARIO ============

    @GetMapping("/inventario/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'INVENTARIO')")
    public ResponseEntity<ApiResponse> generarReporteInventarioEstado() {
        try {
            ReporteInventarioResponse reporte = reporteService.generarReporteInventarioEstado();
            return ResponseEntity.ok(ApiResponse.success("Reporte de estado de inventario", reporte));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/inventario/movimientos")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'INVENTARIO')")
    public ResponseEntity<ApiResponse> generarReporteMovimientosInventario(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) String tipoMovimiento) {

        try {
            List<Map<String, Object>> reporte = reporteService.generarReporteMovimientosInventario(
                    fechaDesde, fechaHasta, productoId, tipoMovimiento);
            return ResponseEntity.ok(ApiResponse.success("Reporte de movimientos de inventario", reporte));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/inventario/valoracion")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> generarReporteValoracionInventario() {
        try {
            Map<String, Object> reporte = reporteService.generarReporteValoracionInventario();
            return ResponseEntity.ok(ApiResponse.success("Reporte de valoración de inventario", reporte));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/inventario/alertas")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'INVENTARIO')")
    public ResponseEntity<ApiResponse> generarReporteAlertasStock() {
        try {
            List<Map<String, Object>> reporte = reporteService.generarReporteAlertasStock();
            return ResponseEntity.ok(ApiResponse.success("Reporte de alertas de stock", reporte));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/inventario/rotacion")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> generarReporteRotacionProductos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {

        try {
            List<Map<String, Object>> reporte = reporteService.generarReporteRotacionProductos(fechaDesde, fechaHasta);
            return ResponseEntity.ok(ApiResponse.success("Reporte de rotación de productos", reporte));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ============ REPORTES FINANCIEROS ============

    @GetMapping("/financiero/utilidades")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> generarReporteUtilidades(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {

        try {
            Map<String, Object> reporte = reporteService.generarReporteUtilidades(fechaDesde, fechaHasta);
            return ResponseEntity.ok(ApiResponse.success("Reporte de utilidades", reporte));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/financiero/compras")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> generarReporteCompras(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {

        try {
            List<Map<String, Object>> reporte = reporteService.generarReporteCompras(fechaDesde, fechaHasta);
            return ResponseEntity.ok(ApiResponse.success("Reporte de compras", reporte));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/financiero/gastos")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> generarReporteGastos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {

        try {
            List<Map<String, Object>> reporte = reporteService.generarReporteGastos(fechaDesde, fechaHasta);
            return ResponseEntity.ok(ApiResponse.success("Reporte de gastos", reporte));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ============ EXPORTACIÓN DE REPORTES ============

    @GetMapping("/exportar/ventas/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<byte[]> exportarReporteVentasExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {

        try {
            byte[] excelBytes = reporteService.exportarReporteVentasExcel(fechaDesde, fechaHasta);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .header("Content-Disposition", "attachment; filename=reporte_ventas.xlsx")
                    .body(excelBytes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/exportar/inventario/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<byte[]> exportarReporteInventarioExcel() {
        try {
            byte[] excelBytes = reporteService.exportarReporteInventarioExcel();
            return ResponseEntity.ok()
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .header("Content-Disposition", "attachment; filename=reporte_inventario.xlsx")
                    .body(excelBytes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/exportar/cierre-caja/pdf/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'CAJERO')")
    public ResponseEntity<byte[]> exportarCierreCajaPDF(@PathVariable Long id) {
        try {
            byte[] pdfBytes = reporteService.exportarCierreCajaPDF(id);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=cierre_caja_" + id + ".pdf")
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/exportar/factura/pdf/{ventaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'VENDEDOR', 'CAJERO')")
    public ResponseEntity<byte[]> exportarFacturaPDF(@PathVariable Long ventaId) {
        try {
            byte[] pdfBytes = reporteService.exportarFacturaPDF(ventaId);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=factura_" + ventaId + ".pdf")
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ============ DASHBOARD Y ESTADÍSTICAS ============

    @GetMapping("/dashboard/principal")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> obtenerDashboardPrincipal() {
        try {
            Map<String, Object> dashboard = reporteService.obtenerDashboardPrincipal();
            return ResponseEntity.ok(ApiResponse.success("Dashboard principal", dashboard));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/dashboard/ventas")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> obtenerDashboardVentas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {

        try {
            Map<String, Object> dashboard = reporteService.obtenerDashboardVentas(fechaDesde, fechaHasta);
            return ResponseEntity.ok(ApiResponse.success("Dashboard de ventas", dashboard));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/dashboard/inventario")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> obtenerDashboardInventario() {
        try {
            Map<String, Object> dashboard = reporteService.obtenerDashboardInventario();
            return ResponseEntity.ok(ApiResponse.success("Dashboard de inventario", dashboard));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/estadisticas/resumen")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> obtenerEstadisticasResumen(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {

        try {
            Map<String, Object> estadisticas = reporteService.obtenerEstadisticasResumen(fechaDesde, fechaHasta);
            return ResponseEntity.ok(ApiResponse.success("Estadísticas resumen", estadisticas));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/estadisticas/tiempo-real")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> obtenerEstadisticasTiempoReal() {
        try {
            Map<String, Object> estadisticas = reporteService.obtenerEstadisticasTiempoReal();
            return ResponseEntity.ok(ApiResponse.success("Estadísticas en tiempo real", estadisticas));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}