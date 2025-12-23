package com.tienda.repository;

import com.tienda.model.MovimientoInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    List<MovimientoInventario> findByProductoId(Long productoId);

    List<MovimientoInventario> findByTipoMovimiento(String tipoMovimiento);

    List<MovimientoInventario> findByUsuarioId(Long usuarioId);

    List<MovimientoInventario> findByFechaMovimientoBetween(LocalDateTime inicio, LocalDateTime fin);

    // ✅ CORREGIDO: Consulta con filtros
    @Query("SELECT m FROM MovimientoInventario m WHERE " +
            "(?1 IS NULL OR m.producto.id = ?1) AND " +
            "(?2 IS NULL OR m.tipoMovimiento = ?2) AND " +
            "(?3 IS NULL OR m.fechaMovimiento >= ?3) AND " +
            "(?4 IS NULL OR m.fechaMovimiento <= ?4)")
    Page<MovimientoInventario> buscarConFiltros(
            Long productoId,
            String tipoMovimiento,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            Pageable pageable);

    // ✅ Movimientos por documento (venta/compra)
    @Query("SELECT m FROM MovimientoInventario m WHERE m.documentoId = ?1 AND m.documentoNumero = ?2")
    List<MovimientoInventario> findByDocumento(Long documentoId, String documentoNumero);

    // ✅ Balance de movimientos por producto
    @Query("SELECT m.producto.id, m.tipoMovimiento, SUM(m.cantidad) " +
            "FROM MovimientoInventario m " +
            "WHERE m.producto.id = ?1 " +
            "GROUP BY m.producto.id, m.tipoMovimiento")
    List<Object[]> balanceMovimientosProducto(Long productoId);

    // ✅ Últimos movimientos
    @Query("SELECT m FROM MovimientoInventario m ORDER BY m.fechaMovimiento DESC")
    Page<MovimientoInventario> findUltimosMovimientos(Pageable pageable);
}