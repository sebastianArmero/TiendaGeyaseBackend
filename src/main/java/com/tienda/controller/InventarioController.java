package com.tienda.controller;

import com.tienda.dto.response.ApiResponse;
import com.tienda.dto.response.EstadoStockResponse;
import com.tienda.dto.response.PaginacionResponse;
import com.tienda.model.Producto;
import com.tienda.service.InventarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventario")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioService inventarioService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'INVENTARIO')")
    public ResponseEntity<ApiResponse> obtenerInventario(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nombre") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        try {
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ?
                    Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            PaginacionResponse<EstadoStockResponse> inventario =
                    inventarioService.obtenerInventarioPaginado(pageable);

            return ResponseEntity.ok(ApiResponse.success("Inventario obtenido", inventario));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/alertas")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'INVENTARIO')")
    public ResponseEntity<ApiResponse> obtenerAlertasStock() {
        try {
            List<EstadoStockResponse> alertas = inventarioService.obtenerAlertasStock();
            return ResponseEntity.ok(ApiResponse.success("Alertas de stock obtenidas", alertas));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/valoracion")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> obtenerValoracionInventario() {
        try {
            Map<String, Object> valoracion = inventarioService.obtenerValoracionInventario();
            return ResponseEntity.ok(ApiResponse.success("Valoración del inventario", valoracion));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/filtrar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'INVENTARIO')")
    public ResponseEntity<ApiResponse> filtrarInventario(
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String alertaStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Producto.AlertaStock alertaEnum = null;

            if (alertaStock != null) {
                alertaEnum = Producto.AlertaStock.valueOf(alertaStock.toUpperCase());
            }

            PaginacionResponse<EstadoStockResponse> inventario =
                    inventarioService.filtrarInventario(codigo, nombre, categoriaId, alertaEnum, pageable);

            return ResponseEntity.ok(ApiResponse.success("Inventario filtrado", inventario));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/ajustar-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'INVENTARIO')")
    public ResponseEntity<ApiResponse> ajustarStock(
            @RequestParam Long productoId,
            @RequestParam BigDecimal cantidad,
            @RequestParam String motivo,
            @RequestParam String tipoAjuste,
            @RequestParam Long usuarioId) {

        try {
            EstadoStockResponse resultado = inventarioService.ajustarStock(
                    productoId, cantidad, motivo, tipoAjuste, usuarioId);

            return ResponseEntity.ok(ApiResponse.success("Stock ajustado exitosamente", resultado));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/incrementar-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'INVENTARIO')")
    public ResponseEntity<ApiResponse> incrementarStock(
            @RequestParam Long productoId,
            @RequestParam BigDecimal cantidad,
            @RequestParam String motivo,
            @RequestParam Long usuarioId) {

        try {
            EstadoStockResponse resultado = inventarioService.incrementarStock(
                    productoId, cantidad, motivo, usuarioId);

            return ResponseEntity.ok(ApiResponse.success("Stock incrementado exitosamente", resultado));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/decrementar-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'INVENTARIO')")
    public ResponseEntity<ApiResponse> decrementarStock(
            @RequestParam Long productoId,
            @RequestParam BigDecimal cantidad,
            @RequestParam String motivo,
            @RequestParam Long usuarioId) {

        try {
            EstadoStockResponse resultado = inventarioService.decrementarStock(
                    productoId, cantidad, motivo, usuarioId);

            return ResponseEntity.ok(ApiResponse.success("Stock decrementado exitosamente", resultado));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/reservar-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'VENDEDOR')")
    public ResponseEntity<ApiResponse> reservarStock(
            @RequestParam Long productoId,
            @RequestParam BigDecimal cantidad) {

        try {
            inventarioService.reservarStock(productoId, cantidad);
            return ResponseEntity.ok(ApiResponse.success("Stock reservado exitosamente", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/liberar-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'VENDEDOR')")
    public ResponseEntity<ApiResponse> liberarStock(
            @RequestParam Long productoId,
            @RequestParam BigDecimal cantidad) {

        try {
            inventarioService.liberarStock(productoId, cantidad);
            return ResponseEntity.ok(ApiResponse.success("Stock liberado exitosamente", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/verificar-stock")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> verificarStock(
            @RequestParam Long productoId,
            @RequestParam BigDecimal cantidad) {

        try {
            boolean disponible = inventarioService.verificarStockDisponible(productoId, cantidad);
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("disponible", disponible);
            respuesta.put("productoId", productoId);
            respuesta.put("cantidadSolicitada", cantidad);

            return ResponseEntity.ok(ApiResponse.success("Verificación de stock", respuesta));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/reorden")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'INVENTARIO')")
    public ResponseEntity<ApiResponse> obtenerProductosParaReorden() {
        try {
            List<EstadoStockResponse> productos = inventarioService.obtenerProductosParaReorden();
            return ResponseEntity.ok(ApiResponse.success("Productos para reorden", productos));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/agotados")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'INVENTARIO')")
    public ResponseEntity<ApiResponse> obtenerProductosAgotados() {
        try {
            List<EstadoStockResponse> productos = inventarioService.obtenerProductosAgotados();
            return ResponseEntity.ok(ApiResponse.success("Productos agotados", productos));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/reporte")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> generarReporteStock() {
        try {
            Map<String, Object> reporte = inventarioService.generarReporteStock();
            return ResponseEntity.ok(ApiResponse.success("Reporte de inventario generado", reporte));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> obtenerDashboardInventario() {
        try {
            Map<String, Object> dashboard = new HashMap<>();

            // Valoración
            Map<String, Object> valoracion = inventarioService.obtenerValoracionInventario();
            dashboard.put("valoracion", valoracion);

            // Alertas
            List<EstadoStockResponse> alertas = inventarioService.obtenerAlertasStock();
            dashboard.put("alertas", alertas);
            dashboard.put("totalAlertas", alertas.size());

            // Productos para reorden
            List<EstadoStockResponse> reorden = inventarioService.obtenerProductosParaReorden();
            dashboard.put("productosReorden", reorden);
            dashboard.put("totalReorden", reorden.size());

            // Inventario completo paginado (primeros 10)
            Pageable pageable = PageRequest.of(0, 10);
            PaginacionResponse<EstadoStockResponse> inventario =
                    inventarioService.obtenerInventarioPaginado(pageable);
            dashboard.put("inventario", inventario);

            return ResponseEntity.ok(ApiResponse.success("Dashboard de inventario", dashboard));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}