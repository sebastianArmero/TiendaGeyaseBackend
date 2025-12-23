package com.tienda.controller;

import com.tienda.dto.response.DashboardResponse;
import com.tienda.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Controlador para métricas y estadísticas del dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Obtener métricas principales")
    @GetMapping("/metricas-principales")
    public ResponseEntity<DashboardResponse> obtenerMetricasPrincipales() {
        return ResponseEntity.ok(dashboardService.obtenerMetricasPrincipales());
    }

    @Operation(summary = "Obtener métricas principales por fecha")
    @GetMapping("/metricas-principales/fecha")
    public ResponseEntity<DashboardResponse> obtenerMetricasPrincipalesPorFecha(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(dashboardService.obtenerMetricasPrincipalesPorFecha(fechaInicio, fechaFin));
    }

    @Operation(summary = "Obtener métricas de ventas")
    @GetMapping("/metricas-ventas")
    public ResponseEntity<Map<String, Object>> obtenerMetricasVentas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(dashboardService.obtenerMetricasVentas(fechaInicio, fechaFin));
    }

    @Operation(summary = "Obtener métricas de inventario")
    @GetMapping("/metricas-inventario")
    public ResponseEntity<Map<String, Object>> obtenerMetricasInventario() {
        return ResponseEntity.ok(dashboardService.obtenerMetricasInventario());
    }

    @Operation(summary = "Obtener métricas de clientes")
    @GetMapping("/metricas-clientes")
    public ResponseEntity<Map<String, Object>> obtenerMetricasClientes() {
        return ResponseEntity.ok(dashboardService.obtenerMetricasClientes());
    }

    @Operation(summary = "Obtener métricas financieras")
    @GetMapping("/metricas-financieras")
    public ResponseEntity<Map<String, Object>> obtenerMetricasFinancieras(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(dashboardService.obtenerMetricasFinancieras(fechaInicio, fechaFin));
    }

    @Operation(summary = "Obtener ventas por periodo")
    @GetMapping("/ventas-periodo")
    public ResponseEntity<Map<String, Object>> obtenerVentasPorPeriodo(
            @RequestParam String periodo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(dashboardService.obtenerVentasPorPeriodo(periodo, fechaInicio, fechaFin));
    }

    @Operation(summary = "Obtener tendencia de ventas")
    @GetMapping("/tendencia-ventas")
    public ResponseEntity<Map<String, Object>> obtenerTendenciaVentas(
            @RequestParam(defaultValue = "7") int dias) {
        return ResponseEntity.ok(dashboardService.obtenerTendenciaVentas(dias));
    }

    @Operation(summary = "Obtener top productos")
    @GetMapping("/top-productos")
    public ResponseEntity<Map<String, Object>> obtenerTopProductos(
            @RequestParam(defaultValue = "10") int limite) {
        return ResponseEntity.ok(dashboardService.obtenerTopProductos(limite));
    }

    @Operation(summary = "Obtener top clientes")
    @GetMapping("/top-clientes")
    public ResponseEntity<Map<String, Object>> obtenerTopClientes(
            @RequestParam(defaultValue = "10") int limite) {
        return ResponseEntity.ok(dashboardService.obtenerTopClientes(limite));
    }

    @Operation(summary = "Obtener top vendedores")
    @GetMapping("/top-vendedores")
    public ResponseEntity<Map<String, Object>> obtenerTopVendedores(
            @RequestParam(defaultValue = "5") int limite) {
        return ResponseEntity.ok(dashboardService.obtenerTopVendedores(limite));
    }

    @Operation(summary = "Obtener productos bajo stock")
    @GetMapping("/productos-bajo-stock")
    public ResponseEntity<Map<String, Object>> obtenerProductosBajoStock(
            @RequestParam(defaultValue = "10") int limite) {
        return ResponseEntity.ok(dashboardService.obtenerProductosBajoStock(limite));
    }

    @Operation(summary = "Obtener métricas en tiempo real")
    @GetMapping("/tiempo-real")
    public ResponseEntity<Map<String, Object>> obtenerMetricasTiempoReal() {
        return ResponseEntity.ok(dashboardService.obtenerMetricasTiempoReal());
    }

    @Operation(summary = "Obtener comparativa de periodos")
    @GetMapping("/comparativa")
    public ResponseEntity<Map<String, Object>> obtenerComparativaPeriodos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodoActualInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodoActualFin,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodoAnteriorInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodoAnteriorFin) {
        return ResponseEntity.ok(dashboardService.obtenerComparativaPeriodos(
                periodoActualInicio, periodoActualFin, periodoAnteriorInicio, periodoAnteriorFin));
    }

    @Operation(summary = "Obtener alertas del sistema")
    @GetMapping("/alertas")
    public ResponseEntity<Map<String, Object>> obtenerAlertasSistema() {
        return ResponseEntity.ok(dashboardService.obtenerAlertasSistema());
    }

    @Operation(summary = "Obtener widgets de métricas de ventas")
    @GetMapping("/widgets/ventas")
    public ResponseEntity<Map<String, Object>> obtenerWidgetMetricasVentas() {
        return ResponseEntity.ok(dashboardService.obtenerWidgetMetricasVentas());
    }

    @Operation(summary = "Obtener widgets de métricas de inventario")
    @GetMapping("/widgets/inventario")
    public ResponseEntity<Map<String, Object>> obtenerWidgetMetricasInventario() {
        return ResponseEntity.ok(dashboardService.obtenerWidgetMetricasInventario());
    }

    @Operation(summary = "Obtener widgets de métricas de clientes")
    @GetMapping("/widgets/clientes")
    public ResponseEntity<Map<String, Object>> obtenerWidgetMetricasClientes() {
        return ResponseEntity.ok(dashboardService.obtenerWidgetMetricasClientes());
    }
}