package com.tienda.service;

import com.tienda.dto.request.AnularVentaRequest;
import com.tienda.dto.request.VentaRequest;
import com.tienda.dto.response.*;
import com.tienda.model.Venta;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface VentaService {

    // CRUD de ventas
    VentaResponse crearVenta(VentaRequest request, Long vendedorId);
    VentaResponse anularVenta(Long ventaId, AnularVentaRequest request, Long usuarioId);
    VentaResponse obtenerVentaPorId(Long id);
    VentaResponse obtenerVentaPorFactura(String numeroFactura);
    List<VentaResponse> obtenerTodasVentas();
    PaginacionResponse<VentaResponse> obtenerVentasPaginadas(Pageable pageable);

    // Búsquedas y filtros
    PaginacionResponse<VentaResponse> filtrarVentas(
            String numeroFactura, String clienteNombre, Long vendedorId,
            String estado, LocalDateTime fechaDesde, LocalDateTime fechaHasta,
            Pageable pageable);

    List<VentaResponse> obtenerVentasPorCliente(Long clienteId);
    List<VentaResponse> obtenerVentasPorVendedor(Long vendedorId);
    List<VentaResponse> obtenerVentasPorFecha(LocalDate fecha);
    List<VentaResponse> obtenerVentasPorRangoFechas(LocalDate fechaInicio, LocalDate fechaFin);

    // Carrito y proceso de venta
    CarritoResponse agregarAlCarrito(Long productoId, BigDecimal cantidad);
    CarritoResponse obtenerCarrito();
    void limpiarCarrito();
    void removerDelCarrito(Long productoId);
    CarritoResponse actualizarCantidadCarrito(Long productoId, BigDecimal cantidad);

    // Validaciones
    boolean verificarStockCarrito();
    Map<String, Object> validarVenta(VentaRequest request);

    // Métodos de negocio
    String generarNumeroFactura();
    BigDecimal calcularTotalVenta(VentaRequest request);
    Map<String, BigDecimal> calcularTotalesVenta(VentaRequest request);

    // Reportes y estadísticas
    Map<String, Object> obtenerEstadisticasDiarias(LocalDate fecha);
    Map<String, Object> obtenerEstadisticasMensuales(Integer mes, Integer anio);
    List<Map<String, Object>> obtenerTopProductosVendidos(LocalDate fechaInicio, LocalDate fechaFin);
    List<Map<String, Object>> obtenerTopClientes(LocalDate fechaInicio, LocalDate fechaFin);

    // Para dashboard
    BigDecimal obtenerVentasDelDia();
    Integer obtenerCantidadVentasDelDia();
    BigDecimal obtenerTicketPromedioDelDia();

    // Métodos internos
    Venta obtenerEntidadVenta(Long id);
    void procesarVenta(Venta venta);
}