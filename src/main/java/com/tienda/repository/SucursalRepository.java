package com.tienda.repository;

import com.tienda.model.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SucursalRepository extends JpaRepository<Sucursal, Long> {

    Optional<Sucursal> findByCodigo(String codigo);

    List<Sucursal> findByEstado(String estado);

    boolean existsByCodigo(String codigo);
}