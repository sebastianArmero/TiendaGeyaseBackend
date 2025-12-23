package com.tienda.repository;

import com.tienda.model.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermisoRepository extends JpaRepository<Permiso, Long> {

    Optional<Permiso> findByCodigo(String codigo);

    List<Permiso> findByModulo(String modulo);

    List<Permiso> findByCategoria(String categoria);

    List<Permiso> findByEstado(String estado);

    @Query("SELECT p FROM Permiso p WHERE p.modulo = ?1 AND p.categoria = ?2")
    List<Permiso> findByModuloAndCategoria(String modulo, String categoria);

    @Query("SELECT DISTINCT p.modulo FROM Permiso p WHERE p.estado = 'ACTIVO'")
    List<String> findModulosActivos();
}