package com.tienda.controller;

import com.tienda.dto.request.CierreCajaRequest;
import com.tienda.dto.response.CierreCajaResponse;
import com.tienda.service.CajaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cierres-caja")
@RequiredArgsConstructor
@Tag(name = "Cierres de Caja", description = "Controlador para gestión de cierres de caja")
public class CierreCajaController {

    private final CajaService cajaService;

    // ============ CRUD CIERRES ============

    @Operation(summary = "Realizar cierre diario", description = "Realiza el cierre diario de una caja")
    @PostMapping
    public ResponseEntity<CierreCajaResponse> realizarCierreDiario(@Valid @RequestBody CierreCajaRequest request) {
        CierreCajaResponse response = cajaService.realizarCierreDiario(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Obtener cierre por ID", description = "Obtiene los detalles de un cierre de caja por su ID")
    @GetMapping("/{id}")
    public ResponseEntity<CierreCajaResponse> obtenerCierrePorId(
            @Parameter(description = "ID del cierre de caja", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(cajaService.obtenerCierrePorId(id));
    }

    @Operation(summary = "Obtener cierre por caja y fecha", description = "Obtiene el cierre de caja de una fecha específica")
    @GetMapping("/caja/{cajaId}/fecha")
    public ResponseEntity<CierreCajaResponse> obtenerCierrePorCajaYFecha(
            @Parameter(description = "ID de la caja", required = true) @PathVariable Long cajaId,
            @Parameter(description = "Fecha del cierre", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(cajaService.obtenerCierrePorCajaYFecha(cajaId, fecha));
    }

    @Operation(summary = "Obtener cierres por caja", description = "Obtiene todos los cierres de una caja específica")
    @GetMapping("/caja/{cajaId}")
    public ResponseEntity<List<CierreCajaResponse>> obtenerCierresPorCaja(
            @Parameter(description = "ID de la caja", required = true) @PathVariable Long cajaId) {
        return ResponseEntity.ok(cajaService.obtenerCierresPorCaja(cajaId));
    }

    @Operation(summary = "Obtener cierres por fecha", description = "Obtiene todos los cierres de una fecha específica")
    @GetMapping("/fecha")
    public ResponseEntity<List<CierreCajaResponse>> obtenerCierresPorFecha(
            @Parameter(description = "Fecha de los cierres", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(cajaService.obtenerCierresPorFecha(fecha));
    }

    @Operation(summary = "Obtener cierres pendientes", description = "Obtiene todos los cierres de caja pendientes de conciliación")
    @GetMapping("/pendientes")
    public ResponseEntity<List<CierreCajaResponse>> obtenerCierresPendientes() {
        return ResponseEntity.ok(cajaService.obtenerCierresPendientes());
    }

    // ============ CONCILIACIÓN ============

    @Operation(summary = "Conciliar cierre", description = "Concilia un cierre de caja pendiente")
    @PutMapping("/{id}/conciliar")
    public ResponseEntity<CierreCajaResponse> conciliarCierre(
            @Parameter(description = "ID del cierre de caja", required = true) @PathVariable Long id,
            @Parameter(description = "ID del usuario que concilia", required = true) @RequestParam Long usuarioId,
            @Parameter(description = "Observaciones de la conciliación (opcional)") @RequestParam(required = false) String observaciones) {
        return ResponseEntity.ok(cajaService.conciliarCierre(id, usuarioId, observaciones));
    }

    @Operation(summary = "Aprobar cierre", description = "Aprueba un cierre de caja conciliado")
    @PutMapping("/{id}/aprobar")
    public ResponseEntity<CierreCajaResponse> aprobarCierre(
            @Parameter(description = "ID del cierre de caja", required = true) @PathVariable Long id,
            @Parameter(description = "ID del usuario que aprueba", required = true) @RequestParam Long usuarioId) {
        return ResponseEntity.ok(cajaService.aprobarCierre(id, usuarioId));
    }

    @Operation(summary = "Rechazar cierre", description = "Rechaza un cierre de caja por inconsistencias")
    @PutMapping("/{id}/rechazar")
    public ResponseEntity<CierreCajaResponse> rechazarCierre(
            @Parameter(description = "ID del cierre de caja", required = true) @PathVariable Long id,
            @Parameter(description = "ID del usuario que rechaza", required = true) @RequestParam Long usuarioId,
            @Parameter(description = "Motivo del rechazo", required = true) @RequestParam String motivo) {
        return ResponseEntity.ok(cajaService.rechazarCierre(id, usuarioId, motivo));
    }
}