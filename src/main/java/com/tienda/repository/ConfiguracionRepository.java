package com.tienda.repository;

import com.tienda.model.Configuracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfiguracionRepository extends JpaRepository<Configuracion, Long> {
    Optional<Configuracion> findByClave(String clave);
    List<Configuracion> findByCategoria(String categoria);
    boolean existsByClave(String clave);
}