package com.tienda.controller;

import com.tienda.dto.request.VentaRequest;
import com.tienda.dto.response.ApiResponse;
import com.tienda.dto.response.PaginacionResponse;
import com.tienda.dto.response.VentaResponse;
import com.tienda.service.VentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'VENDEDOR', 'CAJERO')")
    public ResponseEntity<ApiResponse> crearVenta(@Valid @RequestBody VentaRequest request) {
        try {
            VentaResponse venta = ventaService.crearVenta(request);
            return ResponseEntity.ok(ApiResponse.success("Venta realizada exitosamente", venta));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'VENDEDOR', 'CAJERO')")
    public ResponseEntity<ApiResponse> obtenerVenta(@PathVariable Long id) {
        try {
            VentaResponse venta = ventaService.obtenerVentaPorId(id);
            return ResponseEntity.ok(ApiResponse.success("Venta obtenida", venta));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'VENDEDOR')")
    public ResponseEntity<ApiResponse> obtenerVentas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fechaEmision") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        try {
            Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ?
                    Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            PaginacionResponse<VentaResponse> ventas = ventaService.obtenerVentasPaginadas(pageable);
            return ResponseEntity.ok(ApiResponse.success("Ventas obtenidas", ventas));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/filtrar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'VENDEDOR')")
    public ResponseEntity<ApiResponse> filtrarVentas(
            @RequestParam(required = false) String numeroFactura,
            @RequestParam(required = false) String clienteNombre,
            @RequestParam(required = false) Long vendedorId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) LocalDate fechaDesde,
            @RequestParam(required = false) LocalDate fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);

            PaginacionResponse<VentaResponse> ventas = ventaService.filtrarVentas(
                    numeroFactura, clienteNombre, vendedorId, estado, fechaDesde, fechaHasta, pageable);

            return ResponseEntity.ok(ApiResponse.success("Ventas filtradas", ventas));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> anularVenta(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {  // ✅ Cambiado a Map

        try {
            String motivo = request.get("motivo");
            if (motivo == null || motivo.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("El motivo de anulación es obligatorio"));
            }

            ventaService.anularVenta(id, motivo);
            return ResponseEntity.ok(ApiResponse.success("Venta anulada exitosamente", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/hoy")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'VENDEDOR', 'CAJERO')")
    public ResponseEntity<ApiResponse> obtenerVentasHoy() {
        try {
            List<VentaResponse> ventas = ventaService.obtenerVentasDelDia();
            return ResponseEntity.ok(ApiResponse.success("Ventas de hoy", ventas));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/cliente/{clienteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'VENDEDOR')")
    public ResponseEntity<ApiResponse> obtenerVentasPorCliente(@PathVariable Long clienteId) {
        try {
            List<VentaResponse> ventas = ventaService.obtenerVentasPorCliente(clienteId);
            return ResponseEntity.ok(ApiResponse.success("Ventas del cliente", ventas));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/estadisticas/diarias")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> obtenerEstadisticasDiarias(
            @RequestParam(required = false) String fecha) {  // ✅ Cambiado a String

        try {
            LocalDate fechaConsulta;
            if (fecha != null) {
                fechaConsulta = LocalDate.parse(fecha);
            } else {
                fechaConsulta = LocalDate.now();
            }

            Map<String, Object> estadisticas = ventaService.obtenerEstadisticasDiarias(fechaConsulta);
            return ResponseEntity.ok(ApiResponse.success("Estadísticas diarias", estadisticas));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/estadisticas/mensuales")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> obtenerEstadisticasMensuales(
            @RequestParam int mes,
            @RequestParam int año) {
        try {
            Map<String, Object> estadisticas = ventaService.obtenerEstadisticasMensuales(mes, año);
            return ResponseEntity.ok(ApiResponse.success("Estadísticas mensuales", estadisticas));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/factura/{numeroFactura}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'VENDEDOR', 'CAJERO')")
    public ResponseEntity<ApiResponse> obtenerVentaPorFactura(@PathVariable String numeroFactura) {
        try {
            VentaResponse venta = ventaService.obtenerVentaPorNumeroFactura(numeroFactura);
            return ResponseEntity.ok(ApiResponse.success("Venta obtenida", venta));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/generar-factura")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'VENDEDOR', 'CAJERO')")
    public ResponseEntity<ApiResponse> generarNumeroFactura() {
        try {
            String numeroFactura = ventaService.generarNumeroFactura();
            Map<String, String> respuesta = new HashMap<>();
            respuesta.put("numeroFactura", numeroFactura);

            return ResponseEntity.ok(ApiResponse.success("Número de factura generado", respuesta));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/top-productos")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> obtenerTopProductos(
            @RequestParam(defaultValue = "10") int limite) {
        try {
            List<Map<String, Object>> topProductos = ventaService.obtenerTopProductosVendidos(limite);
            return ResponseEntity.ok(ApiResponse.success("Top productos vendidos", topProductos));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> obtenerDashboardVentas() {
        try {
            Map<String, Object> dashboard = ventaService.obtenerDashboardVentas();
            return ResponseEntity.ok(ApiResponse.success("Dashboard de ventas", dashboard));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}