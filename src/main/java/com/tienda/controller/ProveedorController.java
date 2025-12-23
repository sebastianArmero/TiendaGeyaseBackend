package com.tienda.controller;

import com.tienda.dto.request.ProveedorRequest;
import com.tienda.dto.response.PaginacionResponse;
import com.tienda.dto.response.ProveedorResponse;
import com.tienda.model.Proveedor;
import com.tienda.service.ProveedorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
@Tag(name = "Proveedores", description = "Controlador para gestión de proveedores")
public class ProveedorController {

    private final ProveedorService proveedorService;

    @Operation(summary = "Crear nuevo proveedor")
    @PostMapping
    public ResponseEntity<ProveedorResponse> crearProveedor(@Valid @RequestBody ProveedorRequest request) {
        ProveedorResponse response = proveedorService.crearProveedor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Obtener proveedor por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ProveedorResponse> obtenerProveedorPorId(@PathVariable Long id) {
        return ResponseEntity.ok(proveedorService.obtenerProveedorPorId(id));
    }

    @Operation(summary = "Actualizar proveedor")
    @PutMapping("/{id}")
    public ResponseEntity<ProveedorResponse> actualizarProveedor(
            @PathVariable Long id,
            @Valid @RequestBody ProveedorRequest request) {
        return ResponseEntity.ok(proveedorService.actualizarProveedor(id, request));
    }

    @Operation(summary = "Eliminar proveedor (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProveedor(@PathVariable Long id) {
        proveedorService.eliminarProveedor(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Obtener todos los proveedores")
    @GetMapping
    public ResponseEntity<List<ProveedorResponse>> obtenerTodosProveedores() {
        return ResponseEntity.ok(proveedorService.obtenerTodosProveedores());
    }

    @Operation(summary = "Obtener proveedores paginados")
    @GetMapping("/paginados")
    public ResponseEntity<PaginacionResponse<ProveedorResponse>> obtenerProveedoresPaginados(
            @PageableDefault(size = 10, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(proveedorService.obtenerProveedoresPaginados(pageable));
    }

    @Operation(summary = "Buscar proveedores por nombre")
    @GetMapping("/buscar")
    public ResponseEntity<List<ProveedorResponse>> buscarProveedoresPorNombre(
            @RequestParam String nombre) {
        return ResponseEntity.ok(proveedorService.buscarProveedoresPorNombre(nombre));
    }

    @Operation(summary = "Obtener proveedor por código")
    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<ProveedorResponse> obtenerProveedorPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(proveedorService.obtenerProveedorPorCodigo(codigo));
    }

    @Operation(summary = "Obtener proveedor por RUC")
    @GetMapping("/ruc/{ruc}")
    public ResponseEntity<ProveedorResponse> obtenerProveedorPorRuc(@PathVariable String ruc) {
        return ResponseEntity.ok(proveedorService.obtenerProveedorPorRuc(ruc));
    }

    @Operation(summary = "Obtener proveedores por estado")
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<ProveedorResponse>> obtenerProveedoresPorEstado(
            @PathVariable Proveedor.EstadoProveedor estado) {
        return ResponseEntity.ok(proveedorService.obtenerProveedoresPorEstado(estado));
    }

    @Operation(summary = "Activar proveedor")
    @PutMapping("/{id}/activar")
    public ResponseEntity<Void> activarProveedor(@PathVariable Long id) {
        proveedorService.activarProveedor(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Desactivar proveedor")
    @PutMapping("/{id}/desactivar")
    public ResponseEntity<Void> desactivarProveedor(@PathVariable Long id) {
        proveedorService.desactivarProveedor(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Suspender proveedor")
    @PutMapping("/{id}/suspender")
    public ResponseEntity<Void> suspenderProveedor(
            @PathVariable Long id,
            @RequestParam String motivo) {
        proveedorService.suspenderProveedor(id, motivo);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Contar proveedores activos")
    @GetMapping("/contar-activos")
    public ResponseEntity<Long> contarProveedoresActivos() {
        return ResponseEntity.ok(proveedorService.contarProveedoresActivos());
    }

    @Operation(summary = "Obtener estadísticas de proveedores")
    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticasProveedores() {
        return ResponseEntity.ok(proveedorService.obtenerEstadisticasProveedores());
    }

    @Operation(summary = "Verificar si existe proveedor por código")
    @GetMapping("/existe-codigo")
    public ResponseEntity<Boolean> existeProveedorPorCodigo(@RequestParam String codigo) {
        return ResponseEntity.ok(proveedorService.existeProveedorPorCodigo(codigo));
    }

    @Operation(summary = "Verificar si existe proveedor por RUC")
    @GetMapping("/existe-ruc")
    public ResponseEntity<Boolean> existeProveedorPorRuc(@RequestParam String ruc) {
        return ResponseEntity.ok(proveedorService.existeProveedorPorRuc(ruc));
    }
}