package com.tienda.service;

import com.tienda.dto.request.VentaRequest;
import com.tienda.dto.response.VentaResponse;
import com.tienda.dto.response.PaginacionResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface VentaService {

    // CRUD
    VentaResponse crearVenta(VentaRequest request);
    VentaResponse obtenerVentaPorId(Long id);
    VentaResponse obtenerVentaPorNumeroFactura(String numeroFactura);
    PaginacionResponse<VentaResponse> obtenerVentasPaginadas(Pageable pageable);

    // Filtros
    PaginacionResponse<VentaResponse> filtrarVentas(
            String numeroFactura, String clienteNombre, Long vendedorId,
            String estado, LocalDate fechaDesde, LocalDate fechaHasta, Pageable pageable);

    // Gestión de ventas
    void anularVenta(Long id, String motivo);
    String generarNumeroFactura();

    // Consultas específicas
    List<VentaResponse> obtenerVentasDelDia();
    List<VentaResponse> obtenerVentasPorCliente(Long clienteId);
    List<VentaResponse> obtenerVentasPorVendedor(Long vendedorId);
    List<VentaResponse> obtenerVentasPorRangoFecha(LocalDateTime fechaInicio, LocalDateTime fechaFin);

    // Estadísticas
    Map<String, Object> obtenerEstadisticasDiarias(LocalDate fecha);
    Map<String, Object> obtenerEstadisticasMensuales(int mes, int año);
    Map<String, Object> obtenerDashboardVentas();
    List<Map<String, Object>> obtenerTopProductosVendidos(int limite);

    // Métodos de negocio
    BigDecimal calcularTotalVentasDia(LocalDate fecha);
    Long contarVentasDelDia();
    boolean existeVentaConFactura(String numeroFactura);
}