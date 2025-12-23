package com.tienda.repository;

import com.tienda.model.Proveedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    Optional<Proveedor> findByCodigo(String codigo);

    Optional<Proveedor> findByRuc(String ruc);

    List<Proveedor> findByEstado(Proveedor.EstadoProveedor estado);

    @Query("SELECT p FROM Proveedor p WHERE " +
            "(?1 IS NULL OR p.nombre LIKE %?1%) AND " +
            "(?2 IS NULL OR p.ruc LIKE %?2%) AND " +
            "(?3 IS NULL OR p.estado = ?3)")
    Page<Proveedor> buscarConFiltros(String nombre, String ruc, Proveedor.EstadoProveedor estado, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Proveedor p WHERE p.estado = 'ACTIVO'")
    Long contarProveedoresActivos();

    boolean existsByCodigo(String codigo);

    boolean existsByRuc(String ruc);
}