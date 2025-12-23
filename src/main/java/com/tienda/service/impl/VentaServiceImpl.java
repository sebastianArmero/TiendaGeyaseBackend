package com.tienda.service.impl;

import com.tienda.dto.request.VentaRequest;
import com.tienda.dto.response.VentaResponse;
import com.tienda.dto.response.PaginacionResponse;
import com.tienda.dto.response.DetalleVentaResponse;
import com.tienda.exception.ResourceNotFoundException;
import com.tienda.exception.ValidacionException;
import com.tienda.exception.StockInsuficienteException;
import com.tienda.model.*;
import com.tienda.repository.*;
import com.tienda.service.VentaService;
import com.tienda.service.InventarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VentaServiceImpl implements VentaService {

    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final CajaRepository cajaRepository;
    private final InventarioService inventarioService;

    @Override
    @Transactional
    public VentaResponse crearVenta(VentaRequest request) {
        // Validar cliente
        Cliente cliente = null;
        if (request.getClienteId() != null) {
            cliente = clienteRepository.findById(request.getClienteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        }

        // Validar vendedor
        Usuario vendedor = usuarioRepository.findById(request.getVendedorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendedor no encontrado"));

        // Validar caja
        Caja caja = cajaRepository.findById(request.getCajaId())
                .orElseThrow(() -> new ResourceNotFoundException("Caja no encontrada"));

        // Verificar que la caja esté abierta
        if (!caja.estaAbierta()) {
            throw new ValidacionException("La caja no está abierta");
        }

        // Generar número de factura
        String numeroFactura = generarNumeroFactura();

        // Crear venta
        Venta venta = Venta.builder()
                .numeroFactura(numeroFactura)
                .cliente(cliente)
                .clienteNombre(cliente != null ? cliente.getNombre() : "CONSUMIDOR FINAL")
                .clienteDocumento(cliente != null ? cliente.getNumeroDocumento() : null)
                .vendedor(vendedor)
                .vendedorNombre(vendedor.getNombreCompleto())
                .caja(caja)
                .sucursal(caja.getSucursal())
                .formaPago(request.getFormaPago())
                .estadoPago("PAGADO")
                .estado(Venta.EstadoVenta.COMPLETADA)
                .fechaEmision(LocalDateTime.now())
                .build();

        // Procesar detalles
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal descuentoTotal = BigDecimal.ZERO;
        BigDecimal ivaTotal = BigDecimal.ZERO;

        for (VentaRequest.DetalleVentaRequest detalleRequest : request.getDetalles()) {
            Producto producto = productoRepository.findById(detalleRequest.getProductoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Producto no encontrado: " + detalleRequest.getProductoId()));

            // Verificar stock
            if (!producto.tieneStockSuficiente(detalleRequest.getCantidad())) {
                throw new StockInsuficienteException(
                        "Stock insuficiente para producto: " + producto.getNombre() +
                                ". Disponible: " + producto.getStockDisponible() +
                                ", Requerido: " + detalleRequest.getCantidad());
            }

            DetalleVenta detalle = DetalleVenta.builder()
                    .venta(venta)
                    .producto(producto)
                    .codigoProducto(producto.getCodigo())
                    .nombreProducto(producto.getNombre())
                    .unidadMedida(producto.getUnidadMedida())
                    .cantidad(detalleRequest.getCantidad())
                    .precioUnitario(detalleRequest.getPrecioUnitario())
                    .descuentoPorcentaje(detalleRequest.getDescuentoPorcentaje())
                    .ivaPorcentaje(detalleRequest.getIvaPorcentaje())
                    .build();

            detalle.calcularTotales();

            venta.getDetalles().add(detalle);

            subtotal = subtotal.add(detalle.getSubtotal());
            descuentoTotal = descuentoTotal.add(detalle.getDescuentoUnitario());
            ivaTotal = ivaTotal.add(detalle.getIvaValor());

            // Reservar stock
            producto.reservarStock(detalleRequest.getCantidad());
            productoRepository.save(producto);
        }

        // Calcular totales
        venta.setSubtotal(subtotal);
        venta.setDescuentoTotal(descuentoTotal);
        venta.setIvaTotal(ivaTotal);
        venta.setTotal(subtotal.subtract(descuentoTotal).add(ivaTotal));

        if (request.getEfectivoRecibido() != null) {
            venta.setEfectivoRecibido(request.getEfectivoRecibido());
            venta.setCambio(request.getEfectivoRecibido().subtract(venta.getTotal()));
        }

        venta.calcularTotales();

        // Guardar venta
        Venta ventaGuardada = ventaRepository.save(venta);

        // Actualizar stock (liberar reserva y decrementar)
        for (DetalleVenta detalle : ventaGuardada.getDetalles()) {
            Producto producto = detalle.getProducto();
            producto.liberarStock(detalle.getCantidad());
            producto.setStockActual(producto.getStockActual().subtract(detalle.getCantidad()));
            producto.calcularEstadoStock();
            productoRepository.save(producto);
        }

        log.info("Venta creada: {} - Total: {}", numeroFactura, ventaGuardada.getTotal());

        return convertirAResponse(ventaGuardada);
    }

    @Override
    @Transactional(readOnly = true)
    public VentaResponse obtenerVentaPorId(Long id) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada"));
        return convertirAResponse(venta);
    }

    @Override
    @Transactional(readOnly = true)
    public VentaResponse obtenerVentaPorNumeroFactura(String numeroFactura) {
        Venta venta = ventaRepository.findByNumeroFactura(numeroFactura)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada"));
        return convertirAResponse(venta);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginacionResponse<VentaResponse> obtenerVentasPaginadas(Pageable pageable) {
        Page<Venta> ventasPage = ventaRepository.findAll(pageable);

        List<VentaResponse> ventasResponse = ventasPage.getContent().stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());

        return PaginacionResponse.<VentaResponse>builder()
                .content(ventasResponse)
                .pageNumber(ventasPage.getNumber())
                .pageSize(ventasPage.getSize())
                .totalElements(ventasPage.getTotalElements())
                .totalPages(ventasPage.getTotalPages())
                .last(ventasPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginacionResponse<VentaResponse> filtrarVentas(
            String numeroFactura, String clienteNombre, Long vendedorId,
            String estado, LocalDate fechaDesde, LocalDate fechaHasta, Pageable pageable) {

        LocalDateTime fechaDesdeDT = fechaDesde != null ? fechaDesde.atStartOfDay() : null;
        LocalDateTime fechaHastaDT = fechaHasta != null ?
                fechaHasta.atTime(23, 59, 59) : null;

        Venta.EstadoVenta estadoEnum = null;
        if (estado != null) {
            try {
                estadoEnum = Venta.EstadoVenta.valueOf(estado.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ValidacionException("Estado de venta inválido: " + estado);
            }
        }

        Page<Venta> ventasPage = ventaRepository.buscarConFiltros(
                numeroFactura, clienteNombre, vendedorId, estadoEnum,
                fechaDesdeDT, fechaHastaDT, pageable);

        List<VentaResponse> ventasResponse = ventasPage.getContent().stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());

        return PaginacionResponse.<VentaResponse>builder()
                .content(ventasResponse)
                .pageNumber(ventasPage.getNumber())
                .pageSize(ventasPage.getSize())
                .totalElements(ventasPage.getTotalElements())
                .totalPages(ventasPage.getTotalPages())
                .last(ventasPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public void anularVenta(Long id, String motivo) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada"));

        if (!venta.esAnulable()) {
            throw new ValidacionException("La venta no puede ser anulada en su estado actual");
        }

        venta.setEstado(Venta.EstadoVenta.ANULADA);
        venta.setMotivoAnulacion(motivo);
        venta.setFechaAnulacion(LocalDateTime.now());

        // Restaurar stock de productos
        for (DetalleVenta detalle : venta.getDetalles()) {
            Producto producto = detalle.getProducto();
            producto.setStockActual(producto.getStockActual().add(detalle.getCantidad()));
            producto.calcularEstadoStock();
            productoRepository.save(producto);
        }

        ventaRepository.save(venta);
        log.info("Venta anulada: {} - Motivo: {}", venta.getNumeroFactura(), motivo);
    }

    @Override
    public String generarNumeroFactura() {
        // Obtener el último consecutivo
        Integer ultimoConsecutivo = ventaRepository.findAll().stream()
                .map(Venta::getConsecutivo)
                .max(Integer::compareTo)
                .orElse(0);

        int nuevoConsecutivo = ultimoConsecutivo + 1;

        // Formato: F-000001
        return String.format("F-%06d", nuevoConsecutivo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaResponse> obtenerVentasDelDia() {
        LocalDate hoy = LocalDate.now();
        List<Venta> ventas = ventaRepository.findByFecha(hoy);

        return ventas.stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaResponse> obtenerVentasPorCliente(Long clienteId) {
        List<Venta> ventas = ventaRepository.findByClienteId(clienteId);

        return ventas.stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaResponse> obtenerVentasPorVendedor(Long vendedorId) {
        List<Venta> ventas = ventaRepository.findByVendedorId(vendedorId);

        return ventas.stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaResponse> obtenerVentasPorRangoFecha(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        List<Venta> ventas = ventaRepository.findByFechaEmisionBetween(fechaInicio, fechaFin);

        return ventas.stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticasDiarias(LocalDate fecha) {
        List<Venta> ventas = ventaRepository.findByFecha(fecha);

        BigDecimal totalVentas = ventas.stream()
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long totalClientes = ventas.stream()
                .map(Venta::getClienteId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        Long totalProductosVendidos = ventas.stream()
                .flatMap(v -> v.getDetalles().stream())
                .mapToLong(d -> d.getCantidad().longValue())
                .sum();

        Map<String, Object> estadisticas = new HashMap<>();
        estadisticas.put("fecha", fecha);
        estadisticas.put("totalVentas", totalVentas);
        estadisticas.put("cantidadVentas", ventas.size());
        estadisticas.put("totalClientes", totalClientes);
        estadisticas.put("totalProductosVendidos", totalProductosVendidos);
        estadisticas.put("ventas", ventas.stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList()));

        return estadisticas;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticasMensuales(int mes, int año) {
        YearMonth yearMonth = YearMonth.of(año, mes);
        LocalDateTime inicioMes = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime finMes = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        List<Venta> ventas = ventaRepository.findByFechaEmisionBetween(inicioMes, finMes);

        BigDecimal totalVentas = ventas.stream()
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> ventasPorFormaPago = ventas.stream()
                .collect(Collectors.groupingBy(
                        Venta::getFormaPago,
                        Collectors.reducing(BigDecimal.ZERO, Venta::getTotal, BigDecimal::add)
                ));

        Map<String, Object> estadisticas = new HashMap<>();
        estadisticas.put("mes", mes);
        estadisticas.put("año", año);
        estadisticas.put("totalVentas", totalVentas);
        estadisticas.put("cantidadVentas", ventas.size());
        estadisticas.put("ventasPorFormaPago", ventasPorFormaPago);
        estadisticas.put("promedioVenta", ventas.isEmpty() ? BigDecimal.ZERO :
                totalVentas.divide(new BigDecimal(ventas.size()), 2, java.math.RoundingMode.HALF_UP));

        return estadisticas;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerDashboardVentas() {
        LocalDate hoy = LocalDate.now();
        LocalDate ayer = hoy.minusDays(1);

        // Ventas de hoy
        List<Venta> ventasHoy = ventaRepository.findByFecha(hoy);
        BigDecimal totalHoy = ventasHoy.stream()
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Ventas de ayer
        List<Venta> ventasAyer = ventaRepository.findByFecha(ayer);
        BigDecimal totalAyer = ventasAyer.stream()
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Variación porcentual
        BigDecimal variacion = BigDecimal.ZERO;
        if (totalAyer.compareTo(BigDecimal.ZERO) > 0) {
            variacion = totalHoy.subtract(totalAyer)
                    .divide(totalAyer, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        // Ventas por forma de pago
        Map<String, BigDecimal> ventasPorFormaPago = ventasHoy.stream()
                .collect(Collectors.groupingBy(
                        Venta::getFormaPago,
                        Collectors.reducing(BigDecimal.ZERO, Venta::getTotal, BigDecimal::add)
                ));

        // Top productos
        List<Map<String, Object>> topProductos = obtenerTopProductosVendidos(5);

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("fecha", hoy);
        dashboard.put("ventasHoy", ventasHoy.size());
        dashboard.put("totalHoy", totalHoy);
        dashboard.put("totalAyer", totalAyer);
        dashboard.put("variacion", variacion);
        dashboard.put("ventasPorFormaPago", ventasPorFormaPago);
        dashboard.put("topProductos", topProductos);
        dashboard.put("ultimasVentas", ventasHoy.stream()
                .sorted((v1, v2) -> v2.getFechaEmision().compareTo(v1.getFechaEmision()))
                .limit(10)
                .map(this::convertirAResponse)
                .collect(Collectors.toList()));

        return dashboard;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerTopProductosVendidos(int limite) {
        // Consulta nativa para mejor performance
        String sql = "SELECT p.id, p.codigo, p.nombre, p.categoria_id, " +
                "SUM(dv.cantidad) as cantidad_vendida, " +
                "SUM(dv.total) as total_ventas " +
                "FROM productos p " +
                "JOIN detalle_ventas dv ON p.id = dv.producto_id " +
                "JOIN ventas v ON dv.venta_id = v.id " +
                "WHERE v.estado = 'COMPLETADA' " +
                "GROUP BY p.id, p.codigo, p.nombre, p.categoria_id " +
                "ORDER BY cantidad_vendida DESC " +
                "LIMIT ?";

        // En una implementación real usarías JdbcTemplate o Native Query
        // Por ahora devolvemos datos de ejemplo

        List<Map<String, Object>> topProductos = new ArrayList<>();

        // Datos de ejemplo
        for (int i = 1; i <= Math.min(limite, 5); i++) {
            Map<String, Object> producto = new HashMap<>();
            producto.put("posicion", i);
            producto.put("codigo", "PROD" + i);
            producto.put("nombre", "Producto Ejemplo " + i);
            producto.put("cantidadVendida", 100 - (i * 10));
            producto.put("totalVentas", new BigDecimal(1000 - (i * 100)));
            topProductos.add(producto);
        }

        return topProductos;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calcularTotalVentasDia(LocalDate fecha) {
        return ventaRepository.calcularVentasDelDia(fecha);
    }

    @Override
    @Transactional(readOnly = true)
    public Long contarVentasDelDia() {
        LocalDate hoy = LocalDate.now();
        List<Venta> ventas = ventaRepository.findByFecha(hoy);
        return (long) ventas.size();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeVentaConFactura(String numeroFactura) {
        return ventaRepository.existsByNumeroFactura(numeroFactura);
    }

    // Métodos privados auxiliares
    private VentaResponse convertirAResponse(Venta venta) {
        List<DetalleVentaResponse> detallesResponse = venta.getDetalles().stream()
                .map(detalle -> DetalleVentaResponse.builder()
                        .id(detalle.getId())
                        .productoId(detalle.getProducto().getId())
                        .codigoProducto(detalle.getCodigoProducto())
                        .nombreProducto(detalle.getNombreProducto())
                        .cantidad(detalle.getCantidad())
                        .precioUnitario(detalle.getPrecioUnitario())
                        .descuentoUnitario(detalle.getDescuentoUnitario())
                        .ivaValor(detalle.getIvaValor())
                        .subtotal(detalle.getSubtotal())
                        .total(detalle.getTotal())
                        .build())
                .collect(Collectors.toList());

        return VentaResponse.builder()
                .id(venta.getId())
                .numeroFactura(venta.getNumeroFactura())
                .clienteId(venta.getCliente() != null ? venta.getCliente().getId() : null)
                .clienteNombre(venta.getClienteNombre())
                .clienteDocumento(venta.getClienteDocumento())
                .vendedorId(venta.getVendedor().getId())
                .vendedorNombre(venta.getVendedorNombre())
                .cajaId(venta.getCaja().getId())
                .cajaNombre(venta.getCaja().getNombre())
                .sucursalId(venta.getSucursal() != null ? venta.getSucursal().getId() : null)
                .sucursalNombre(venta.getSucursal() != null ? venta.getSucursal().getNombre() : null)
                .fechaEmision(venta.getFechaEmision())
                .subtotal(venta.getSubtotal())
                .descuentoTotal(venta.getDescuentoTotal())
                .ivaTotal(venta.getIvaTotal())
                .total(venta.getTotal())
                .efectivoRecibido(venta.getEfectivoRecibido())
                .cambio(venta.getCambio())
                .formaPago(venta.getFormaPago())
                .estadoPago(venta.getEstadoPago())
                .estado(venta.getEstado().name())
                .motivoAnulacion(venta.getMotivoAnulacion())
                .detalles(detallesResponse)
                .creadoEn(venta.getCreadoEn())
                .build();
    }
}