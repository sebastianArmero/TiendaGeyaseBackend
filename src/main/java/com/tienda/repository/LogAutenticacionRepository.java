package com.tienda.repository;

import com.tienda.model.LogAutenticacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogAutenticacionRepository extends JpaRepository<LogAutenticacion, Long> {

    List<LogAutenticacion> findByUsuarioId(Long usuarioId);

    List<LogAutenticacion> findByTipoEvento(String tipoEvento);

    List<LogAutenticacion> findByIpAddress(String ipAddress);

    @Query("SELECT l FROM LogAutenticacion l WHERE l.creadoEn BETWEEN ?1 AND ?2")
    List<LogAutenticacion> findByRangoFecha(LocalDateTime inicio, LocalDateTime fin);

    @Query("SELECT COUNT(l) FROM LogAutenticacion l WHERE l.tipoEvento = ?1 AND DATE(l.creadoEn) = CURRENT_DATE")
    Long contarEventosHoy(String tipoEvento);
}