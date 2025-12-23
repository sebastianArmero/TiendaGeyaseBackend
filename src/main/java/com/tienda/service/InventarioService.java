package com.tienda.service;

import com.tienda.dto.response.EstadoStockResponse;
import com.tienda.dto.response.PaginacionResponse;
import com.tienda.model.Producto;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface InventarioService {

    // Consultas de inventario
    List<EstadoStockResponse> obtenerInventarioConEstados();
    PaginacionResponse<EstadoStockResponse> obtenerInventarioPaginado(Pageable pageable);
    List<EstadoStockResponse> obtenerAlertasStock();
    Map<String, Object> obtenerValoracionInventario();

    // Búsquedas filtradas
    PaginacionResponse<EstadoStockResponse> filtrarInventario(
            String codigo, String nombre, Long categoriaId,
            Producto.AlertaStock alertaStock, Pageable pageable);

    // Ajustes de stock
    EstadoStockResponse ajustarStock(Long productoId, BigDecimal cantidad,
                                     String motivo, String tipoAjuste, Long usuarioId);
    EstadoStockResponse incrementarStock(Long productoId, BigDecimal cantidad,
                                         String motivo, Long usuarioId);
    EstadoStockResponse decrementarStock(Long productoId, BigDecimal cantidad,
                                         String motivo, Long usuarioId);

    // Gestión de stock
    void reservarStock(Long productoId, BigDecimal cantidad);
    void liberarStock(Long productoId, BigDecimal cantidad);
    boolean verificarStockDisponible(Long productoId, BigDecimal cantidad);

    // Reportes
    List<EstadoStockResponse> obtenerProductosParaReorden();
    List<EstadoStockResponse> obtenerProductosAgotados();
    Map<String, Object> generarReporteStock();
}