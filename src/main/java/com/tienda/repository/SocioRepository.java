package com.tienda.repository;

import com.tienda.model.Socio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SocioRepository extends JpaRepository<Socio, Long> {
    List<Socio> findByEstado(String estado);
}