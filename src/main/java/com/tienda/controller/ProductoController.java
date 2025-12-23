package com.tienda.controller;

import com.tienda.dto.request.ProductoRequest;
import com.tienda.dto.response.ApiResponse;
import com.tienda.dto.response.PaginacionResponse;
import com.tienda.dto.response.ProductoResponse;
import com.tienda.model.Producto;
import com.tienda.service.ProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'INVENTARIO')")
    public ResponseEntity<ApiResponse> crearProducto(@Valid @RequestBody ProductoRequest request) {
        try {
            ProductoResponse producto = productoService.crearProducto(request);
            return ResponseEntity.ok(ApiResponse.success("Producto creado exitosamente", producto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'INVENTARIO')")
    public ResponseEntity<ApiResponse> actualizarProducto(
            @PathVariable Long id,
            @Valid @RequestBody ProductoRequest request) {
        try {
            ProductoResponse producto = productoService.actualizarProducto(id, request);
            return ResponseEntity.ok(ApiResponse.success("Producto actualizado exitosamente", producto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> eliminarProducto(@PathVariable Long id) {
        try {
            productoService.eliminarProducto(id);
            return ResponseEntity.ok(ApiResponse.success("Producto eliminado exitosamente", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> obtenerProducto(@PathVariable Long id) {
        try {
            ProductoResponse producto = productoService.obtenerProductoPorId(id);
            return ResponseEntity.ok(ApiResponse.success("Producto obtenido", producto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/codigo/{codigo}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> obtenerProductoPorCodigo(@PathVariable String codigo) {
        try {
            ProductoResponse producto = productoService.obtenerProductoPorCodigo(codigo);
            return ResponseEntity.ok(ApiResponse.success("Producto obtenido", producto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> obtenerTodosProductos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nombre") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        try {
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ?
                    Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            PaginacionResponse<ProductoResponse> productos =
                    productoService.obtenerProductosPaginados(pageable);

            return ResponseEntity.ok(ApiResponse.success("Productos obtenidos", productos));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/buscar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> buscarProductos(
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Producto.EstadoProducto estadoEnum = null;

            if (estado != null) {
                estadoEnum = Producto.EstadoProducto.valueOf(estado.toUpperCase());
            }

            PaginacionResponse<ProductoResponse> productos =
                    productoService.filtrarProductos(codigo, nombre, categoriaId, estadoEnum, pageable);

            return ResponseEntity.ok(ApiResponse.success("Productos filtrados", productos));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/ajustar-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'INVENTARIO')")
    public ResponseEntity<ApiResponse> ajustarStock(
            @PathVariable Long id,
            @RequestParam BigDecimal cantidad,
            @RequestParam String motivo) {
        try {
            ProductoResponse producto = productoService.ajustarStock(id, cantidad, motivo);
            return ResponseEntity.ok(ApiResponse.success("Stock ajustado exitosamente", producto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/incrementar-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'INVENTARIO')")
    public ResponseEntity<ApiResponse> incrementarStock(
            @PathVariable Long id,
            @RequestParam BigDecimal cantidad,
            @RequestParam String motivo) {
        try {
            ProductoResponse producto = productoService.incrementarStock(id, cantidad, motivo);
            return ResponseEntity.ok(ApiResponse.success("Stock incrementado exitosamente", producto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/decrementar-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'INVENTARIO')")
    public ResponseEntity<ApiResponse> decrementarStock(
            @PathVariable Long id,
            @RequestParam BigDecimal cantidad,
            @RequestParam String motivo) {
        try {
            ProductoResponse producto = productoService.decrementarStock(id, cantidad, motivo);
            return ResponseEntity.ok(ApiResponse.success("Stock decrementado exitosamente", producto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/actualizar-precio")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> actualizarPrecio(
            @PathVariable Long id,
            @RequestParam BigDecimal nuevoPrecio) {
        try {
            ProductoResponse producto = productoService.actualizarPrecio(id, nuevoPrecio);
            return ResponseEntity.ok(ApiResponse.success("Precio actualizado exitosamente", producto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/importar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> importarProductos(@RequestParam("archivo") MultipartFile archivo) {
        try {
            Map<String, Object> resultado = productoService.importarProductosDesdeExcel(archivo);
            return ResponseEntity.ok(ApiResponse.success("Importación iniciada", resultado));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/exportar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<byte[]> exportarProductos() {
        try {
            byte[] excelBytes = productoService.exportarProductosAExcel();
            return ResponseEntity.ok()
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .header("Content-Disposition", "attachment; filename=productos.xlsx")
                    .body(excelBytes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse> obtenerEstadisticas() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalProductos", productoService.contarProductosActivos());
            stats.put("productosBajoStock", productoService.contarProductosBajoStock());

            return ResponseEntity.ok(ApiResponse.success("Estadísticas obtenidas", stats));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}