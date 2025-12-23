package com.tienda.repository;

import com.tienda.model.Caja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CajaRepository extends JpaRepository<Caja, Long> {

    Optional<Caja> findByCodigo(String codigo);

    List<Caja> findBySucursalId(Long sucursalId);

    List<Caja> findByEstado(Caja.EstadoCaja estado);

    List<Caja> findByUsuarioAsignadoId(Long usuarioId);

    // ✅ Cajas abiertas
    @Query("SELECT c FROM Caja c WHERE c.estado = 'ABIERTA'")
    List<Caja> findCajasAbiertas();

    // ✅ Cajas por sucursal con estado
    @Query("SELECT c FROM Caja c WHERE c.sucursal.id = ?1 AND c.estado = ?2")
    List<Caja> findBySucursalAndEstado(Long sucursalId, Caja.EstadoCaja estado);

    // ✅ Caja asignada a usuario
    @Query("SELECT c FROM Caja c WHERE c.usuarioAsignado.id = ?1 AND c.estado = 'ABIERTA'")
    Optional<Caja> findCajaAbiertaPorUsuario(Long usuarioId);

    boolean existsByCodigo(String codigo);
}