package com.tienda.service.impl;

import com.tienda.dto.request.FiltroReporteRequest;
import com.tienda.dto.response.*;
import com.tienda.exception.ResourceNotFoundException;
import com.tienda.exception.ValidacionException;
import com.tienda.model.*;
import com.tienda.repository.*;
import com.tienda.service.ReporteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReporteServiceImpl implements ReporteService {

    private final CierreCajaRepository cierreCajaRepository;
    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final CajaRepository cajaRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final CategoriaRepository categoriaRepository;

    // ============ CIERRE DIARIO ============

    @Override
    @Transactional
    public CierreCajaResponse generarCierreDiario(Long cajaId, LocalDate fecha) {
        // Verificar si ya existe cierre para esta caja y fecha
        cierreCajaRepository.findByCajaAndFecha(cajaId, fecha)
                .ifPresent(cierre -> {
                    throw new ValidacionException("Ya existe un cierre para esta caja en la fecha: " + fecha);
                });

        Caja caja = cajaRepository.findById(cajaId)
                .orElseThrow(() -> new ResourceNotFoundException("Caja no encontrada"));

        if (!caja.estaAbierta()) {
            throw new ValidacionException("La caja debe estar abierta para generar cierre");
        }

        // Obtener ventas del día
        List<Venta> ventasDelDia = ventaRepository.findByFecha(fecha).stream()
                .filter(v -> v.getCaja() != null && v.getCaja().getId().equals(cajaId))
                .filter(v -> v.getEstado() == Venta.EstadoVenta.COMPLETADA)
                .collect(Collectors.toList());

        // Calcular totales
        BigDecimal totalVentas = ventasDelDia.stream()
                .map(Venta::getTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calcular por forma de pago
        Map<String, BigDecimal> ventasPorFormaPago = ventasDelDia.stream()
                .collect(Collectors.groupingBy(
                        Venta::getFormaPago,
                        Collectors.reducing(BigDecimal.ZERO, Venta::getTotal, BigDecimal::add)
                ));

        BigDecimal efectivo = ventasPorFormaPago.getOrDefault("EFECTIVO", BigDecimal.ZERO);
        BigDecimal tarjetas = BigDecimal.ZERO;
        BigDecimal transferencias = BigDecimal.ZERO;
        BigDecimal otros = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> entry : ventasPorFormaPago.entrySet()) {
            String formaPago = entry.getKey();
            BigDecimal monto = entry.getValue();

            if (formaPago.contains("TARJETA")) {
                tarjetas = tarjetas.add(monto);
            } else if (formaPago.contains("TRANSFERENCIA")) {
                transferencias = transferencias.add(monto);
            } else if (!"EFECTIVO".equals(formaPago)) {
                otros = otros.add(monto);
            }
        }

        // Calcular saldos teóricos
        BigDecimal saldoInicial = caja.getSaldoInicial() != null ? caja.getSaldoInicial() : BigDecimal.ZERO;
        BigDecimal saldoFinalTeorico = saldoInicial.add(efectivo);

        // Crear cierre
        CierreCaja cierre = CierreCaja.builder()
                .caja(caja)
                .fechaCierre(fecha)
                .horaApertura(caja.getFechaApertura() != null ?
                        caja.getFechaApertura().toLocalTime() : LocalTime.of(8, 0))
                .horaCierre(LocalTime.now())
                .usuario(caja.getUsuarioAsignado())
                .saldoInicial(saldoInicial)
                .saldoFinalTeorico(saldoFinalTeorico)
                .saldoFinalReal(saldoFinalTeorico) // Por defecto igual al teórico
                .totalVentas(totalVentas)
                .efectivo(efectivo)
                .tarjetas(tarjetas)
                .transferencias(transferencias)
                .otrosMedios(otros)
                .estado(CierreCaja.EstadoCierre.PENDIENTE)
                .observaciones("Cierre automático generado el " + LocalDateTime.now())
                .build();

        cierre = cierreCajaRepository.save(cierre);

        // Cerrar la caja
        caja.setEstado(Caja.EstadoCaja.CERRADA);
        caja.setFechaCierre(LocalDateTime.now());
        cajaRepository.save(caja);

        log.info("Cierre diario generado para caja {}: {}", caja.getCodigo(), fecha);

        return convertirCierreAResponse(cierre, ventasPorFormaPago, ventasDelDia.size());
    }

    @Override
    @Transactional(readOnly = true)
    public CierreCajaResponse obtenerCierreDiario(Long cajaId, LocalDate fecha) {
        CierreCaja cierre = cierreCajaRepository.findByCajaAndFecha(cajaId, fecha)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró cierre para la caja " + cajaId + " en la fecha " + fecha));

        return convertirCierreAResponse(cierre, new HashMap<>(), 0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CierreCajaResponse> obtenerHistorialCierres(LocalDate fechaInicio, LocalDate fechaFin) {
        List<CierreCaja> cierres = cierreCajaRepository.findByFechaBetween(fechaInicio, fechaFin);

        return cierres.stream()
                .map(cierre -> convertirCierreAResponse(cierre, new HashMap<>(), 0))
                .collect(Collectors.toList());
    }

    // ============ REPORTES DE VENTAS ============

    @Override
    @Transactional(readOnly = true)
    public ReporteVentaResponse generarReporteVentas(FiltroReporteRequest filtro) {
        LocalDate fechaInicio = filtro.getFechaInicio() != null ?
                filtro.getFechaInicio() : LocalDate.now().minusDays(30);
        LocalDate fechaFin = filtro.getFechaFin() != null ?
                filtro.getFechaFin() : LocalDate.now();

        // Obtener ventas en el período
        List<Venta> ventas = ventaRepository.findByFechaEmisionBetween(
                        fechaInicio.atStartOfDay(),
                        fechaFin.atTime(LocalTime.MAX)
                ).stream()
                .filter(v -> v.getEstado() == Venta.EstadoVenta.COMPLETADA)
                .filter(v -> filtro.getSucursalId() == null ||
                        (v.getSucursal() != null && v.getSucursal().getId().equals(filtro.getSucursalId())))
                .filter(v -> filtro.getVendedorId() == null ||
                        (v.getVendedor() != null && v.getVendedor().getId().equals(filtro.getVendedorId())))
                .collect(Collectors.toList());

        // Calcular totales
        BigDecimal totalVentas = ventas.stream()
                .map(Venta::getTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCosto = calcularCostoVentas(ventas);
        BigDecimal totalUtilidad = totalVentas.subtract(totalCosto);
        BigDecimal margenUtilidad = totalCosto.compareTo(BigDecimal.ZERO) > 0 ?
                totalUtilidad.divide(totalCosto, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100")) : BigDecimal.ZERO;

        // Estadísticas
        int numeroVentas = ventas.size();
        int numeroProductosVendidos = calcularTotalProductosVendidos(ventas);
        long numeroClientes = ventas.stream()
                .map(Venta::getClienteId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        long numeroVendedores = ventas.stream()
                .map(Venta::getVendedorId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        // Promedios
        BigDecimal ticketPromedio = numeroVentas > 0 ?
                totalVentas.divide(new BigDecimal(numeroVentas), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal itemsPorVenta = numeroVentas > 0 ?
                new BigDecimal(numeroProductosVendidos).divide(new BigDecimal(numeroVentas), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        // Ventas por forma de pago
        Map<String, BigDecimal> ventasPorFormaPago = ventas.stream()
                .collect(Collectors.groupingBy(
                        Venta::getFormaPago,
                        Collectors.reducing(BigDecimal.ZERO, Venta::getTotal, BigDecimal::add)
                ));

        // Ventas por vendedor
        Map<String, BigDecimal> ventasPorVendedor = ventas.stream()
                .filter(v -> v.getVendedor() != null)
                .collect(Collectors.groupingBy(
                        v -> v.getVendedor().getNombreCompleto(),
                        Collectors.reducing(BigDecimal.ZERO, Venta::getTotal, BigDecimal::add)
                ));

        // Ventas por hora
        Map<String, BigDecimal> ventasPorHora = ventas.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getFechaEmision().getHour() + ":00",
                        Collectors.reducing(BigDecimal.ZERO, Venta::getTotal, BigDecimal::add)
                ));

        return ReporteVentaResponse.builder()
                .periodo(fechaInicio + " - " + fechaFin)
                .fecha(fechaFin)
                .totalVentas(totalVentas)
                .totalCosto(totalCosto)
                .totalUtilidad(totalUtilidad)
                .margenUtilidad(margenUtilidad)
                .numeroVentas(numeroVentas)
                .numeroProductosVendidos(numeroProductosVendidos)
                .numeroClientes((int) numeroClientes)
                .numeroVendedores((int) numeroVendedores)
                .ticketPromedio(ticketPromedio)
                .itemsPorVenta(itemsPorVenta)
                .ventasPorFormaPago(ventasPorFormaPago)
                .ventasPorVendedor(ventasPorVendedor)
                .ventasPorHora(ventasPorHora)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> generarEstadisticasVentas(LocalDate fechaInicio, LocalDate fechaFin) {
        ReporteVentaResponse reporte = generarReporteVentas(
                new FiltroReporteRequest("VENTAS", fechaInicio, fechaFin, null, null,
                        null, null, null, null, null, null, null, null, null, null, null));

        // Comparar con período anterior
        LocalDate fechaInicioAnterior = fechaInicio.minusDays(
                ChronoUnit.DAYS.between(fechaInicio, fechaFin) + 1);
        LocalDate fechaFinAnterior = fechaInicio.minusDays(1);

        ReporteVentaResponse reporteAnterior = generarReporteVentas(
                new FiltroReporteRequest("VENTAS", fechaInicioAnterior, fechaFinAnterior, null, null,
                        null, null, null, null, null, null, null, null, null, null, null));

        // Calcular crecimiento
        BigDecimal crecimientoVentas = calcularCrecimiento(
                reporte.getTotalVentas(), reporteAnterior.getTotalVentas());
        BigDecimal crecimientoUtilidad = calcularCrecimiento(
                reporte.getTotalUtilidad(), reporteAnterior.getTotalUtilidad());

        Map<String, Object> estadisticas = new HashMap<>();
        estadisticas.put("reporteActual", reporte);
        estadisticas.put("reporteAnterior", reporteAnterior);
        estadisticas.put("crecimientoVentas", crecimientoVentas);
        estadisticas.put("crecimientoUtilidad", crecimientoUtilidad);
        estadisticas.put("diasAnalizados", ChronoUnit.DAYS.between(fechaInicio, fechaFin) + 1);
        estadisticas.put("tendencia", determinarTendencia(crecimientoVentas));

        return estadisticas;
    }

    // ============ REPORTES DE INVENTARIO ============

    @Override
    @Transactional(readOnly = true)
    public ReporteInventarioResponse generarReporteInventario() {
        List<Producto> productos = productoRepository.findAll();

        // Calcular valoración
        BigDecimal valorTotalCosto = BigDecimal.ZERO;
        BigDecimal valorTotalVenta = BigDecimal.ZERO;

        Map<String, Integer> productosPorCategoria = new HashMap<>();
        Map<String, BigDecimal> valorPorCategoria = new HashMap<>();
        Map<String, Integer> alertasPorTipo = new HashMap<>();

        int productosActivos = 0;
        int productosInactivos = 0;
        int productosConStock = 0;
        int productosAgotados = 0;
        int productosCriticos = 0;
        int productosBajoStock = 0;
        int productosSobreStock = 0;
        int productosParaReorden = 0;

        for (Producto producto : productos) {
            // Valoración
            BigDecimal valorCosto = producto.getValorCosto();
            BigDecimal valorVenta = producto.getValorVenta();

            valorTotalCosto = valorTotalCosto.add(valorCosto != null ? valorCosto : BigDecimal.ZERO);
            valorTotalVenta = valorTotalVenta.add(valorVenta != null ? valorVenta : BigDecimal.ZERO);

            // Conteos
            if (producto.getEstado() == Producto.EstadoProducto.ACTIVO) {
                productosActivos++;
            } else {
                productosInactivos++;
            }

            if (producto.getStockActual().compareTo(BigDecimal.ZERO) > 0) {
                productosConStock++;
            }

            // Alertas
            switch (producto.getAlertaStock()) {
                case AGOTADO:
                    productosAgotados++;
                    alertasPorTipo.put("AGOTADO", alertasPorTipo.getOrDefault("AGOTADO", 0) + 1);
                    break;
                case CRITICO:
                    productosCriticos++;
                    alertasPorTipo.put("CRITICO", alertasPorTipo.getOrDefault("CRITICO", 0) + 1);
                    break;
                case BAJO:
                    productosBajoStock++;
                    alertasPorTipo.put("BAJO", alertasPorTipo.getOrDefault("BAJO", 0) + 1);
                    break;
                case SOBRE:
                    productosSobreStock++;
                    alertasPorTipo.put("SOBRE", alertasPorTipo.getOrDefault("SOBRE", 0) + 1);
                    break;
            }

            // Para reorden
            if (producto.getStockActual().compareTo(producto.getStockMinimo()) <= 0 &&
                    producto.getEstado() == Producto.EstadoProducto.ACTIVO) {
                productosParaReorden++;
            }

            // Por categoría
            if (producto.getCategoria() != null) {
                String categoria = producto.getCategoria().getNombre();
                productosPorCategoria.put(categoria,
                        productosPorCategoria.getOrDefault(categoria, 0) + 1);

                valorPorCategoria.put(categoria,
                        valorPorCategoria.getOrDefault(categoria, BigDecimal.ZERO)
                                .add(valorCosto != null ? valorCosto : BigDecimal.ZERO));
            }
        }

        // Calcular utilidad y margen
        BigDecimal utilidadPotencial = valorTotalVenta.subtract(valorTotalCosto);
        BigDecimal margenPromedio = valorTotalCosto.compareTo(BigDecimal.ZERO) > 0 ?
                utilidadPotencial.divide(valorTotalCosto, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100")) : BigDecimal.ZERO;

        return ReporteInventarioResponse.builder()
                .tipoReporte("INVENTARIO_COMPLETO")
                .fechaGeneracion(LocalDateTime.now())
                .valorTotalCosto(valorTotalCosto)
                .valorTotalVenta(valorTotalVenta)
                .utilidadPotencial(utilidadPotencial)
                .margenPromedio(margenPromedio)
                .totalProductos(productos.size())
                .productosActivos(productosActivos)
                .productosInactivos(productosInactivos)
                .productosConStock(productosConStock)
                .productosSinStock(productos.size() - productosConStock)
                .productosAgotados(productosAgotados)
                .productosCriticos(productosCriticos)
                .productosBajoStock(productosBajoStock)
                .productosSobreStock(productosSobreStock)
                .productosParaReorden(productosParaReorden)
                .productosPorCategoria(productosPorCategoria)
                .valorPorCategoria(valorPorCategoria)
                .alertasPorTipo(alertasPorTipo)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReporteInventarioResponse> generarReporteProductosBajoStock() {
        List<Producto> productosBajoStock = productoRepository.findProductosParaReorden();

        return productosBajoStock.stream()
                .map(producto -> ReporteInventarioResponse.builder()
                        .tipoReporte("PRODUCTO_BAJO_STOCK")
                        .fechaGeneracion(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());
    }

    // ============ REPORTES DE UTILIDADES ============

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> generarReporteUtilidades(LocalDate fechaInicio, LocalDate fechaFin) {
        // Obtener ventas del período
        List<Venta> ventas = ventaRepository.findByFechaEmisionBetween(
                        fechaInicio.atStartOfDay(),
                        fechaFin.atTime(LocalTime.MAX)
                ).stream()
                .filter(v -> v.getEstado() == Venta.EstadoVenta.COMPLETADA)
                .collect(Collectors.toList());

        // Calcular utilidades
        BigDecimal totalIngresos = ventas.stream()
                .map(Venta::getTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCosto = calcularCostoVentas(ventas);
        BigDecimal utilidadBruta = totalIngresos.subtract(totalCosto);

        // Calcular gastos (simulado - en producción vendría de otra tabla)
        BigDecimal totalGastos = BigDecimal.ZERO;
        BigDecimal utilidadNeta = utilidadBruta.subtract(totalGastos);

        // Calcular márgenes
        BigDecimal margenBruto = totalIngresos.compareTo(BigDecimal.ZERO) > 0 ?
                utilidadBruta.divide(totalIngresos, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100")) : BigDecimal.ZERO;

        BigDecimal margenNeto = totalIngresos.compareTo(BigDecimal.ZERO) > 0 ?
                utilidadNeta.divide(totalIngresos, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100")) : BigDecimal.ZERO;

        Map<String, Object> reporte = new HashMap<>();
        reporte.put("periodo", fechaInicio + " - " + fechaFin);
        reporte.put("totalIngresos", totalIngresos);
        reporte.put("totalCostoVentas", totalCosto);
        reporte.put("utilidadBruta", utilidadBruta);
        reporte.put("margenBruto", margenBruto);
        reporte.put("totalGastos", totalGastos);
        reporte.put("utilidadNeta", utilidadNeta);
        reporte.put("margenNeto", margenNeto);
        reporte.put("numeroVentas", ventas.size());
        reporte.put("fechaGeneracion", LocalDateTime.now());

        return reporte;
    }

    // ============ MÉTODOS DE EXPORTACIÓN ============

    @Override
    public byte[] exportarCierreDiarioPDF(Long cierreId) {
        // Implementación básica - en producción usarías iText o similar
        log.info("Exportando cierre {} a PDF", cierreId);
        return new byte[0];
    }

    @Override
    public byte[] exportarCierreDiarioExcel(Long cierreId) {
        // Implementación básica - en producción usarías Apache POI
        log.info("Exportando cierre {} a Excel", cierreId);
        return new byte[0];
    }

    @Override
    public byte[] exportarReporteExcel(Map<String, Object> datos, String tipoReporte) {
        log.info("Exportando reporte {} a Excel", tipoReporte);
        return new byte[0];
    }

    @Override
    public byte[] exportarReportePDF(Map<String, Object> datos, String tipoReporte) {
        log.info("Exportando reporte {} a PDF", tipoReporte);
        return new byte[0];
    }

    @Override
    public byte[] exportarReporteCSV(Map<String, Object> datos, String tipoReporte) {
        log.info("Exportando reporte {} a CSV", tipoReporte);
        return new byte[0];
    }

    // ============ MÉTODOS PRIVADOS AUXILIARES ============

    private CierreCajaResponse convertirCierreAResponse(CierreCaja cierre,
                                                        Map<String, BigDecimal> ventasPorFormaPago,
                                                        int numeroVentas) {
        return CierreCajaResponse.builder()
                .id(cierre.getId())
                .cajaNombre(cierre.getCaja() != null ? cierre.getCaja().getNombre() : null)
                .cajaId(cierre.getCaja() != null ? cierre.getCaja().getId() : null)
                .fechaCierre(cierre.getFechaCierre())
                .horaApertura(cierre.getHoraApertura())
                .horaCierre(cierre.getHoraCierre())
                .usuarioNombre(cierre.getUsuario() != null ? cierre.getUsuario().getNombreCompleto() : null)
                .usuarioId(cierre.getUsuario() != null ? cierre.getUsuario().getId() : null)
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
                .estado(cierre.getEstado() != null ? cierre.getEstado().name() : null)
                .observaciones(cierre.getObservaciones())
                .conciliadoPor(cierre.getConciliadoPor() != null ?
                        cierre.getConciliadoPor().getNombreCompleto() : null)
                .fechaConciliacion(cierre.getFechaConciliacion())
                .numeroVentas(numeroVentas)
                .ventasPorFormaPago(ventasPorFormaPago)
                .creadoEn(cierre.getCreadoEn())
                .build();
    }

    private BigDecimal calcularCostoVentas(List<Venta> ventas) {
        BigDecimal totalCosto = BigDecimal.ZERO;

        for (Venta venta : ventas) {
            // Obtener detalles de venta
            List<DetalleVenta> detalles = detalleVentaRepository.findByVentaId(venta.getId());

            for (DetalleVenta detalle : detalles) {
                if (detalle.getCostoUnitario() != null && detalle.getCantidad() != null) {
                    totalCosto = totalCosto.add(
                            detalle.getCostoUnitario().multiply(detalle.getCantidad())
                    );
                }
            }
        }

        return totalCosto;
    }

    private int calcularTotalProductosVendidos(List<Venta> ventas) {
        int total = 0;

        for (Venta venta : ventas) {
            List<DetalleVenta> detalles = detalleVentaRepository.findByVentaId(venta.getId());
            total += detalles.stream()
                    .map(DetalleVenta::getCantidad)
                    .filter(Objects::nonNull)
                    .map(BigDecimal::intValue)
                    .reduce(0, Integer::sum);
        }

        return total;
    }

    private BigDecimal calcularCrecimiento(BigDecimal valorActual, BigDecimal valorAnterior) {
        if (valorAnterior.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return valorActual.subtract(valorAnterior)
                .divide(valorAnterior, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private String determinarTendencia(BigDecimal crecimiento) {
        if (crecimiento.compareTo(new BigDecimal("10")) > 0) {
            return "FUERTE_CRECIMIENTO";
        } else if (crecimiento.compareTo(new BigDecimal("5")) > 0) {
            return "CRECIMIENTO_MODERADO";
        } else if (crecimiento.compareTo(new BigDecimal("0")) > 0) {
            return "CRECIMIENTO_LEVE";
        } else if (crecimiento.compareTo(new BigDecimal("-5")) > 0) {
            return "DECRECIMIENTO_LEVE";
        } else {
            return "FUERTE_DECRECIMIENTO";
        }
    }

    // ============ MÉTODOS NO IMPLEMENTADOS COMPLETAMENTE ============

    @Override
    public List<ReporteVentaResponse> generarReporteVentasPorVendedor(LocalDate fechaInicio, LocalDate fechaFin) {
        // Implementación simplificada
        return new ArrayList<>();
    }

    @Override
    public List<ReporteVentaResponse> generarReporteVentasPorProducto(LocalDate fechaInicio, LocalDate fechaFin) {
        // Implementación simplificada
        return new ArrayList<>();
    }

    @Override
    public List<ReporteVentaResponse> generarReporteVentasPorCategoria(LocalDate fechaInicio, LocalDate fechaFin) {
        // Implementación simplificada
        return new ArrayList<>();
    }

    @Override
    public Map<String, Object> generarReporteMovimientosInventario(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        // Implementación simplificada
        return new HashMap<>();
    }

    @Override
    public List<ReporteInventarioResponse> generarReporteProductosAgotados() {
        // Implementación simplificada
        return new ArrayList<>();
    }

    @Override
    public Map<String, Object> generarReporteRotacionProductos() {
        // Implementación simplificada
        return new HashMap<>();
    }

    @Override
    public Map<String, Object> generarReporteMargenGanancia() {
        // Implementación simplificada
        return new HashMap<>();
    }

    @Override
    public Map<String, Object> generarReporteTopProductosRentables() {
        // Implementación simplificada
        return new HashMap<>();
    }

    @Override
    public Map<String, Object> generarReporteClientes() {
        // Implementación simplificada
        return new HashMap<>();
    }

    @Override
    public List<Map<String, Object>> generarReporteTopClientes(Integer limite) {
        // Implementación simplificada
        return new ArrayList<>();
    }

    @Override
    public Map<String, Object> generarReporteFrecuenciaClientes() {
        // Implementación simplificada
        return new HashMap<>();
    }

    @Override
    public Map<String, Object> generarBalanceGeneral(LocalDate fecha) {
        // Implementación simplificada
        return new HashMap<>();
    }

    @Override
    public Map<String, Object> generarEstadoResultados(LocalDate fechaInicio, LocalDate fechaFin) {
        // Implementación simplificada
        return new HashMap<>();
    }

    @Override
    public Map<String, Object> generarFlujoCaja(LocalDate fechaInicio, LocalDate fechaFin) {
        // Implementación simplificada
        return new HashMap<>();
    }

    @Override
    public Map<String, Object> generarReportePersonalizado(FiltroReporteRequest filtro) {
        // Implementación simplificada
        return new HashMap<>();
    }

    @Override
    public PaginacionResponse<Map<String, Object>> generarReportePaginado(FiltroReporteRequest filtro, Pageable pageable) {
        // Implementación simplificada
        return PaginacionResponse.<Map<String, Object>>builder()
                .content(new ArrayList<>())
                .pageNumber(0)
                .pageSize(10)
                .totalElements(0)
                .totalPages(0)
                .last(true)
                .build();
    }
}