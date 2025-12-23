package com.tienda.service;

import com.tienda.dto.request.CajaRequest;
import com.tienda.dto.request.CierreCajaRequest;
import com.tienda.dto.response.CajaResponse;
import com.tienda.dto.response.CierreCajaResponse;
import com.tienda.dto.response.PaginacionResponse;
import com.tienda.model.Caja;
import com.tienda.model.CierreCaja;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface CajaService {

    // ============ CRUD CAJAS ============
    CajaResponse crearCaja(CajaRequest request);
    CajaResponse actualizarCaja(Long id, CajaRequest request);
    void eliminarCaja(Long id);
    CajaResponse obtenerCajaPorId(Long id);
    CajaResponse obtenerCajaPorCodigo(String codigo);
    List<CajaResponse> obtenerTodasCajas();
    PaginacionResponse<CajaResponse> obtenerCajasPaginadas(Pageable pageable);

    // ============ GESTIÓN DE CAJAS ============
    CajaResponse abrirCaja(Long cajaId, Long usuarioId, BigDecimal saldoInicial);
    CajaResponse cerrarCaja(Long cajaId, Long usuarioId, BigDecimal saldoFinalReal, String observaciones);
    CajaResponse asignarUsuario(Long cajaId, Long usuarioId);
    CajaResponse desasignarUsuario(Long cajaId);
    CajaResponse bloquearCaja(Long cajaId, String motivo);
    CajaResponse desbloquearCaja(Long cajaId);

    // ============ CONSULTAS DE CAJAS ============
    List<CajaResponse> obtenerCajasPorSucursal(Long sucursalId);
    List<CajaResponse> obtenerCajasPorEstado(Caja.EstadoCaja estado);
    List<CajaResponse> obtenerCajasAbiertas();
    List<CajaResponse> obtenerCajasDisponibles();
    CajaResponse obtenerCajaAbiertaPorUsuario(Long usuarioId);

    // ============ CIERRE DE CAJA ============
    CierreCajaResponse realizarCierreDiario(CierreCajaRequest request);
    CierreCajaResponse obtenerCierrePorId(Long cierreId);
    CierreCajaResponse obtenerCierrePorCajaYFecha(Long cajaId, LocalDate fecha);
    List<CierreCajaResponse> obtenerCierresPorCaja(Long cajaId);
    List<CierreCajaResponse> obtenerCierresPorFecha(LocalDate fecha);
    List<CierreCajaResponse> obtenerCierresPendientes();

    // ============ CONCILIACIÓN ============
    CierreCajaResponse conciliarCierre(Long cierreId, Long usuarioId, String observaciones);
    CierreCajaResponse aprobarCierre(Long cierreId, Long usuarioId);
    CierreCajaResponse rechazarCierre(Long cierreId, Long usuarioId, String motivo);

    // ============ REPORTES Y ESTADÍSTICAS ============
    Map<String, Object> obtenerEstadoCajas();
    Map<String, Object> generarReporteCierreDiario(Long cajaId, LocalDate fecha);
    Map<String, Object> generarReporteMensual(Integer mes, Integer año);
    Map<String, Object> obtenerEstadisticasCajas();

    // ============ VALIDACIONES ============
    boolean existeCajaPorCodigo(String codigo);
    boolean verificarCajaAbierta(Long cajaId);
    boolean verificarUsuarioTieneCajaAbierta(Long usuarioId);

    // ============ MÉTODOS INTERNOS ============
    Caja obtenerEntidadCaja(Long id);
    Caja obtenerCajaAbiertaEntidad(Long cajaId);
    void actualizarSaldoCaja(Long cajaId, BigDecimal monto, String tipoOperacion);
}