package com.tienda.service.impl;

import com.tienda.dto.request.CajaRequest;
import com.tienda.dto.request.CierreCajaRequest;
import com.tienda.dto.response.CajaResponse;
import com.tienda.dto.response.CierreCajaResponse;
import com.tienda.dto.response.PaginacionResponse;
import com.tienda.exception.CajaAbiertaException;
import com.tienda.exception.ResourceNotFoundException;
import com.tienda.exception.ValidacionException;
import com.tienda.model.*;
import com.tienda.repository.*;
import com.tienda.service.CajaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CajaServiceImpl implements CajaService {

    private final CajaRepository cajaRepository;
    private final CierreCajaRepository cierreCajaRepository;
    private final UsuarioRepository usuarioRepository;
    private final SucursalRepository sucursalRepository;
    private final VentaRepository ventaRepository;

    // ============ CRUD CAJAS ============

    @Override
    @Transactional
    public CajaResponse crearCaja(CajaRequest request) {
        // Validar código único
        if (existeCajaPorCodigo(request.getCodigo())) {
            throw new ValidacionException("Ya existe una caja con ese código");
        }

        // Buscar sucursal
        Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));

        // Buscar usuario asignado si se especifica
        Usuario usuarioAsignado = null;
        if (request.getUsuarioAsignadoId() != null) {
            usuarioAsignado = usuarioRepository.findById(request.getUsuarioAsignadoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        }

        // Convertir estado
        Caja.EstadoCaja estado = Caja.EstadoCaja.CERRADA;
        if (request.getEstado() != null) {
            try {
                estado = Caja.EstadoCaja.valueOf(request.getEstado().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ValidacionException("Estado de caja inválido");
            }
        }

        // Crear caja
        Caja caja = Caja.builder()
                .codigo(request.getCodigo())
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .sucursal(sucursal)
                .saldoInicial(request.getSaldoInicial() != null ? request.getSaldoInicial() : BigDecimal.ZERO)
                .saldoActual(request.getSaldoInicial() != null ? request.getSaldoInicial() : BigDecimal.ZERO)
                .estado(estado)
                .usuarioAsignado(usuarioAsignado)
                .build();

        caja = cajaRepository.save(caja);
        log.info("Caja creada: {} - {}", caja.getCodigo(), caja.getNombre());

        return convertirAResponse(caja);
    }

    @Override
    @Transactional
    public CajaResponse actualizarCaja(Long id, CajaRequest request) {
        Caja caja = obtenerEntidadCaja(id);

        // Validar código único si se cambia
        if (request.getCodigo() != null && !request.getCodigo().equals(caja.getCodigo())) {
            if (existeCajaPorCodigo(request.getCodigo())) {
                throw new ValidacionException("Ya existe una caja con ese código");
            }
            caja.setCodigo(request.getCodigo());
        }

        // Validar que no esté abierta para ciertos cambios
        if (caja.estaAbierta() && request.getSaldoInicial() != null) {
            throw new ValidacionException("No se puede modificar el saldo inicial de una caja abierta");
        }

        // Actualizar campos
        if (request.getNombre() != null) caja.setNombre(request.getNombre());
        if (request.getDescripcion() != null) caja.setDescripcion(request.getDescripcion());

        // Actualizar sucursal
        if (request.getSucursalId() != null) {
            Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
            caja.setSucursal(sucursal);
        }

        // Actualizar saldo inicial (solo si está cerrada)
        if (request.getSaldoInicial() != null && caja.estaCerrada()) {
            caja.setSaldoInicial(request.getSaldoInicial());
            caja.setSaldoActual(request.getSaldoInicial());
        }

        // Actualizar estado
        if (request.getEstado() != null) {
            try {
                caja.setEstado(Caja.EstadoCaja.valueOf(request.getEstado().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new ValidacionException("Estado de caja inválido");
            }
        }

        // Actualizar usuario asignado
        if (request.getUsuarioAsignadoId() != null) {
            Usuario usuario = usuarioRepository.findById(request.getUsuarioAsignadoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
            caja.setUsuarioAsignado(usuario);
        }

        caja = cajaRepository.save(caja);

        return convertirAResponse(caja);
    }

    @Override
    @Transactional
    public void eliminarCaja(Long id) {
        Caja caja = obtenerEntidadCaja(id);

        // Validar que no esté abierta
        if (caja.estaAbierta()) {
            throw new ValidacionException("No se puede eliminar una caja abierta");
        }

        // Validar que no tenga cierres asociados
        List<CierreCaja> cierres = cierreCajaRepository.findByCajaId(id);
        if (!cierres.isEmpty()) {
            throw new ValidacionException("No se puede eliminar una caja con cierres registrados");
        }

        cajaRepository.delete(caja);
        log.info("Caja eliminada: {}", caja.getNombre());
    }

    @Override
    @Transactional(readOnly = true)
    public CajaResponse obtenerCajaPorId(Long id) {
        Caja caja = obtenerEntidadCaja(id);
        return convertirAResponse(caja);
    }

    @Override
    @Transactional(readOnly = true)
    public CajaResponse obtenerCajaPorCodigo(String codigo) {
        Caja caja = cajaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Caja no encontrada"));
        return convertirAResponse(caja);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CajaResponse> obtenerTodasCajas() {
        return cajaRepository.findAll().stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginacionResponse<CajaResponse> obtenerCajasPaginadas(Pageable pageable) {
        Page<Caja> cajasPage = cajaRepository.findAll(pageable);

        List<CajaResponse> cajasResponse = cajasPage.getContent().stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());

        return PaginacionResponse.<CajaResponse>builder()
                .content(cajasResponse)
                .pageNumber(cajasPage.getNumber())
                .pageSize(cajasPage.getSize())
                .totalElements(cajasPage.getTotalElements())
                .totalPages(cajasPage.getTotalPages())
                .last(cajasPage.isLast())
                .build();
    }

    // ============ GESTIÓN DE CAJAS ============

    @Override
    @Transactional
    public CajaResponse abrirCaja(Long cajaId, Long usuarioId, BigDecimal saldoInicial) {
        Caja caja = obtenerEntidadCaja(cajaId);
        Usuario usuario = obtenerUsuario(usuarioId);

        // Validar que la caja no esté abierta
        if (caja.estaAbierta()) {
            throw new CajaAbiertaException(caja.getNombre(),
                    caja.getUsuarioAsignado() != null ?
                            caja.getUsuarioAsignado().getNombreCompleto() : "Desconocido");
        }

        // Validar que el usuario no tenga otra caja abierta
        if (verificarUsuarioTieneCajaAbierta(usuarioId)) {
            Optional<Caja> cajaUsuario = cajaRepository.findCajaAbiertaPorUsuario(usuarioId);
            if (cajaUsuario.isPresent()) {
                throw new ValidacionException("El usuario ya tiene la caja " +
                        cajaUsuario.get().getNombre() + " abierta");
            }
        }

        // Actualizar caja
        caja.setEstado(Caja.EstadoCaja.ABIERTA);
        caja.setUsuarioAsignado(usuario);
        caja.setFechaApertura(LocalDateTime.now());
        caja.setFechaCierre(null);

        // Actualizar saldos
        if (saldoInicial != null) {
            caja.setSaldoInicial(saldoInicial);
            caja.setSaldoActual(saldoInicial);
        }

        caja = cajaRepository.save(caja);

        log.info("Caja abierta: {} por usuario: {}", caja.getNombre(), usuario.getNombreCompleto());

        return convertirAResponse(caja);
    }

    @Override
    @Transactional
    public CajaResponse cerrarCaja(Long cajaId, Long usuarioId, BigDecimal saldoFinalReal, String observaciones) {
        Caja caja = obtenerCajaAbiertaEntidad(cajaId);
        Usuario usuario = obtenerUsuario(usuarioId);

        // Validar que el usuario que cierra sea el asignado
        if (caja.getUsuarioAsignado() == null ||
                !caja.getUsuarioAsignado().getId().equals(usuarioId)) {
            throw new ValidacionException("Solo el usuario asignado puede cerrar la caja");
        }

        // Calcular total de ventas del día
        BigDecimal totalVentasDia = ventaRepository.calcularVentasDelDia(LocalDate.now());
        if (totalVentasDia == null) {
            totalVentasDia = BigDecimal.ZERO;
        }

        // Calcular saldo final teórico
        BigDecimal saldoFinalTeorico = caja.getSaldoInicial().add(totalVentasDia);

        // Crear cierre de caja
        CierreCaja cierre = CierreCaja.builder()
                .caja(caja)
                .fechaCierre(LocalDate.now())
                .horaApertura(caja.getFechaApertura() != null ?
                        caja.getFechaApertura().toLocalTime() : LocalTime.now())
                .horaCierre(LocalTime.now())
                .usuario(usuario)
                .saldoInicial(caja.getSaldoInicial())
                .saldoFinalTeorico(saldoFinalTeorico)
                .saldoFinalReal(saldoFinalReal != null ? saldoFinalReal : saldoFinalTeorico)
                .totalVentas(totalVentasDia)
                .efectivo(saldoFinalReal != null ? saldoFinalReal : BigDecimal.ZERO) // Simplificado
                .estado(CierreCaja.EstadoCierre.PENDIENTE)
                .observaciones(observaciones)
                .creadoEn(LocalDateTime.now())
                .build();

        // Guardar cierre
        cierre = cierreCajaRepository.save(cierre);

        // Cerrar caja
        caja.setEstado(Caja.EstadoCaja.CERRADA);
        caja.setFechaCierre(LocalDateTime.now());
        caja.setSaldoActual(saldoFinalReal != null ? saldoFinalReal : saldoFinalTeorico);
        cajaRepository.save(caja);

        log.info("Caja cerrada: {} - Diferencia: {}",
                caja.getNombre(), cierre.getDiferencia());

        return convertirAResponse(caja);
    }

    @Override
    @Transactional
    public CajaResponse asignarUsuario(Long cajaId, Long usuarioId) {
        Caja caja = obtenerEntidadCaja(cajaId);
        Usuario usuario = obtenerUsuario(usuarioId);

        // Validar que la caja no esté abierta
        if (caja.estaAbierta()) {
            throw new ValidacionException("No se puede asignar usuario a una caja abierta");
        }

        caja.setUsuarioAsignado(usuario);
        caja = cajaRepository.save(caja);

        log.info("Usuario asignado a caja: {} -> {}",
                usuario.getNombreCompleto(), caja.getNombre());

        return convertirAResponse(caja);
    }

    @Override
    @Transactional
    public CajaResponse desasignarUsuario(Long cajaId) {
        Caja caja = obtenerEntidadCaja(cajaId);

        // Validar que la caja no esté abierta
        if (caja.estaAbierta()) {
            throw new ValidacionException("No se puede desasignar usuario de una caja abierta");
        }

        caja.setUsuarioAsignado(null);
        caja = cajaRepository.save(caja);

        return convertirAResponse(caja);
    }

    @Override
    @Transactional
    public CajaResponse bloquearCaja(Long cajaId, String motivo) {
        Caja caja = obtenerEntidadCaja(cajaId);

        caja.setEstado(Caja.EstadoCaja.BLOQUEADA);
        caja = cajaRepository.save(caja);

        log.info("Caja bloqueada: {} - Motivo: {}", caja.getNombre(), motivo);

        return convertirAResponse(caja);
    }

    @Override
    @Transactional
    public CajaResponse desbloquearCaja(Long cajaId) {
        Caja caja = obtenerEntidadCaja(cajaId);

        caja.setEstado(Caja.EstadoCaja.CERRADA);
        caja = cajaRepository.save(caja);

        return convertirAResponse(caja);
    }

    // ============ CONSULTAS DE CAJAS ============

    @Override
    @Transactional(readOnly = true)
    public List<CajaResponse> obtenerCajasPorSucursal(Long sucursalId) {
        return cajaRepository.findBySucursalId(sucursalId).stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CajaResponse> obtenerCajasPorEstado(Caja.EstadoCaja estado) {
        return cajaRepository.findByEstado(estado).stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CajaResponse> obtenerCajasAbiertas() {
        return cajaRepository.findCajasAbiertas().stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CajaResponse> obtenerCajasDisponibles() {
        return cajaRepository.findByEstado(Caja.EstadoCaja.CERRADA).stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CajaResponse obtenerCajaAbiertaPorUsuario(Long usuarioId) {
        Caja caja = cajaRepository.findCajaAbiertaPorUsuario(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no tiene caja abierta"));
        return convertirAResponse(caja);
    }

    // ============ CIERRE DE CAJA ============

    @Override
    @Transactional
    public CierreCajaResponse realizarCierreDiario(CierreCajaRequest request) {
        // Buscar caja
        Caja caja = obtenerEntidadCaja(request.getCajaId());

        // Validar que la caja esté abierta
        if (!caja.estaAbierta()) {
            throw new ValidacionException("La caja debe estar abierta para realizar cierre");
        }

        // Obtener usuario asignado
        Usuario usuario = caja.getUsuarioAsignado();
        if (usuario == null) {
            throw new ValidacionException("La caja no tiene usuario asignado");
        }

        // Calcular total de ventas del día
        BigDecimal totalVentas = ventaRepository.calcularVentasDelDia(request.getFechaCierre());
        if (totalVentas == null) {
            totalVentas = BigDecimal.ZERO;
        }

        // Calcular saldo final teórico
        BigDecimal saldoFinalTeorico = caja.getSaldoInicial().add(totalVentas);

        // Si no se proporciona saldo final real, usar el teórico
        BigDecimal saldoFinalReal = request.getSaldoFinalReal() != null ?
                request.getSaldoFinalReal() : saldoFinalTeorico;

        // Crear cierre de caja
        CierreCaja cierre = CierreCaja.builder()
                .caja(caja)
                .fechaCierre(request.getFechaCierre())
                .horaApertura(request.getHoraApertura())
                .horaCierre(request.getHoraCierre())
                .usuario(usuario)
                .saldoInicial(caja.getSaldoInicial())
                .saldoFinalTeorico(saldoFinalTeorico)
                .saldoFinalReal(saldoFinalReal)
                .totalVentas(totalVentas)
                .efectivo(request.getEfectivo() != null ? request.getEfectivo() : saldoFinalReal)
                .tarjetas(request.getTarjetas())
                .transferencias(request.getTransferencias())
                .otrosMedios(request.getOtrosMedios())
                .estado(CierreCaja.EstadoCierre.PENDIENTE)
                .observaciones(request.getObservaciones())
                .creadoEn(LocalDateTime.now())
                .build();

        // Calcular diferencia
        cierre.setDiferencia(cierre.getDiferencia());

        // Guardar cierre
        cierre = cierreCajaRepository.save(cierre);

        // Cerrar la caja
        caja.setEstado(Caja.EstadoCaja.CERRADA);
        caja.setFechaCierre(LocalDateTime.now());
        caja.setSaldoActual(saldoFinalReal);
        cajaRepository.save(caja);

        log.info("Cierre diario realizado para caja {}: Diferencia: {}",
                caja.getNombre(), cierre.getDiferencia());

        return convertirCierreAResponse(cierre);
    }

    @Override
    @Transactional(readOnly = true)
    public CierreCajaResponse obtenerCierrePorId(Long cierreId) {
        CierreCaja cierre = cierreCajaRepository.findById(cierreId)
                .orElseThrow(() -> new ResourceNotFoundException("Cierre de caja no encontrado"));
        return convertirCierreAResponse(cierre);
    }

    @Override
    @Transactional(readOnly = true)
    public CierreCajaResponse obtenerCierrePorCajaYFecha(Long cajaId, LocalDate fecha) {
        CierreCaja cierre = cierreCajaRepository.findByCajaAndFecha(cajaId, fecha)
                .orElseThrow(() -> new ResourceNotFoundException("Cierre de caja no encontrado para esa fecha"));
        return convertirCierreAResponse(cierre);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CierreCajaResponse> obtenerCierresPorCaja(Long cajaId) {
        return cierreCajaRepository.findByCajaId(cajaId).stream()
                .map(this::convertirCierreAResponse)
                .sorted((c1, c2) -> c2.getFechaCierre().compareTo(c1.getFechaCierre()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CierreCajaResponse> obtenerCierresPorFecha(LocalDate fecha) {
        return cierreCajaRepository.findByFecha(fecha).stream()
                .map(this::convertirCierreAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CierreCajaResponse> obtenerCierresPendientes() {
        return cierreCajaRepository.findCierresPendientes().stream()
                .map(this::convertirCierreAResponse)
                .collect(Collectors.toList());
    }

    // ============ CONCILIACIÓN ============

    @Override
    @Transactional
    public CierreCajaResponse conciliarCierre(Long cierreId, Long usuarioId, String observaciones) {
        CierreCaja cierre = obtenerCierre(cierreId);
        Usuario usuario = obtenerUsuario(usuarioId);

        // Validar que el cierre esté pendiente
        if (cierre.getEstado() != CierreCaja.EstadoCierre.PENDIENTE) {
            throw new ValidacionException("Solo se pueden conciliar cierres pendientes");
        }

        cierre.setEstado(CierreCaja.EstadoCierre.CONCILIADO);
        cierre.setConciliadoPor(usuario);
        cierre.setFechaConciliacion(LocalDateTime.now());

        if (observaciones != null) {
            cierre.setObservaciones(cierre.getObservaciones() + "\nConciliación: " + observaciones);
        }

        cierre = cierreCajaRepository.save(cierre);

        log.info("Cierre conciliado: {} - Usuario: {}", cierreId, usuario.getNombreCompleto());

        return convertirCierreAResponse(cierre);
    }

    @Override
    @Transactional
    public CierreCajaResponse aprobarCierre(Long cierreId, Long usuarioId) {
        CierreCaja cierre = obtenerCierre(cierreId);
        Usuario usuario = obtenerUsuario(usuarioId);

        // Validar que el cierre esté conciliado
        if (cierre.getEstado() != CierreCaja.EstadoCierre.CONCILIADO) {
            throw new ValidacionException("Solo se pueden aprobar cierres conciliados");
        }

        cierre.setEstado(CierreCaja.EstadoCierre.APROBADO);
        cierre.setConciliadoPor(usuario);
        cierre.setFechaConciliacion(LocalDateTime.now());

        cierre = cierreCajaRepository.save(cierre);

        return convertirCierreAResponse(cierre);
    }

    @Override
    @Transactional
    public CierreCajaResponse rechazarCierre(Long cierreId, Long usuarioId, String motivo) {
        CierreCaja cierre = obtenerCierre(cierreId);
        Usuario usuario = obtenerUsuario(usuarioId);

        cierre.setEstado(CierreCaja.EstadoCierre.RECHAZADO);
        cierre.setConciliadoPor(usuario);
        cierre.setFechaConciliacion(LocalDateTime.now());

        if (motivo != null) {
            cierre.setObservaciones(cierre.getObservaciones() + "\nRechazado: " + motivo);
        }

        cierre = cierreCajaRepository.save(cierre);

        // Reabrir la caja si está cerrada
        Caja caja = cierre.getCaja();
        if (caja.estaCerrada()) {
            caja.setEstado(Caja.EstadoCaja.ABIERTA);
            caja.setFechaCierre(null);
            cajaRepository.save(caja);
        }

        return convertirCierreAResponse(cierre);
    }

    // ============ REPORTES Y ESTADÍSTICAS ============

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadoCajas() {
        Map<String, Object> estado = new HashMap<>();

        List<Caja> cajas = cajaRepository.findAll();

        long totalCajas = cajas.size();
        long cajasAbiertas = cajas.stream()
                .filter(Caja::estaAbierta)
                .count();
        long cajasCerradas = cajas.stream()
                .filter(Caja::estaCerrada)
                .count();
        long cajasBloqueadas = cajas.stream()
                .filter(c -> c.getEstado() == Caja.EstadoCaja.BLOQUEADA)
                .count();

        estado.put("totalCajas", totalCajas);
        estado.put("cajasAbiertas", cajasAbiertas);
        estado.put("cajasCerradas", cajasCerradas);
        estado.put("cajasBloqueadas", cajasBloqueadas);
        estado.put("cajasDisponibles", cajasCerradas);
        estado.put("fechaConsulta", LocalDateTime.now());

        return estado;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> generarReporteCierreDiario(Long cajaId, LocalDate fecha) {
        Map<String, Object> reporte = new HashMap<>();

        // Obtener cierre del día
        CierreCaja cierre = cierreCajaRepository.findByCajaAndFecha(cajaId, fecha)
                .orElseThrow(() -> new ResourceNotFoundException("No hay cierre para esa fecha"));

        // Obtener ventas del día
        List<Venta> ventas = ventaRepository.findByFecha(fecha);

        // Calcular estadísticas
        BigDecimal totalVentasEfectivo = ventas.stream()
                .filter(v -> "EFECTIVO".equals(v.getFormaPago()))
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalVentasTarjeta = ventas.stream()
                .filter(v -> v.getFormaPago() != null && v.getFormaPago().contains("TARJETA"))
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Construir reporte
        reporte.put("cierre", convertirCierreAResponse(cierre));
        reporte.put("totalVentas", ventas.size());
        reporte.put("ventasEfectivo", totalVentasEfectivo);
        reporte.put("ventasTarjeta", totalVentasTarjeta);
        reporte.put("promedioTicket", ventas.isEmpty() ? BigDecimal.ZERO :
                cierre.getTotalVentas().divide(new BigDecimal(ventas.size()), 2, java.math.RoundingMode.HALF_UP));
        reporte.put("ventasPorHora", calcularVentasPorHora(ventas));
        reporte.put("fechaGeneracion", LocalDateTime.now());

        return reporte;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> generarReporteMensual(Integer mes, Integer año) {
        Map<String, Object> reporte = new HashMap<>();

        // Obtener resumen mensual del repositorio
        List<Object[]> resumen = cierreCajaRepository.resumenMensual();

        // Filtrar por mes y año solicitados
        List<Map<String, Object>> datosMensuales = new ArrayList<>();
        BigDecimal totalVentasMensual = BigDecimal.ZERO;
        BigDecimal totalEgresosMensual = BigDecimal.ZERO;

        for (Object[] fila : resumen) {
            Integer mesDato = (Integer) fila[0];
            Integer añoDato = (Integer) fila[1];

            if ((mes == null || mesDato.equals(mes)) &&
                    (año == null || añoDato.equals(año))) {

                Map<String, Object> mesData = new HashMap<>();
                mesData.put("mes", mesDato);
                mesData.put("año", añoDato);
                mesData.put("totalVentas", fila[2]);
                mesData.put("totalEgresos", fila[3]);

                datosMensuales.add(mesData);

                if (fila[2] != null) {
                    totalVentasMensual = totalVentasMensual.add((BigDecimal) fila[2]);
                }
                if (fila[3] != null) {
                    totalEgresosMensual = totalEgresosMensual.add((BigDecimal) fila[3]);
                }
            }
        }

        reporte.put("mes", mes);
        reporte.put("año", año);
        reporte.put("datosMensuales", datosMensuales);
        reporte.put("totalVentasMensual", totalVentasMensual);
        reporte.put("totalEgresosMensual", totalEgresosMensual);
        reporte.put("utilidadNeta", totalVentasMensual.subtract(totalEgresosMensual));
        reporte.put("fechaGeneracion", LocalDateTime.now());

        return reporte;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticasCajas() {
        Map<String, Object> estadisticas = new HashMap<>();

        List<Caja> cajas = cajaRepository.findAll();
        List<CierreCaja> cierres = cierreCajaRepository.findAll();

        // Estadísticas básicas
        estadisticas.put("totalCajas", cajas.size());
        estadisticas.put("totalCierres", cierres.size());

        // Ventas totales históricas
        BigDecimal ventasTotalesHistoricas = cierres.stream()
                .map(CierreCaja::getTotalVentas)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        estadisticas.put("ventasTotalesHistoricas", ventasTotalesHistoricas);

        // Caja con más ventas
        Map<Long, BigDecimal> ventasPorCaja = new HashMap<>();
        for (CierreCaja cierre : cierres) {
            Long cajaId = cierre.getCaja().getId();
            BigDecimal ventas = cierre.getTotalVentas() != null ? cierre.getTotalVentas() : BigDecimal.ZERO;
            ventasPorCaja.put(cajaId, ventasPorCaja.getOrDefault(cajaId, BigDecimal.ZERO).add(ventas));
        }

        if (!ventasPorCaja.isEmpty()) {
            Map.Entry<Long, BigDecimal> maxEntry = Collections.max(
                    ventasPorCaja.entrySet(), Map.Entry.comparingByValue());

            Caja cajaTop = cajaRepository.findById(maxEntry.getKey()).orElse(null);
            if (cajaTop != null) {
                Map<String, Object> cajaTopInfo = new HashMap<>();
                cajaTopInfo.put("cajaId", cajaTop.getId());
                cajaTopInfo.put("cajaNombre", cajaTop.getNombre());
                cajaTopInfo.put("totalVentas", maxEntry.getValue());
                estadisticas.put("cajaTopVentas", cajaTopInfo);
            }
        }

        // Diferencias promedio
        BigDecimal diferenciaPromedio = cierres.stream()
                .map(CierreCaja::getDiferencia)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(cierres.size()), 2, java.math.RoundingMode.HALF_UP);
        estadisticas.put("diferenciaPromedio", diferenciaPromedio);

        // Cierres por estado
        Map<String, Long> cierresPorEstado = cierres.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getEstado().name(),
                        Collectors.counting()
                ));
        estadisticas.put("cierresPorEstado", cierresPorEstado);

        estadisticas.put("fechaActualizacion", LocalDateTime.now());

        return estadisticas;
    }

    // ============ VALIDACIONES ============

    @Override
    @Transactional(readOnly = true)
    public boolean existeCajaPorCodigo(String codigo) {
        return cajaRepository.existsByCodigo(codigo);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verificarCajaAbierta(Long cajaId) {
        Caja caja = obtenerEntidadCaja(cajaId);
        return caja.estaAbierta();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verificarUsuarioTieneCajaAbierta(Long usuarioId) {
        return cajaRepository.findCajaAbiertaPorUsuario(usuarioId).isPresent();
    }

    // ============ MÉTODOS INTERNOS ============

    @Override
    @Transactional(readOnly = true)
    public Caja obtenerEntidadCaja(Long id) {
        return cajaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caja no encontrada con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Caja obtenerCajaAbiertaEntidad(Long cajaId) {
        Caja caja = obtenerEntidadCaja(cajaId);
        if (!caja.estaAbierta()) {
            throw new ValidacionException("La caja no está abierta");
        }
        return caja;
    }

    @Override
    @Transactional
    public void actualizarSaldoCaja(Long cajaId, BigDecimal monto, String tipoOperacion) {
        Caja caja = obtenerCajaAbiertaEntidad(cajaId);

        switch (tipoOperacion.toUpperCase()) {
            case "VENTA":
                caja.setSaldoActual(caja.getSaldoActual().add(monto));
                break;
            case "EGRESO":
                caja.setSaldoActual(caja.getSaldoActual().subtract(monto));
                break;
            default:
                throw new ValidacionException("Tipo de operación no válido: " + tipoOperacion);
        }

        cajaRepository.save(caja);

        log.debug("Saldo actualizado caja {}: {} - Nuevo saldo: {}",
                caja.getNombre(), tipoOperacion, caja.getSaldoActual());
    }

    // ============ MÉTODOS PRIVADOS AUXILIARES ============

    private Usuario obtenerUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private CierreCaja obtenerCierre(Long cierreId) {
        return cierreCajaRepository.findById(cierreId)
                .orElseThrow(() -> new ResourceNotFoundException("Cierre de caja no encontrado"));
    }

    private CajaResponse convertirAResponse(Caja caja) {
        // Calcular estadísticas
        Integer totalVentasHoy = 0;
        BigDecimal totalVentasHoyMonto = BigDecimal.ZERO;
        if (caja.estaAbierta()) {
            List<Venta> ventasHoy = ventaRepository.findByFecha(LocalDate.now());
            ventasHoy = ventasHoy.stream()
                    .filter(v -> v.getCaja() != null && v.getCaja().getId().equals(caja.getId()))
                    .collect(Collectors.toList());

            totalVentasHoy = ventasHoy.size();
            totalVentasHoyMonto = ventasHoy.stream()
                    .map(Venta::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // Obtener último cierre
        LocalDateTime ultimoCierre = null;
        List<CierreCaja> cierres = cierreCajaRepository.findByCajaId(caja.getId());
        if (!cierres.isEmpty()) {
            ultimoCierre = cierres.get(cierres.size() - 1).getCreadoEn();
        }

        return CajaResponse.builder()
                .id(caja.getId())
                .codigo(caja.getCodigo())
                .nombre(caja.getNombre())
                .descripcion(caja.getDescripcion())
                .sucursalId(caja.getSucursal() != null ? caja.getSucursal().getId() : null)
                .sucursalNombre(caja.getSucursal() != null ? caja.getSucursal().getNombre() : null)
                .saldoInicial(caja.getSaldoInicial())
                .saldoActual(caja.getSaldoActual())
                .estado(caja.getEstado().name())
                .fechaApertura(caja.getFechaApertura())
                .fechaCierre(caja.getFechaCierre())
                .usuarioAsignadoId(caja.getUsuarioAsignado() != null ? caja.getUsuarioAsignado().getId() : null)
                .usuarioAsignadoNombre(caja.getUsuarioAsignado() != null ?
                        caja.getUsuarioAsignado().getNombreCompleto() : null)
                .totalVentasHoy(totalVentasHoy)
                .totalVentasHoyMonto(totalVentasHoyMonto)
                .totalCierres(cierres.size())
                .ultimoCierre(ultimoCierre)
                .creadoEn(caja.getCreadoEn())
                .actualizadoEn(caja.getActualizadoEn())
                .build();
    }

    private CierreCajaResponse convertirCierreAResponse(CierreCaja cierre) {
        return CierreCajaResponse.builder()
                .id(cierre.getId())
                .cajaId(cierre.getCaja().getId())
                .cajaNombre(cierre.getCaja().getNombre())
                .cajaCodigo(cierre.getCaja().getCodigo())
                .fechaCierre(cierre.getFechaCierre())
                .horaApertura(cierre.getHoraApertura())
                .horaCierre(cierre.getHoraCierre())
                .usuarioId(cierre.getUsuario().getId())
                .usuarioNombre(cierre.getUsuario().getNombreCompleto())
                .saldoInicial(cierre.getSaldoInicial())
                .saldoFinalTeorico(cierre.getSaldoFinalTeorico())
                .saldoFinalReal(cierre.getSaldoFinalReal())
                .diferencia(cierre.getDiferencia())
                .totalVentas(cierre.getTotalVentas())
                .totalCompras(cierre.getTotalCompras())
                .totalGastos(cierre.getTotalGastos())
                .totalIngresos(cierre.getTotalIngresos())
                .totalEgresos(cierre.getTotalEgresos())
                .efectivo(cierre.getEfectivo())
                .tarjetas(cierre.getTarjetas())
                .transferencias(cierre.getTransferencias())
                .otrosMedios(cierre.getOtrosMedios())
                .estado(cierre.getEstado().name())
                .observaciones(cierre.getObservaciones())
                .conciliadoPorId(cierre.getConciliadoPor() != null ? cierre.getConciliadoPor().getId() : null)
                .conciliadoPorNombre(cierre.getConciliadoPor() != null ?
                        cierre.getConciliadoPor().getNombreCompleto() : null)
                .fechaConciliacion(cierre.getFechaConciliacion())
                .creadoEn(cierre.getCreadoEn())
                .build();
    }

    private Map<String, BigDecimal> calcularVentasPorHora(List<Venta> ventas) {
        Map<String, BigDecimal> ventasPorHora = new HashMap<>();

        for (Venta venta : ventas) {
            String hora = venta.getFechaEmision().getHour() + ":00";
            BigDecimal total = venta.getTotal() != null ? venta.getTotal() : BigDecimal.ZERO;

            ventasPorHora.put(hora,
                    ventasPorHora.getOrDefault(hora, BigDecimal.ZERO).add(total));
        }

        return ventasPorHora;
    }
}