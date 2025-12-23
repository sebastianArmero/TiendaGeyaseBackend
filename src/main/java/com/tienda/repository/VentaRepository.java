package com.tienda.repository;

import com.tienda.model.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    Optional<Venta> findByNumeroFactura(String numeroFactura);
    List<Venta> findByClienteId(Long clienteId);
    List<Venta> findByVendedorId(Long vendedorId);
    List<Venta> findByCajaId(Long cajaId);
    List<Venta> findByEstado(Venta.EstadoVenta estado);
    List<Venta> findBySucursalId(Long sucursalId);
    List<Venta> findByFechaEmisionBetween(LocalDateTime inicio, LocalDateTime fin);

    @Query("SELECT v FROM Venta v WHERE DATE(v.fechaEmision) = :fecha")
    List<Venta> findByFecha(@Param("fecha") LocalDate fecha);

    // ✅ MÉTODONUEVO: Buscar ventas por rango de fechas y estado
    @Query("SELECT v FROM Venta v WHERE v.fechaEmision BETWEEN :inicio AND :fin AND v.estado = :estado")
    List<Venta> findByFechaBetweenAndEstado(@Param("inicio") LocalDateTime inicio,
                                            @Param("fin") LocalDateTime fin,
                                            @Param("estado") Venta.EstadoVenta estado);

    // ✅ MÉTOD NUEVO:Buscarconmúltiplesfiltros
    @Query("SELECT v FROM Venta v WHERE " +
            "(:numeroFactura IS NULL OR v.numeroFactura LIKE %:numeroFactura%) AND " +
            "(:clienteNombre IS NULL OR v.clienteNombre LIKE %:clienteNombre%) AND " +
            "(:vendedorId IS NULL OR v.vendedor.id = :vendedorId) AND " +
            "(:estado IS NULL OR v.estado = :estado) AND " +
            "(:fechaDesde IS NULL OR v.fechaEmision >= :fechaDesde) AND " +
            "(:fechaHasta IS NULL OR v.fechaEmision <= :fechaHasta)")
    Page<Venta> buscarConFiltros(
            @Param("numeroFactura") String numeroFactura,
            @Param("clienteNombre") String clienteNombre,
            @Param("vendedorId") Long vendedorId,
            @Param("estado") Venta.EstadoVenta estado,
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta,
            Pageable pageable);

    @Query("SELECT SUM(v.total) FROM Venta v WHERE v.estado = 'COMPLETADA' AND DATE(v.fechaEmision) = :fecha")
    BigDecimal calcularVentasDelDia(@Param("fecha") LocalDate fecha);

    // ✅ MÉTODONUEVO: Calcular ventas por período
    @Query("SELECT SUM(v.total) FROM Venta v WHERE v.estado = 'COMPLETADA' AND v.fechaEmision BETWEEN :inicio AND :fin")
    BigDecimal calcularVentasPorPeriodo(@Param("inicio") LocalDateTime inicio,
                                        @Param("fin") LocalDateTime fin);



    // ✅ MÉTODONUEVO: Contar ventas por período
    @Query("SELECT COUNT(v) FROM Venta v WHERE v.estado = 'COMPLETADA' AND v.fechaEmision BETWEEN :inicio AND :fin")
    Long contarVentasPorPeriodo(@Param("inicio") LocalDateTime inicio,
                                @Param("fin") LocalDateTime fin);

    // ✅ MÉTODONUEVO: Ventas por forma de pago
    @Query("SELECT v.formaPago, SUM(v.total) FROM Venta v WHERE v.estado = 'COMPLETADA' AND v.fechaEmision BETWEEN :inicio AND :fin GROUP BY v.formaPago")
    List<Object[]> ventasPorFormaPago(@Param("inicio") LocalDateTime inicio,
                                      @Param("fin") LocalDateTime fin);

    // ✅ MÉTODONUEVO: Ventas por vendedor
    @Query("SELECT v.vendedor.nombreCompleto, SUM(v.total) FROM Venta v WHERE v.estado = 'COMPLETADA' AND v.fechaEmision BETWEEN :inicio AND :fin GROUP BY v.vendedor.id, v.vendedor.nombreCompleto")
    List<Object[]> ventasPorVendedor(@Param("inicio") LocalDateTime inicio,
                                     @Param("fin") LocalDateTime fin);

    @Query("SELECT v.vendedor.nombreCompleto, SUM(v.total) FROM Venta v WHERE v.estado = 'COMPLETADA' GROUP BY v.vendedor.id, v.vendedor.nombreCompleto")
    List<Object[]> ventasPorVendedor();
    boolean existsByNumeroFactura(String numeroFactura);


}