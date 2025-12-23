package com.tienda.repository;

import com.tienda.model.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findByCodigo(String codigo);
    Optional<Producto> findByCodigoBarras(String codigoBarras);
    List<Producto> findByCategoriaId(Long categoriaId);
    List<Producto> findByProveedorId(Long proveedorId);
    List<Producto> findByEstado(Producto.EstadoProducto estado);
    List<Producto> findByAlertaStock(Producto.AlertaStock alertaStock);

    // ✅ CORREGIDO: Usando @Param
    @Query("SELECT p FROM Producto p WHERE p.alertaStock IN :alertas")
    List<Producto> findByAlertaStockIn(@Param("alertas") List<Producto.AlertaStock> alertas);

    @Query("SELECT p FROM Producto p WHERE p.stockActual <= p.stockMinimo")
    List<Producto> findProductosStockBajo();

    @Query("SELECT p FROM Producto p WHERE p.stockActual <= 0")
    List<Producto> findProductosAgotados();

    @Query("SELECT p FROM Producto p WHERE p.stockActual <= p.stockMinimo AND p.estado = 'ACTIVO'")
    List<Producto> findProductosParaReorden();

    // ✅ CORREGIDO: Usando @Param correctamente
    @Query("SELECT p FROM Producto p WHERE " +
            "(:codigo IS NULL OR p.codigo LIKE %:codigo%) AND " +
            "(:nombre IS NULL OR p.nombre LIKE %:nombre%) AND " +
            "(:categoriaId IS NULL OR p.categoria.id = :categoriaId) AND " +
            "(:estado IS NULL OR p.estado = :estado)")
    Page<Producto> buscarConFiltros(
            @Param("codigo") String codigo,
            @Param("nombre") String nombre,
            @Param("categoriaId") Long categoriaId,
            @Param("estado") Producto.EstadoProducto estado,
            Pageable pageable);

    boolean existsByCodigo(String codigo);
    boolean existsByCodigoBarras(String codigoBarras);
}