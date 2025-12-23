package com.tienda.controller;

import com.tienda.dto.request.CajaRequest;
import com.tienda.dto.response.CajaResponse;
import com.tienda.dto.response.PaginacionResponse;
import com.tienda.model.Caja;
import com.tienda.service.CajaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cajas")
@RequiredArgsConstructor
@Tag(name = "Cajas", description = "Controlador para gestión de cajas registradoras")
public class CajaController {

    private final CajaService cajaService;

    // ============ CRUD CAJAS ============

    @Operation(summary = "Crear nueva caja", description = "Crea una nueva caja registradora en el sistema")
    @PostMapping
    public ResponseEntity<CajaResponse> crearCaja(@Valid @RequestBody CajaRequest request) {
        CajaResponse response = cajaService.crearCaja(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Obtener caja por ID", description = "Obtiene los detalles de una caja por su ID")
    @GetMapping("/{id}")
    public ResponseEntity<CajaResponse> obtenerCajaPorId(
            @Parameter(description = "ID de la caja", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(cajaService.obtenerCajaPorId(id));
    }

    @Operation(summary = "Actualizar caja", description = "Actualiza la información de una caja existente")
    @PutMapping("/{id}")
    public ResponseEntity<CajaResponse> actualizarCaja(
            @Parameter(description = "ID de la caja", required = true) @PathVariable Long id,
            @Valid @RequestBody CajaRequest request) {
        return ResponseEntity.ok(cajaService.actualizarCaja(id, request));
    }

    @Operation(summary = "Eliminar caja", description = "Elimina una caja del sistema")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCaja(
            @Parameter(description = "ID de la caja", required = true) @PathVariable Long id) {
        cajaService.eliminarCaja(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Obtener todas las cajas", description = "Obtiene una lista de todas las cajas registradas")
    @GetMapping
    public ResponseEntity<List<CajaResponse>> obtenerTodasCajas() {
        return ResponseEntity.ok(cajaService.obtenerTodasCajas());
    }

    @Operation(summary = "Obtener cajas paginadas", description = "Obtiene cajas con paginación")
    @GetMapping("/paginadas")
    public ResponseEntity<PaginacionResponse<CajaResponse>> obtenerCajasPaginadas(
            @Parameter(description = "Configuración de paginación")
            @PageableDefault(size = 10, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(cajaService.obtenerCajasPaginadas(pageable));
    }

    @Operation(summary = "Obtener caja por código", description = "Busca una caja por su código único")
    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<CajaResponse> obtenerCajaPorCodigo(
            @Parameter(description = "Código de la caja", required = true) @PathVariable String codigo) {
        return ResponseEntity.ok(cajaService.obtenerCajaPorCodigo(codigo));
    }

    // ============ GESTIÓN DE CAJAS ============

    @Operation(summary = "Abrir caja", description = "Abre una caja para su uso diario")
    @PutMapping("/{id}/abrir")
    public ResponseEntity<CajaResponse> abrirCaja(
            @Parameter(description = "ID de la caja", required = true) @PathVariable Long id,
            @Parameter(description = "ID del usuario que abre la caja", required = true) @RequestParam Long usuarioId,
            @Parameter(description = "Saldo inicial de la caja (opcional)") @RequestParam(required = false) BigDecimal saldoInicial) {
        return ResponseEntity.ok(cajaService.abrirCaja(id, usuarioId, saldoInicial));
    }

    @Operation(summary = "Cerrar caja", description = "Cierra una caja al final del día")
    @PutMapping("/{id}/cerrar")
    public ResponseEntity<CajaResponse> cerrarCaja(
            @Parameter(description = "ID de la caja", required = true) @PathVariable Long id,
            @Parameter(description = "ID del usuario que cierra la caja", required = true) @RequestParam Long usuarioId,
            @Parameter(description = "Saldo final real contado (opcional)") @RequestParam(required = false) BigDecimal saldoFinalReal,
            @Parameter(description = "Observaciones del cierre (opcional)") @RequestParam(required = false) String observaciones) {
        return ResponseEntity.ok(cajaService.cerrarCaja(id, usuarioId, saldoFinalReal, observaciones));
    }

    @Operation(summary = "Asignar usuario a caja", description = "Asigna un usuario a una caja específica")
    @PutMapping("/{id}/asignar-usuario")
    public ResponseEntity<CajaResponse> asignarUsuario(
            @Parameter(description = "ID de la caja", required = true) @PathVariable Long id,
            @Parameter(description = "ID del usuario a asignar", required = true) @RequestParam Long usuarioId) {
        return ResponseEntity.ok(cajaService.asignarUsuario(id, usuarioId));
    }

    @Operation(summary = "Desasignar usuario de caja", description = "Remueve el usuario asignado de una caja")
    @PutMapping("/{id}/desasignar-usuario")
    public ResponseEntity<CajaResponse> desasignarUsuario(
            @Parameter(description = "ID de la caja", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(cajaService.desasignarUsuario(id));
    }

    @Operation(summary = "Bloquear caja", description = "Bloquea una caja por mantenimiento o auditoría")
    @PutMapping("/{id}/bloquear")
    public ResponseEntity<CajaResponse> bloquearCaja(
            @Parameter(description = "ID de la caja", required = true) @PathVariable Long id,
            @Parameter(description = "Motivo del bloqueo", required = true) @RequestParam String motivo) {
        return ResponseEntity.ok(cajaService.bloquearCaja(id, motivo));
    }

    @Operation(summary = "Desbloquear caja", description = "Desbloquea una caja previamente bloqueada")
    @PutMapping("/{id}/desbloquear")
    public ResponseEntity<CajaResponse> desbloquearCaja(
            @Parameter(description = "ID de la caja", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(cajaService.desbloquearCaja(id));
    }

    // ============ CONSULTAS DE CAJAS ============

    @Operation(summary = "Obtener cajas por sucursal", description = "Obtiene todas las cajas de una sucursal específica")
    @GetMapping("/sucursal/{sucursalId}")
    public ResponseEntity<List<CajaResponse>> obtenerCajasPorSucursal(
            @Parameter(description = "ID de la sucursal", required = true) @PathVariable Long sucursalId) {
        return ResponseEntity.ok(cajaService.obtenerCajasPorSucursal(sucursalId));
    }

    @Operation(summary = "Obtener cajas por estado", description = "Filtra cajas por su estado actual")
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<CajaResponse>> obtenerCajasPorEstado(
            @Parameter(description = "Estado de la caja (ABIERTA, CERRADA, BLOQUEADA, EN_AUDITORIA)", required = true)
            @PathVariable Caja.EstadoCaja estado) {
        return ResponseEntity.ok(cajaService.obtenerCajasPorEstado(estado));
    }

    @Operation(summary = "Obtener cajas abiertas", description = "Obtiene todas las cajas que están actualmente abiertas")
    @GetMapping("/abiertas")
    public ResponseEntity<List<CajaResponse>> obtenerCajasAbiertas() {
        return ResponseEntity.ok(cajaService.obtenerCajasAbiertas());
    }

    @Operation(summary = "Obtener cajas disponibles", description = "Obtiene cajas cerradas y disponibles para uso")
    @GetMapping("/disponibles")
    public ResponseEntity<List<CajaResponse>> obtenerCajasDisponibles() {
        return ResponseEntity.ok(cajaService.obtenerCajasDisponibles());
    }

    @Operation(summary = "Obtener caja abierta por usuario", description = "Obtiene la caja que un usuario tiene actualmente abierta")
    @GetMapping("/usuario/{usuarioId}/abierta")
    public ResponseEntity<CajaResponse> obtenerCajaAbiertaPorUsuario(
            @Parameter(description = "ID del usuario", required = true) @PathVariable Long usuarioId) {
        return ResponseEntity.ok(cajaService.obtenerCajaAbiertaPorUsuario(usuarioId));
    }

    // ============ REPORTES Y ESTADÍSTICAS ============

    @Operation(summary = "Obtener estado de cajas", description = "Obtiene estadísticas del estado actual de todas las cajas")
    @GetMapping("/estado")
    public ResponseEntity<Map<String, Object>> obtenerEstadoCajas() {
        return ResponseEntity.ok(cajaService.obtenerEstadoCajas());
    }

    @Operation(summary = "Generar reporte de cierre diario", description = "Genera un reporte detallado del cierre de caja de un día específico")
    @GetMapping("/{id}/reporte-diario")
    public ResponseEntity<Map<String, Object>> generarReporteCierreDiario(
            @Parameter(description = "ID de la caja", required = true) @PathVariable Long id,
            @Parameter(description = "Fecha del reporte", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(cajaService.generarReporteCierreDiario(id, fecha));
    }

    @Operation(summary = "Generar reporte mensual", description = "Genera un reporte consolidado mensual de todas las cajas")
    @GetMapping("/reporte-mensual")
    public ResponseEntity<Map<String, Object>> generarReporteMensual(
            @Parameter(description = "Mes del reporte (1-12, opcional)") @RequestParam(required = false) Integer mes,
            @Parameter(description = "Año del reporte (opcional)") @RequestParam(required = false) Integer año) {
        return ResponseEntity.ok(cajaService.generarReporteMensual(mes, año));
    }

    @Operation(summary = "Obtener estadísticas de cajas", description = "Obtiene estadísticas históricas de todas las cajas")
    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticasCajas() {
        return ResponseEntity.ok(cajaService.obtenerEstadisticasCajas());
    }

    // ============ VALIDACIONES ============

    @Operation(summary = "Verificar existencia por código", description = "Verifica si existe una caja con el código especificado")
    @GetMapping("/existe-codigo")
    public ResponseEntity<Boolean> existeCajaPorCodigo(
            @Parameter(description = "Código a verificar", required = true) @RequestParam String codigo) {
        return ResponseEntity.ok(cajaService.existeCajaPorCodigo(codigo));
    }

    @Operation(summary = "Verificar si caja está abierta", description = "Verifica si una caja específica está abierta")
    @GetMapping("/{id}/verificar-abierta")
    public ResponseEntity<Boolean> verificarCajaAbierta(
            @Parameter(description = "ID de la caja", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(cajaService.verificarCajaAbierta(id));
    }

    @Operation(summary = "Verificar si usuario tiene caja abierta", description = "Verifica si un usuario tiene alguna caja abierta")
    @GetMapping("/usuario/{usuarioId}/verificar-caja-abierta")
    public ResponseEntity<Boolean> verificarUsuarioTieneCajaAbierta(
            @Parameter(description = "ID del usuario", required = true) @PathVariable Long usuarioId) {
        return ResponseEntity.ok(cajaService.verificarUsuarioTieneCajaAbierta(usuarioId));
    }

    // ============ MÉTODOS INTERNOS (para otros servicios) ============

    @Operation(summary = "Actualizar saldo de caja", description = "Actualiza el saldo de una caja (uso interno)")
    @PutMapping("/{id}/actualizar-saldo")
    public ResponseEntity<Void> actualizarSaldoCaja(
            @Parameter(description = "ID de la caja", required = true) @PathVariable Long id,
            @Parameter(description = "Monto a actualizar", required = true) @RequestParam BigDecimal monto,
            @Parameter(description = "Tipo de operación (VENTA, EGRESO)", required = true) @RequestParam String tipoOperacion) {
        cajaService.actualizarSaldoCaja(id, monto, tipoOperacion);
        return ResponseEntity.ok().build();
    }
}