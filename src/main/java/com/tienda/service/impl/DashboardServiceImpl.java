package com.tienda.service.impl;

import com.tienda.dto.EstadisticasDTO;
import com.tienda.dto.response.DashboardResponse;
import com.tienda.model.*;
import com.tienda.repository.*;
import com.tienda.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final CierreCajaRepository cierreCajaRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse obtenerMetricasPrincipales() {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate finMes = hoy.with(TemporalAdjusters.lastDayOfMonth());

        return obtenerMetricasPrincipalesPorFecha(inicioMes, finMes);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse obtenerMetricasPrincipalesPorFecha(LocalDate fechaInicio, LocalDate fechaFin) {

        // ✅ CORREGIDO: Pasar 'fechaInicio' y 'fechaFin' a calcularEstadisticasPeriodo
        LocalDate hoy = LocalDate.now(); // ✅ Agregada esta variable

        // 1. Obtener estadísticas básicas
        EstadisticasDTO estadisticas = calcularEstadisticasPeriodo(fechaInicio, fechaFin);

        // 2. Calcular métricas principales
        DashboardResponse.MetricasPrincipales metricas = calcularMetricasPrincipales(estadisticas);

        // 3. Obtener resumen general
        DashboardResponse.ResumenGeneral resumen = calcularResumenGeneral(hoy); // ✅ Usando 'hoy'

        // 4. Obtener datos para gráficos
        DashboardResponse.DatosGraficos graficos = obtenerDatosGraficos(fechaInicio, fechaFin);

        // 5. Obtener top listas
        DashboardResponse.TopListas tops = obtenerTopListas();

        // 6. Obtener alertas
        List<DashboardResponse.AlertaDashboard> alertas = obtenerAlertasDashboard();

        // 7. Calcular tendencias
        DashboardResponse.Tendencia tendencia = calcularTendencias(fechaInicio, fechaFin);

        return DashboardResponse.builder()
                .resumen(resumen)
                .metricas(metricas)
                .graficos(graficos)
                .tops(tops)
                .alertas(alertas)
                .tendencia(tendencia)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerMetricasVentas(LocalDate fechaInicio, LocalDate fechaFin) {
        Map<String, Object> metricas = new HashMap<>();

        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(LocalTime.MAX);

        // Ventas del periodo
        List<Venta> ventas = ventaRepository.findByFechaEmisionBetween(inicio, fin);
        List<Venta> ventasCompletadas = ventas.stream()
                .filter(v -> v.getEstado() == Venta.EstadoVenta.COMPLETADA)
                .collect(Collectors.toList());

        // Cálculos básicos
        BigDecimal ventasTotales = ventasCompletadas.stream()
                .map(Venta::getTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Integer cantidadVentas = ventasCompletadas.size();
        BigDecimal ticketPromedio = cantidadVentas > 0 ?
                ventasTotales.divide(new BigDecimal(cantidadVentas), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        // Ventas por forma de pago
        Map<String, BigDecimal> ventasPorFormaPago = ventasCompletadas.stream()
                .collect(Collectors.groupingBy(
                        Venta::getFormaPago,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Venta::getTotal,
                                BigDecimal::add
                        )
                ));

        // Ventas por día
        Map<LocalDate, BigDecimal> ventasPorDia = ventasCompletadas.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getFechaEmision().toLocalDate(),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Venta::getTotal,
                                BigDecimal::add
                        )
                ));

        // Productos más vendidos
        List<Object[]> productosMasVendidos = detalleVentaRepository.findProductosMasVendidos();

        metricas.put("ventasTotales", ventasTotales);
        metricas.put("cantidadVentas", cantidadVentas);
        metricas.put("ticketPromedio", ticketPromedio);
        metricas.put("ventasPorFormaPago", ventasPorFormaPago);
        metricas.put("ventasPorDia", ventasPorDia);
        metricas.put("productosMasVendidos", productosMasVendidos);
        metricas.put("ventasAnuladas", ventas.stream()
                .filter(v -> v.getEstado() == Venta.EstadoVenta.ANULADA)
                .count());
        metricas.put("ventasPendientes", ventas.stream()
                .filter(v -> v.getEstado() == Venta.EstadoVenta.PENDIENTE)
                .count());

        return metricas;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerMetricasInventario() {
        Map<String, Object> metricas = new HashMap<>();

        List<Producto> productos = productoRepository.findAll();
        List<Producto> productosActivos = productos.stream()
                .filter(p -> p.getEstado() == Producto.EstadoProducto.ACTIVO)
                .collect(Collectors.toList());

        // Valoración del inventario
        BigDecimal valorCostoTotal = productosActivos.stream()
                .map(p -> p.getStockActual().multiply(
                        p.getCostoPromedio() != null ? p.getCostoPromedio() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal valorVentaTotal = productosActivos.stream()
                .map(p -> p.getStockActual().multiply(p.getPrecioVenta()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Estados de stock
        long productosBajoStock = productosActivos.stream()
                .filter(p -> p.getStockActual().compareTo(p.getStockMinimo()) <= 0)
                .count();

        long productosAgotados = productosActivos.stream()
                .filter(p -> p.getStockActual().compareTo(BigDecimal.ZERO) <= 0)
                .count();

        long productosSobreStock = productosActivos.stream()
                .filter(p -> p.getStockMaximo() != null &&
                        p.getStockActual().compareTo(p.getStockMaximo()) > 0)
                .count();

        // ✅ CORREGIDO: Usar elnuevo métodocalcularVentasPorPeriodo
        LocalDate hace30Dias = LocalDate.now().minusDays(30);
        BigDecimal ventasUltimos30Dias = ventaRepository.calcularVentasPorPeriodo(
                hace30Dias.atStartOfDay(),
                LocalDateTime.now()
        );

        if (ventasUltimos30Dias == null) {
            ventasUltimos30Dias = BigDecimal.ZERO;
        }

        BigDecimal rotacion = valorCostoTotal.compareTo(BigDecimal.ZERO) > 0 ?
                ventasUltimos30Dias.divide(valorCostoTotal, 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        metricas.put("totalProductos", productosActivos.size());
        metricas.put("valorCostoTotal", valorCostoTotal);
        metricas.put("valorVentaTotal", valorVentaTotal);
        metricas.put("productosBajoStock", productosBajoStock);
        metricas.put("productosAgotados", productosAgotados);
        metricas.put("productosSobreStock", productosSobreStock);
        metricas.put("rotacionInventario", rotacion);
        metricas.put("utilidadPotencial", valorVentaTotal.subtract(valorCostoTotal));

        return metricas;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerMetricasClientes() {
        Map<String, Object> metricas = new HashMap<>();

        List<Cliente> clientes = clienteRepository.findAll();
        List<Cliente> clientesActivos = clientes.stream()
                .filter(c -> "ACTIVO".equals(c.getEstado()))
                .collect(Collectors.toList());

        // Nuevos clientes este mes
        LocalDate inicioMes = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
        long nuevosClientesMes = clientesActivos.stream()
                .filter(c -> c.getFechaRegistro() != null &&
                        c.getFechaRegistro().toLocalDate().isAfter(inicioMes.minusDays(1)))
                .count();

        // Clientes inactivos (sin compras en 60 días)
        LocalDate fechaLimiteInactivos = LocalDate.now().minusDays(60);
        long clientesInactivos = clientesActivos.stream()
                .filter(c -> c.getUltimaCompra() != null &&
                        c.getUltimaCompra().toLocalDate().isBefore(fechaLimiteInactivos))
                .count();

        // Segmentación por tipo
        Map<String, Long> clientesPorTipo = clientesActivos.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getTipo() != null ? c.getTipo().name() : "SIN_TIPO",
                        Collectors.counting()
                ));

        // Top clientes por compras
        // ✅ CORREGIDO: Usar PageRequest correctamente
        PageRequest pageRequest = PageRequest.of(0, 10);
        List<Cliente> topClientes = clienteRepository.findTopClientes(pageRequest).getContent();

        metricas.put("totalClientes", clientesActivos.size());
        metricas.put("nuevosClientesMes", nuevosClientesMes);
        metricas.put("clientesInactivos", clientesInactivos);
        metricas.put("clientesPorTipo", clientesPorTipo);
        metricas.put("tasaRetencion", calcularTasaRetencion());
        metricas.put("topClientes", topClientes.stream()
                .map(this::convertirClienteAMapa)
                .collect(Collectors.toList()));

        return metricas;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerMetricasFinancieras(LocalDate fechaInicio, LocalDate fechaFin) {
        Map<String, Object> metricas = new HashMap<>();

        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(LocalTime.MAX);

        // Ventas del periodo
        List<Venta> ventas = ventaRepository.findByFechaEmisionBetween(inicio, fin);
        List<Venta> ventasCompletadas = ventas.stream()
                .filter(v -> v.getEstado() == Venta.EstadoVenta.COMPLETADA)
                .collect(Collectors.toList());

        BigDecimal ventasTotales = ventasCompletadas.stream()
                .map(Venta::getTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calcular costo de ventas
        BigDecimal costoVentas = ventasCompletadas.stream()
                .flatMap(v -> v.getDetalles().stream())
                .map(d -> d.getCantidad().multiply(
                        d.getCostoUnitario() != null ? d.getCostoUnitario() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal utilidadBruta = ventasTotales.subtract(costoVentas);
        BigDecimal margenUtilidad = ventasTotales.compareTo(BigDecimal.ZERO) > 0 ?
                utilidadBruta.divide(ventasTotales, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100")) :
                BigDecimal.ZERO;

        // Cierres de caja del periodo
        // ✅ CORREGIDO: Usar findByFechaBetween que agregamos
        List<CierreCaja> cierres = cierreCajaRepository.findByFechaBetween(fechaInicio, fechaFin);
        BigDecimal totalEgresos = cierres.stream()
                .map(CierreCaja::getTotalEgresos)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalIngresos = cierres.stream()
                .map(CierreCaja::getTotalIngresos)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        metricas.put("ventasTotales", ventasTotales);
        metricas.put("costoVentas", costoVentas);
        metricas.put("utilidadBruta", utilidadBruta);
        metricas.put("margenUtilidad", margenUtilidad);
        metricas.put("totalEgresos", totalEgresos);
        metricas.put("totalIngresos", totalIngresos);
        metricas.put("utilidadNeta", utilidadBruta.subtract(totalEgresos));
        metricas.put("ventasPorMedioPago", calcularVentasPorMedioPago(ventasCompletadas));

        return metricas;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerVentasPorPeriodo(String periodo, LocalDate fechaInicio, LocalDate fechaFin) {
        Map<String, Object> resultado = new HashMap<>();
        List<Map<String, Object>> datos = new ArrayList<>();

        LocalDate fechaActual = fechaInicio;

        while (!fechaActual.isAfter(fechaFin)) {
            LocalDate fechaFinPeriodo;

            switch (periodo.toUpperCase()) {
                case "DIARIO":
                    fechaFinPeriodo = fechaActual;
                    break;
                case "SEMANAL":
                    fechaFinPeriodo = fechaActual.plusDays(6);
                    break;
                case "MENSUAL":
                    fechaFinPeriodo = fechaActual.with(TemporalAdjusters.lastDayOfMonth());
                    break;
                default:
                    fechaFinPeriodo = fechaActual;
            }

            if (fechaFinPeriodo.isAfter(fechaFin)) {
                fechaFinPeriodo = fechaFin;
            }

            BigDecimal ventasPeriodo = ventaRepository.calcularVentasDelDia(fechaActual);
            if (ventasPeriodo == null) {
                ventasPeriodo = BigDecimal.ZERO;
            }

            Map<String, Object> dato = new HashMap<>();
            dato.put("periodo", fechaActual.toString());
            dato.put("ventas", ventasPeriodo);
            dato.put("fechaInicio", fechaActual.toString());
            dato.put("fechaFin", fechaFinPeriodo.toString());

            datos.add(dato);

            // Avanzar al siguiente periodo
            switch (periodo.toUpperCase()) {
                case "DIARIO":
                    fechaActual = fechaActual.plusDays(1);
                    break;
                case "SEMANAL":
                    fechaActual = fechaActual.plusWeeks(1);
                    break;
                case "MENSUAL":
                    fechaActual = fechaActual.plusMonths(1);
                    break;
                default:
                    fechaActual = fechaActual.plusDays(1);
            }
        }

        resultado.put("periodo", periodo);
        resultado.put("datos", datos);
        resultado.put("totalVentas", datos.stream()
                .map(d -> (BigDecimal) d.get("ventas"))
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerTendenciaVentas(int dias) {
        Map<String, Object> tendencia = new HashMap<>();
        List<Map<String, Object>> datos = new ArrayList<>();

        LocalDate fechaFin = LocalDate.now();
        LocalDate fechaInicio = fechaFin.minusDays(dias - 1);

        BigDecimal ventasTotales = BigDecimal.ZERO;
        BigDecimal ventasPrimeraMitad = BigDecimal.ZERO;
        BigDecimal ventasSegundaMitad = BigDecimal.ZERO;

        LocalDate fechaActual = fechaInicio;
        int contador = 0;
        int mitad = dias / 2;

        while (!fechaActual.isAfter(fechaFin)) {
            BigDecimal ventasDia = ventaRepository.calcularVentasDelDia(fechaActual);
            if (ventasDia == null) {
                ventasDia = BigDecimal.ZERO;
            }

            ventasTotales = ventasTotales.add(ventasDia);

            if (contador < mitad) {
                ventasPrimeraMitad = ventasPrimeraMitad.add(ventasDia);
            } else {
                ventasSegundaMitad = ventasSegundaMitad.add(ventasDia);
            }

            Map<String, Object> dato = new HashMap<>();
            dato.put("fecha", fechaActual.toString());
            dato.put("ventas", ventasDia);
            dato.put("diaSemana", fechaActual.getDayOfWeek().toString());

            datos.add(dato);

            fechaActual = fechaActual.plusDays(1);
            contador++;
        }

        // Calcular crecimiento
        BigDecimal crecimiento = BigDecimal.ZERO;
        String direccion = "NEUTRAL";

        if (ventasPrimeraMitad.compareTo(BigDecimal.ZERO) > 0) {
            crecimiento = ventasSegundaMitad.subtract(ventasPrimeraMitad)
                    .divide(ventasPrimeraMitad, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));

            if (crecimiento.compareTo(new BigDecimal("5")) > 0) {
                direccion = "POSITIVA";
            } else if (crecimiento.compareTo(new BigDecimal("-5")) < 0) {
                direccion = "NEGATIVA";
            }
        }

        tendencia.put("periodoDias", dias);
        tendencia.put("fechaInicio", fechaInicio.toString());
        tendencia.put("fechaFin", fechaFin.toString());
        tendencia.put("ventasTotales", ventasTotales);
        tendencia.put("ventasPromedioDiarias",
                ventasTotales.divide(new BigDecimal(dias), 2, RoundingMode.HALF_UP));
        tendencia.put("crecimiento", crecimiento);
        tendencia.put("direccion", direccion);
        tendencia.put("datos", datos);

        return tendencia;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerTopProductos(int limite) {
        Map<String, Object> resultado = new HashMap<>();

        List<Object[]> productosData = detalleVentaRepository.findProductosMasVendidos();

        List<Map<String, Object>> topProductos = productosData.stream()
                .limit(limite)
                .map(data -> {
                    Map<String, Object> productoMap = new HashMap<>();
                    productoMap.put("id", data[0]);
                    productoMap.put("nombre", data[1]);
                    productoMap.put("cantidadVendida", data[2]);
                    return productoMap;
                })
                .collect(Collectors.toList());

        resultado.put("totalProductos", productosData.size());
        resultado.put("limite", limite);
        resultado.put("productos", topProductos);

        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerTopClientes(int limite) {
        Map<String, Object> resultado = new HashMap<>();

        PageRequest pageRequest = PageRequest.of(0, limite);
        List<Cliente> topClientes = clienteRepository.findTopClientes(pageRequest).getContent();

        List<Map<String, Object>> clientesMap = topClientes.stream()
                .map(this::convertirClienteAMapa)
                .collect(Collectors.toList());

        resultado.put("totalClientes", clienteRepository.count());
        resultado.put("limite", limite);
        resultado.put("clientes", clientesMap);

        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerTopVendedores(int limite) {
        Map<String, Object> resultado = new HashMap<>();

        List<Object[]> ventasPorVendedor = ventaRepository.ventasPorVendedor();

        List<Map<String, Object>> vendedores = ventasPorVendedor.stream()
                .limit(limite)
                .map(data -> {
                    Map<String, Object> vendedorMap = new HashMap<>();
                    vendedorMap.put("nombre", data[0]);
                    vendedorMap.put("ventasTotales", data[1]);
                    return vendedorMap;
                })
                .collect(Collectors.toList());

        resultado.put("totalVendedores", ventasPorVendedor.size());
        resultado.put("limite", limite);
        resultado.put("vendedores", vendedores);

        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerProductosBajoStock(int limite) {
        Map<String, Object> resultado = new HashMap<>();

        List<Producto> productosBajoStock = productoRepository.findProductosStockBajo();

        List<Map<String, Object>> productos = productosBajoStock.stream()
                .limit(limite)
                .map(p -> {
                    Map<String, Object> productoMap = new HashMap<>();
                    productoMap.put("id", p.getId());
                    productoMap.put("codigo", p.getCodigo());
                    productoMap.put("nombre", p.getNombre());
                    productoMap.put("stockActual", p.getStockActual());
                    productoMap.put("stockMinimo", p.getStockMinimo());
                    productoMap.put("alertaStock", p.getAlertaStock().name());
                    productoMap.put("necesitaReorden",
                            p.getStockActual().compareTo(p.getStockMinimo()) <= 0);
                    return productoMap;
                })
                .collect(Collectors.toList());

        resultado.put("totalProductosBajoStock", productosBajoStock.size());
        resultado.put("limite", limite);
        resultado.put("productos", productos);

        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerAlertasSistema() {
        Map<String, Object> alertas = new HashMap<>();
        List<Map<String, Object>> listaAlertas = new ArrayList<>();

        // Alertas de inventario
        List<Producto> productosCriticos = productoRepository.findByAlertaStock(
                Producto.AlertaStock.CRITICO
        );

        for (Producto producto : productosCriticos) {
            Map<String, Object> alerta = new HashMap<>();
            alerta.put("tipo", "INVENTARIO");
            alerta.put("prioridad", "ALTA");
            alerta.put("titulo", "Stock Crítico");
            alerta.put("mensaje", "Producto " + producto.getNombre() +
                    " tiene stock crítico: " + producto.getStockActual());
            alerta.put("fecha", LocalDateTime.now().toString());
            alerta.put("leida", false);
            alerta.put("accion", "/inventario/ajustar/" + producto.getId());
            listaAlertas.add(alerta);
        }

        // Alertas de ventas (si no hay ventas hoy)
        LocalDate hoy = LocalDate.now();
        BigDecimal ventasHoy = ventaRepository.calcularVentasDelDia(hoy);
        if (ventasHoy == null || ventasHoy.compareTo(BigDecimal.ZERO) == 0) {
            Map<String, Object> alerta = new HashMap<>();
            alerta.put("tipo", "VENTAS");
            alerta.put("prioridad", "MEDIA");
            alerta.put("titulo", "Sin ventas hoy");
            alerta.put("mensaje", "No se han registrado ventas hoy");
            alerta.put("fecha", LocalDateTime.now().toString());
            alerta.put("leida", false);
            alerta.put("accion", "/ventas/nueva");
            listaAlertas.add(alerta);
        }

        alertas.put("totalAlertas", listaAlertas.size());
        alertas.put("alertasNoLeidas", listaAlertas.stream()
                .filter(a -> !(Boolean) a.get("leida"))
                .count());
        alertas.put("alertas", listaAlertas);

        return alertas;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerMetricasTiempoReal() {
        Map<String, Object> metricas = new HashMap<>();
        LocalDateTime ahora = LocalDateTime.now();

        // Ventas hoy
        LocalDate hoy = LocalDate.now();
        BigDecimal ventasHoy = ventaRepository.calcularVentasDelDia(hoy);
        if (ventasHoy == null) {
            ventasHoy = BigDecimal.ZERO;
        }

        // Ventas última hora
        LocalDateTime haceUnaHora = ahora.minusHours(1);
        List<Venta> ventasUltimaHora = ventaRepository.findByFechaEmisionBetween(haceUnaHora, ahora);
        BigDecimal ventasUltimaHoraTotal = ventasUltimaHora.stream()
                .filter(v -> v.getEstado() == Venta.EstadoVenta.COMPLETADA)
                .map(Venta::getTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Productos vendidos hoy
        Integer productosVendidosHoy = detalleVentaRepository.findDetallesRecientes(
                        hoy.atStartOfDay()
                ).stream()
                .map(DetalleVenta::getCantidad)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .intValue();

        // Clientes atendidos hoy
        Long clientesAtendidosHoy = ventaRepository.findByFecha(hoy).stream()
                .map(Venta::getClienteId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        metricas.put("timestamp", ahora.toString());
        metricas.put("ventasHoy", ventasHoy);
        metricas.put("ventasUltimaHora", ventasUltimaHoraTotal);
        metricas.put("transaccionesUltimaHora", ventasUltimaHora.size());
        metricas.put("productosVendidosHoy", productosVendidosHoy);
        metricas.put("clientesAtendidosHoy", clientesAtendidosHoy);
        metricas.put("ticketPromedioHoy", ventasHoy.compareTo(BigDecimal.ZERO) > 0 && clientesAtendidosHoy > 0 ?
                ventasHoy.divide(new BigDecimal(clientesAtendidosHoy), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO);

        return metricas;
    }

    // Métodos privados auxiliares - ✅ CORREGIDO: Agregar parámetros
    private EstadisticasDTO calcularEstadisticasPeriodo(LocalDate fechaInicio, LocalDate fechaFin) {
        EstadisticasDTO estadisticas = new EstadisticasDTO();

        // ✅ CORREGIDO: Usar setters correctos
        estadisticas.setFechaInicio(fechaInicio);
        estadisticas.setFechaFin(fechaFin);

        // Obtener métricas de ventas para el periodo
        Map<String, Object> metricasVentas = obtenerMetricasVentas(fechaInicio, fechaFin);

        estadisticas.setVentasTotales((BigDecimal) metricasVentas.get("ventasTotales"));
        estadisticas.setCantidadVentas((Integer) metricasVentas.get("cantidadVentas"));
        estadisticas.setTicketPromedio((BigDecimal) metricasVentas.get("ticketPromedio"));

        // Obtener métricas de clientes
        Map<String, Object> metricasClientes = obtenerMetricasClientes();
        estadisticas.setClientesAtendidos((Integer) metricasClientes.get("totalClientes"));

        // Calcular utilidad
        Map<String, Object> metricasFinancieras = obtenerMetricasFinancieras(fechaInicio, fechaFin);
        estadisticas.setUtilidadBruta((BigDecimal) metricasFinancieras.get("utilidadBruta"));
        estadisticas.setMargenUtilidad((BigDecimal) metricasFinancieras.get("margenUtilidad"));

        return estadisticas;
    }

    private DashboardResponse.MetricasPrincipales calcularMetricasPrincipales(EstadisticasDTO estadisticas) {
        return DashboardResponse.MetricasPrincipales.builder()
                .ventasTotales(estadisticas.getVentasTotales())
                .ventasPromedioDiarias(calcularVentasPromedioDiarias(estadisticas))
                .ticketPromedio(estadisticas.getTicketPromedio())
                .transaccionesTotales(estadisticas.getCantidadVentas())
                .totalProductos((int) productoRepository.count())
                .productosBajoStock(productoRepository.findProductosStockBajo().size())
                .productosAgotados(productoRepository.findProductosAgotados().size())
                .valorInventario(calcularValorInventario())
                .totalClientes((int) clienteRepository.count())
                .nuevosClientes(calcularNuevosClientesMes())
                .tasaRetencion(calcularTasaRetencion())
                .utilidadBruta(estadisticas.getUtilidadBruta())
                .margenUtilidad(estadisticas.getMargenUtilidad())
                .gastosTotales(BigDecimal.ZERO) // Implementar según tu sistema
                .ingresosTotales(estadisticas.getVentasTotales())
                .build();
    }

    private DashboardResponse.ResumenGeneral calcularResumenGeneral(LocalDate hoy) {
        BigDecimal ventasHoy = ventaRepository.calcularVentasDelDia(hoy);
        if (ventasHoy == null) {
            ventasHoy = BigDecimal.ZERO;
        }

        return DashboardResponse.ResumenGeneral.builder()
                .fechaConsulta(hoy)
                .periodo("HOY")
                .totalVentasHoy(ventaRepository.findByFecha(hoy).size())
                .ventasHoy(ventasHoy)
                .nuevosClientesHoy(calcularNuevosClientesHoy())
                .productosVendidosHoy(calcularProductosVendidosHoy())
                .alertasActivas((Integer) obtenerAlertasSistema().get("totalAlertas"))
                .build();
    }

    private DashboardResponse.DatosGraficos obtenerDatosGraficos(LocalDate fechaInicio, LocalDate fechaFin) {
        // Implementar según tus necesidades de gráficos
        return DashboardResponse.DatosGraficos.builder()
                .ventasPorDia(new ArrayList<>())
                .ventasPorCategoria(new ArrayList<>())
                .ventasPorFormaPago(new ArrayList<>())
                .tendenciaVentas(new ArrayList<>())
                .productosMasVendidos(new ArrayList<>())
                .estadoInventario(new ArrayList<>())
                .build();
    }

    private DashboardResponse.TopListas obtenerTopListas() {
        return DashboardResponse.TopListas.builder()
                .topProductos((List<Map<String, Object>>) obtenerTopProductos(10).get("productos"))
                .topClientes((List<Map<String, Object>>) obtenerTopClientes(10).get("clientes"))
                .topVendedores((List<Map<String, Object>>) obtenerTopVendedores(5).get("vendedores"))
                .productosParaReorden((List<Map<String, Object>>) obtenerProductosBajoStock(10).get("productos"))
                .build();
    }

    private List<DashboardResponse.AlertaDashboard> obtenerAlertasDashboard() {
        List<DashboardResponse.AlertaDashboard> alertas = new ArrayList<>();

        // Alertas de stock crítico
        productoRepository.findByAlertaStock(Producto.AlertaStock.CRITICO)
                .forEach(producto -> {
                    alertas.add(DashboardResponse.AlertaDashboard.builder()
                            .tipo("INVENTARIO")
                            .titulo("Stock Crítico")
                            .mensaje("Producto " + producto.getNombre() + " en stock crítico")
                            .prioridad("ALTA")
                            .fecha(LocalDateTime.now().toString())
                            .leida(false)
                            .accion("/productos/" + producto.getId())
                            .build());
                });

        // Alertas de productos agotados
        productoRepository.findProductosAgotados().stream()
                .limit(5)
                .forEach(producto -> {
                    alertas.add(DashboardResponse.AlertaDashboard.builder()
                            .tipo("INVENTARIO")
                            .titulo("Producto Agotado")
                            .mensaje("Producto " + producto.getNombre() + " está agotado")
                            .prioridad("MEDIA")
                            .fecha(LocalDateTime.now().toString())
                            .leida(false)
                            .accion("/inventario/reabastecer/" + producto.getId())
                            .build());
                });

        return alertas;
    }

    private DashboardResponse.Tendencia calcularTendencias(LocalDate fechaInicio, LocalDate fechaFin) {
        // Obtener datos del periodo actual
        Map<String, Object> metricasActual = obtenerMetricasVentas(fechaInicio, fechaFin);
        BigDecimal ventasActual = (BigDecimal) metricasActual.get("ventasTotales");

        // Obtener datos del periodo anterior (misma duración)
        long dias = fechaInicio.until(fechaFin).getDays();
        LocalDate fechaInicioAnterior = fechaInicio.minusDays(dias);
        LocalDate fechaFinAnterior = fechaInicio.minusDays(1);

        Map<String, Object> metricasAnterior = obtenerMetricasVentas(fechaInicioAnterior, fechaFinAnterior);
        BigDecimal ventasAnterior = (BigDecimal) metricasAnterior.get("ventasTotales");

        // Calcular crecimiento
        BigDecimal crecimientoVentas = BigDecimal.ZERO;
        String tendenciaVentas = "NEUTRAL";

        if (ventasAnterior != null && ventasAnterior.compareTo(BigDecimal.ZERO) > 0) {
            crecimientoVentas = ventasActual.subtract(ventasAnterior)
                    .divide(ventasAnterior, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));

            if (crecimientoVentas.compareTo(new BigDecimal("10")) > 0) {
                tendenciaVentas = "POSITIVA";
            } else if (crecimientoVentas.compareTo(new BigDecimal("-10")) < 0) {
                tendenciaVentas = "NEGATIVA";
            }
        }

        return DashboardResponse.Tendencia.builder()
                .crecimientoVentas(crecimientoVentas)
                .crecimientoClientes(BigDecimal.ZERO) // Implementar
                .crecimientoUtilidad(BigDecimal.ZERO) // Implementar
                .tendenciaVentas(tendenciaVentas)
                .tendenciaInventario("ESTABLE") // Implementar
                .tendenciaClientes("ESTABLE") // Implementar
                .build();
    }

    // Métodos auxiliares de cálculo
    private BigDecimal calcularVentasPromedioDiarias(EstadisticasDTO estadisticas) {
        if (estadisticas.getFechaInicio() != null && estadisticas.getFechaFin() != null) {
            long dias = estadisticas.getFechaInicio().until(estadisticas.getFechaFin()).getDays() + 1;
            if (dias > 0 && estadisticas.getVentasTotales() != null) {
                return estadisticas.getVentasTotales()
                        .divide(new BigDecimal(dias), 2, RoundingMode.HALF_UP);
            }
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calcularValorInventario() {
        return productoRepository.findAll().stream()
                .filter(p -> p.getEstado() == Producto.EstadoProducto.ACTIVO)
                .map(p -> p.getStockActual().multiply(
                        p.getCostoPromedio() != null ? p.getCostoPromedio() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Integer calcularNuevosClientesMes() {
        LocalDate inicioMes = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
        return (int) clienteRepository.findAll().stream()
                .filter(c -> c.getFechaRegistro() != null &&
                        c.getFechaRegistro().toLocalDate().isAfter(inicioMes.minusDays(1)))
                .count();
    }

    private Integer calcularNuevosClientesHoy() {
        LocalDate hoy = LocalDate.now();
        return (int) clienteRepository.findAll().stream()
                .filter(c -> c.getFechaRegistro() != null &&
                        c.getFechaRegistro().toLocalDate().equals(hoy))
                .count();
    }

    private Integer calcularProductosVendidosHoy() {
        LocalDate hoy = LocalDate.now();
        return detalleVentaRepository.findDetallesRecientes(hoy.atStartOfDay()).stream()
                .map(DetalleVenta::getCantidad)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .intValue();
    }

    private BigDecimal calcularTasaRetencion() {
        long totalClientes = clienteRepository.count();
        long clientesConMasDeUnaCompra = clienteRepository.findAll().stream()
                .filter(c -> c.getTotalCompras() != null &&
                        c.getTotalCompras().compareTo(new BigDecimal("0")) > 0)
                .count();

        if (totalClientes > 0) {
            return new BigDecimal(clientesConMasDeUnaCompra)
                    .divide(new BigDecimal(totalClientes), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
        return BigDecimal.ZERO;
    }

    private Map<String, BigDecimal> calcularVentasPorMedioPago(List<Venta> ventas) {
        return ventas.stream()
                .collect(Collectors.groupingBy(
                        Venta::getFormaPago,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Venta::getTotal,
                                BigDecimal::add
                        )
                ));
    }

    private Map<String, Object> convertirClienteAMapa(Cliente cliente) {
        Map<String, Object> clienteMap = new HashMap<>();
        clienteMap.put("id", cliente.getId());
        clienteMap.put("nombre", cliente.getNombre());
        clienteMap.put("documento", cliente.getNumeroDocumento());
        clienteMap.put("email", cliente.getEmail());
        clienteMap.put("telefono", cliente.getTelefono());
        clienteMap.put("totalCompras", cliente.getTotalCompras());
        clienteMap.put("ultimaCompra", cliente.getUltimaCompra());
        clienteMap.put("tipo", cliente.getTipo() != null ? cliente.getTipo().name() : "OCASIONAL");
        return clienteMap;
    }

    // Métodos de widget (simplificados)
    @Override
    public Map<String, Object> obtenerWidgetMetricasVentas() {
        return obtenerMetricasVentas(
                LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()),
                LocalDate.now()
        );
    }

    @Override
    public Map<String, Object> obtenerWidgetMetricasInventario() {
        return obtenerMetricasInventario();
    }

    @Override
    public Map<String, Object> obtenerWidgetMetricasClientes() {
        return obtenerMetricasClientes();
    }

    @Override
    public Map<String, Object> obtenerWidgetMetricasFinancieras() {
        return obtenerMetricasFinancieras(
                LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()),
                LocalDate.now()
        );
    }

    @Override
    public Map<String, Object> obtenerNotificacionesPendientes() {
        // Implementar según tu sistema de notificaciones
        return new HashMap<>();
    }

    @Override
    public Map<String, Object> obtenerComparativaPeriodos(LocalDate periodoActualInicio,
                                                          LocalDate periodoActualFin,
                                                          LocalDate periodoAnteriorInicio,
                                                          LocalDate periodoAnteriorFin) {
        Map<String, Object> comparativa = new HashMap<>();

        Map<String, Object> metricasActual = obtenerMetricasVentas(periodoActualInicio, periodoActualFin);
        Map<String, Object> metricasAnterior = obtenerMetricasVentas(periodoAnteriorInicio, periodoAnteriorFin);

        BigDecimal ventasActual = (BigDecimal) metricasActual.get("ventasTotales");
        BigDecimal ventasAnterior = (BigDecimal) metricasAnterior.get("ventasTotales");

        BigDecimal crecimiento = BigDecimal.ZERO;
        if (ventasAnterior != null && ventasAnterior.compareTo(BigDecimal.ZERO) > 0) {
            crecimiento = ventasActual.subtract(ventasAnterior)
                    .divide(ventasAnterior, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        comparativa.put("periodoActual", metricasActual);
        comparativa.put("periodoAnterior", metricasAnterior);
        comparativa.put("crecimiento", crecimiento);
        comparativa.put("diferenciaVentas", ventasActual.subtract(ventasAnterior));
        comparativa.put("tendencia", crecimiento.compareTo(BigDecimal.ZERO) > 0 ? "POSITIVA" : "NEGATIVA");

        return comparativa;
    }
}