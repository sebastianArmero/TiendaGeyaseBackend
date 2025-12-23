package com.tienda.repository;

import com.tienda.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByCodigo(String codigo);

    List<Rol> findByEstado(String estado);

    List<Rol> findByNivelGreaterThanEqual(Integer nivel);

    @Query("SELECT r FROM Rol r WHERE r.nombre LIKE %?1%")
    List<Rol> buscarPorNombre(String nombre);

    boolean existsByCodigo(String codigo);
}