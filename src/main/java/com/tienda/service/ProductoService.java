package com.tienda.service;

import com.tienda.dto.request.ProductoRequest;
import com.tienda.dto.response.ProductoResponse;
import com.tienda.dto.response.PaginacionResponse;
import com.tienda.model.Producto;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ProductoService {

    // CRUD
    ProductoResponse crearProducto(ProductoRequest request);
    ProductoResponse actualizarProducto(Long id, ProductoRequest request);
    void eliminarProducto(Long id);
    ProductoResponse obtenerProductoPorId(Long id);
    ProductoResponse obtenerProductoPorCodigo(String codigo);
    List<ProductoResponse> obtenerTodosProductos();
    PaginacionResponse<ProductoResponse> obtenerProductosPaginados(Pageable pageable);

    // Búsquedas y filtros
    List<ProductoResponse> buscarProductosPorNombre(String nombre);
    List<ProductoResponse> buscarProductosPorCodigo(String codigo);
    PaginacionResponse<ProductoResponse> filtrarProductos(
            String codigo, String nombre, Long categoriaId,
            Producto.EstadoProducto estado, Pageable pageable);

    // Gestión de stock
    ProductoResponse ajustarStock(Long productoId, BigDecimal cantidad, String motivo);
    ProductoResponse incrementarStock(Long productoId, BigDecimal cantidad, String motivo);
    ProductoResponse decrementarStock(Long productoId, BigDecimal cantidad, String motivo);
    ProductoResponse actualizarPrecio(Long productoId, BigDecimal nuevoPrecio);

    // Importación/Exportación
    Map<String, Object> importarProductosDesdeExcel(MultipartFile archivo);
    byte[] exportarProductosAExcel();

    // Validaciones
    boolean existeProductoPorCodigo(String codigo);
    boolean existeProductoPorCodigoBarras(String codigoBarras);

    // Métodos internos
    Producto obtenerEntidadProducto(Long id);
    boolean verificarStockDisponible(Long productoId, BigDecimal cantidad);

    // Para dashboard
    Long contarProductosActivos();
    Long contarProductosBajoStock();
}