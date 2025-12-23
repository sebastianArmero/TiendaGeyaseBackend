package com.tienda.repository;

import com.tienda.model.CierreCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CierreCajaRepository extends JpaRepository<CierreCaja, Long> {

    List<CierreCaja> findByCajaId(Long cajaId);
    List<CierreCaja> findByUsuarioId(Long usuarioId);
    List<CierreCaja> findByEstado(CierreCaja.EstadoCierre estado);

    @Query("SELECT c FROM CierreCaja c WHERE c.fechaCierre = :fecha")
    List<CierreCaja> findByFecha(@Param("fecha") LocalDate fecha);

    // ✅ MÉTODO NUEVO: Buscar por caja y fecha
    @Query("SELECT c FROM CierreCaja c WHERE c.caja.id = :cajaId AND c.fechaCierre = :fecha")
    Optional<CierreCaja> findByCajaAndFecha(@Param("cajaId") Long cajaId, @Param("fecha") LocalDate fecha);

    // ✅ MÉTODO NUEVO: Buscar por rango de fechas
    @Query("SELECT c FROM CierreCaja c WHERE c.fechaCierre BETWEEN :fechaInicio AND :fechaFin")
    List<CierreCaja> findByFechaBetween(@Param("fechaInicio") LocalDate fechaInicio,
                                        @Param("fechaFin") LocalDate fechaFin);

    // ✅ MÉTODO NUEVO: Cierres pendientes
    @Query("SELECT c FROM CierreCaja c WHERE c.estado = 'PENDIENTE' ORDER BY c.fechaCierre DESC")
    List<CierreCaja> findCierresPendientes();

    // ✅ MÉTODO NUEVO: Cierres por sucursal
    @Query("SELECT c FROM CierreCaja c WHERE c.caja.sucursal.id = :sucursalId AND c.fechaCierre = :fecha")
    List<CierreCaja> findBySucursalAndFecha(@Param("sucursalId") Long sucursalId,
                                            @Param("fecha") LocalDate fecha);

    @Query("SELECT MONTH(c.fechaCierre) as mes, YEAR(c.fechaCierre) as año, " +
            "SUM(COALESCE(c.totalVentas, 0)) as totalVentas, " +
            "SUM(COALESCE(c.totalEgresos, 0)) as totalEgresos " +
            "FROM CierreCaja c " +
            "WHERE c.estado = 'APROBADO' " +
            "GROUP BY YEAR(c.fechaCierre), MONTH(c.fechaCierre) " +
            "ORDER BY YEAR(c.fechaCierre) DESC, MONTH(c.fechaCierre) DESC")
    List<Object[]> resumenMensual();

}