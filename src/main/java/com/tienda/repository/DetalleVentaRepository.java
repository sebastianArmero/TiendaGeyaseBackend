package com.tienda.repository;

import com.tienda.model.DetalleVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {

    List<DetalleVenta> findByVentaId(Long ventaId);

    List<DetalleVenta> findByProductoId(Long productoId);

    // ✅ Productos más vendidos por cantidad
    @Query("SELECT dv.producto.id, dv.producto.nombre, SUM(dv.cantidad) as totalVendido " +
            "FROM DetalleVenta dv " +
            "JOIN dv.venta v " +
            "WHERE v.estado = 'COMPLETADA' " +
            "GROUP BY dv.producto.id, dv.producto.nombre " +
            "ORDER BY totalVendido DESC")
    List<Object[]> findProductosMasVendidos();

    // ✅ Total vendido por producto
    @Query("SELECT dv.producto.id, dv.producto.nombre, SUM(dv.total) as totalVenta " +
            "FROM DetalleVenta dv " +
            "JOIN dv.venta v " +
            "WHERE v.estado = 'COMPLETADA' " +
            "GROUP BY dv.producto.id, dv.producto.nombre")
    List<Object[]> findTotalVentasPorProducto();

    // ✅ Detalles de ventas recientes
    @Query("SELECT dv FROM DetalleVenta dv JOIN dv.venta v WHERE v.fechaEmision >= ?1")
    List<DetalleVenta> findDetallesRecientes(java.time.LocalDateTime fecha);

    // ✅ Cantidad total vendida de un producto
    @Query("SELECT SUM(dv.cantidad) FROM DetalleVenta dv WHERE dv.producto.id = ?1")
    BigDecimal calcularCantidadVendidaProducto(Long productoId);
}