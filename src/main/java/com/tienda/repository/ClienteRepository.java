package com.tienda.repository;

import com.tienda.model.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByNumeroDocumento(String numeroDocumento);

    List<Cliente> findByTipo(Cliente.TipoCliente tipo);

    List<Cliente> findByEstado(String estado);

    @Query("SELECT c FROM Cliente c WHERE " +
            "(?1 IS NULL OR c.nombre LIKE %?1%) AND " +
            "(?2 IS NULL OR c.numeroDocumento LIKE %?2%) AND " +
            "(?3 IS NULL OR c.tipo = ?3)")
    Page<Cliente> buscarConFiltros(String nombre, String documento, Cliente.TipoCliente tipo, Pageable pageable);

    // ✅ Clientes con mayor compra
    @Query("SELECT c FROM Cliente c ORDER BY c.totalCompras DESC")
    Page<Cliente> findTopClientes(Pageable pageable);

    // ✅ Clientes inactivos (sin compras en X días)
    @Query("SELECT c FROM Cliente c WHERE c.ultimaCompra < ?1")
    List<Cliente> findClientesInactivos(java.time.LocalDateTime fechaLimite);

    @Query("SELECT COUNT(c) FROM Cliente c WHERE c.estado = 'ACTIVO'")
    Long contarClientesActivos();

    boolean existsByNumeroDocumento(String numeroDocumento);
}