package com.tienda.service.impl;

import com.tienda.dto.request.AnularVentaRequest;
import com.tienda.dto.request.VentaRequest;
import com.tienda.dto.response.*;
import com.tienda.exception.ResourceNotFoundException;
import com.tienda.exception.StockInsuficienteException;
import com.tienda.exception.ValidacionException;
import com.tienda.model.*;
import com.tienda.repository.*;
import com.tienda.service.VentaService;
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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    private final SucursalRepository sucursalRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;

    // Carrito en memoria (para sesión - en producción usarías Redis o base de datos)
    private final Map<Long, CarritoItem> carrito = new ConcurrentHashMap<>();

    // Clase interna para el carrito
    private static class CarritoItem {
        Long productoId;
        BigDecimal cantidad;
        BigDecimal precioUnitario;
        BigDecimal descuentoPorcentaje;
        BigDecimal ivaPorcentaje;

        CarritoItem(Long productoId, BigDecimal cantidad, BigDecimal precioUnitario,
                    BigDecimal descuentoPorcentaje, BigDecimal ivaPorcentaje) {
            this.productoId = productoId;
            this.cantidad = cantidad;
            this.precioUnitario = precioUnitario;
            this.descuentoPorcentaje = descuentoPorcentaje;
            this.ivaPorcentaje = ivaPorcentaje;
        }
    }

    @Override
    @Transactional
    public VentaResponse crearVenta(VentaRequest request, Long vendedorId) {
        // Validar venta
        Map<String, Object> validacion = validarVenta(request);
        if (!(Boolean) validacion.get("valido")) {
            throw new ValidacionException((String) validacion.get("mensaje"));
        }

        // Obtener vendedor
        Usuario vendedor = usuarioRepository.findById(vendedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendedor no encontrado"));

        // Obtener cliente
        Cliente cliente = null;
        if (request.getClienteId() != null) {
            cliente = clienteRepository.findById(request.getClienteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        }

        // Obtener caja
        Caja caja = null;
        if (request.getCajaId() != null) {
            caja = cajaRepository.findById(request.getCajaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Caja no encontrada"));
        }

        // Obtener sucursal del vendedor
        Sucursal sucursal = vendedor.getSucursal();

        // Generar número de factura
        String numeroFactura = generarNumeroFactura();

        // Calcular totales
        Map<String, BigDecimal> totales = calcularTotalesVenta(request);

        // Crear venta
        Venta venta = Venta.builder()
                .numeroFactura(numeroFactura)
                .prefijoFactura("F")
                .consecutivo(obtenerSiguienteConsecutivo())
                .cliente(cliente)
                .clienteNombre(request.getClienteNombre() != null ? request.getClienteNombre() :
                        (cliente != null ? cliente.getNombre() : "CONSUMIDOR FINAL"))
                .clienteDocumento(request.getClienteDocumento() != null ? request.getClienteDocumento() :
                        (cliente != null ? cliente.getNumeroDocumento() : "9999999999"))
                .clienteDireccion(request.getClienteDireccion())
                .clienteTelefono(request.getClienteTelefono())
                .clienteEmail(request.getClienteEmail())
                .subtotal(totales.get("subtotal"))
                .descuentoTotal(request.getDescuentoTotal() != null ? request.getDescuentoTotal() : BigDecimal.ZERO)
                .ivaTotal(request.getIvaTotal() != null ? request.getIvaTotal() : totales.get("ivaTotal"))
                .otrosImpuestos(request.getOtrosImpuestos() != null ? request.getOtrosImpuestos() : BigDecimal.ZERO)
                .total(totales.get("total"))
                .efectivoRecibido(request.getEfectivoRecibido())
                .cambio(request.getEfectivoRecibido() != null ?
                        request.getEfectivoRecibido().subtract(totales.get("total")) : BigDecimal.ZERO)
                .formaPago(request.getFormaPago())
                .estadoPago("PAGADO")
                .estado(Venta.EstadoVenta.COMPLETADA)
                .vendedor(vendedor)
                .vendedorNombre(vendedor.getNombreCompleto())
                .caja(caja)
                .sucursal(sucursal)
                .build();

        // Guardar venta
        venta = ventaRepository.save(venta);

        // Procesar detalles
        procesarDetallesVenta(venta, request.getItems());

        // Actualizar stock
        actualizarStockVenta(request.getItems());

        // Registrar movimientos de inventario
        registrarMovimientosInventario(venta, request.getItems(), vendedor);

        // Actualizar estadísticas del cliente
        if (cliente != null) {
            actualizarEstadisticasCliente(cliente, venta.getTotal());
        }

        // Limpiar carrito
        limpiarCarrito();

        log.info("Venta creada exitosamente: {} - Total: {}",
                venta.getNumeroFactura(), venta.getTotal());

        return convertirAResponse(venta);
    }

    @Override
    @Transactional
    public VentaResponse anularVenta(Long ventaId, AnularVentaRequest request, Long usuarioId) {
        Venta venta = obtenerEntidadVenta(ventaId);
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Validar que la venta se puede anular
        if (!venta.esAnulable()) {
            throw new ValidacionException("La venta no se puede anular. Estado actual: " + venta.getEstado());
        }

        // Anular venta
        venta.setEstado(Venta.EstadoVenta.ANULADA);
        venta.setMotivoAnulacion(request.getMotivo());
        venta.setFechaAnulacion(LocalDateTime.now());
        venta.setUsuarioAnulacion(usuario);

        // Revertir stock
        revertirStockVenta(venta);

        venta = ventaRepository.save(venta);

        log.info("Venta anulada: {} - Motivo: {}", venta.getNumeroFactura(), request.getMotivo());

        return convertirAResponse(venta);
    }

    @Override
    @Transactional(readOnly = true)
    public VentaResponse obtenerVentaPorId(Long id) {
        Venta venta = obtenerEntidadVenta(id);
        return convertirAResponse(venta);
    }

    @Override
    @Transactional(readOnly = true)
    public VentaResponse obtenerVentaPorFactura(String numeroFactura) {
        Venta venta = ventaRepository.findByNumeroFactura(numeroFactura)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada"));
        return convertirAResponse(venta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaResponse> obtenerTodasVentas() {
        return ventaRepository.findAll().stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
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
            String estado, LocalDateTime fechaDesde, LocalDateTime fechaHasta,
            Pageable pageable) {

        // Convertir estado
        Venta.EstadoVenta estadoVenta = null;
        if (estado != null) {
            try {
                estadoVenta = Venta.EstadoVenta.valueOf(estado.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Si no es un estado válido, se filtra por cadena
            }
        }

        Page<Venta> ventasPage = ventaRepository.buscarConFiltros(
                numeroFactura, clienteNombre, vendedorId, estadoVenta,
                fechaDesde, fechaHasta, pageable);

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
    public List<VentaResponse> obtenerVentasPorFecha(LocalDate fecha) {
        List<Venta> ventas = ventaRepository.findByFecha(fecha);
        return ventas.stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaResponse> obtenerVentasPorRangoFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(23, 59, 59);

        List<Venta> ventas = ventaRepository.findByFechaEmisionBetween(inicio, fin);
        return ventas.stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    // ============ MÉTODOS DEL CARRITO ============

    @Override
    @Transactional(readOnly = true)
    public CarritoResponse agregarAlCarrito(Long productoId, BigDecimal cantidad) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        // Verificar stock
        if (!producto.tieneStockSuficiente(cantidad)) {
            throw new StockInsuficienteException(
                    "Stock insuficiente para " + producto.getNombre() +
                            ". Disponible: " + producto.getStockDisponible());
        }

        // Agregar al carrito
        CarritoItem item = new CarritoItem(
                productoId,
                cantidad,
                producto.getPrecioVenta(),
                BigDecimal.ZERO, // descuento porcentaje
                new BigDecimal("19") // IVA 19%
        );

        carrito.put(productoId, item);

        return obtenerCarrito();
    }

    @Override
    @Transactional(readOnly = true)
    public CarritoResponse obtenerCarrito() {
        List<CarritoResponse.ItemCarritoResponse> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CarritoItem item : carrito.values()) {
            Producto producto = productoRepository.findById(item.productoId)
                    .orElse(null);

            if (producto != null) {
                BigDecimal itemSubtotal = item.cantidad.multiply(item.precioUnitario);
                BigDecimal descuento = itemSubtotal.multiply(
                        item.descuentoPorcentaje.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));

                CarritoResponse.ItemCarritoResponse itemResponse =
                        CarritoResponse.ItemCarritoResponse.builder()
                                .productoId(producto.getId())
                                .codigo(producto.getCodigo())
                                .nombre(producto.getNombre())
                                .cantidad(item.cantidad)
                                .precioUnitario(item.precioUnitario)
                                .descuentoUnitario(descuento)
                                .subtotal(itemSubtotal.subtract(descuento))
                                .stockSuficiente(producto.tieneStockSuficiente(item.cantidad))
                                .stockDisponible(producto.getStockDisponible())
                                .build();

                items.add(itemResponse);
                subtotal = subtotal.add(itemResponse.getSubtotal());
            }
        }

        // Calcular IVA (19%)
        BigDecimal ivaTotal = subtotal.multiply(new BigDecimal("0.19"));
        BigDecimal total = subtotal.add(ivaTotal);

        return CarritoResponse.builder()
                .items(items)
                .subtotal(subtotal)
                .descuentoTotal(BigDecimal.ZERO)
                .ivaTotal(ivaTotal)
                .total(total)
                .totalItems(items.size())
                .build();
    }

    @Override
    public void limpiarCarrito() {
        carrito.clear();
    }

    @Override
    public void removerDelCarrito(Long productoId) {
        carrito.remove(productoId);
    }

    @Override
    public CarritoResponse actualizarCantidadCarrito(Long productoId, BigDecimal cantidad) {
        if (carrito.containsKey(productoId)) {
            CarritoItem item = carrito.get(productoId);
            item.cantidad = cantidad;
            carrito.put(productoId, item);
        }
        return obtenerCarrito();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verificarStockCarrito() {
        for (CarritoItem item : carrito.values()) {
            Producto producto = productoRepository.findById(item.productoId).orElse(null);
            if (producto == null || !producto.tieneStockSuficiente(item.cantidad)) {
                return false;
            }
        }
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> validarVenta(VentaRequest request) {
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("valido", true);

        // Validar items
        if (request.getItems() == null || request.getItems().isEmpty()) {
            resultado.put("valido", false);
            resultado.put("mensaje", "La venta debe tener al menos un producto");
            return resultado;
        }

        // Validar stock de cada producto
        for (VentaRequest.ItemVentaRequest item : request.getItems()) {
            Producto producto = productoRepository.findById(item.getProductoId())
                    .orElse(null);

            if (producto == null) {
                resultado.put("valido", false);
                resultado.put("mensaje", "Producto no encontrado: ID " + item.getProductoId());
                return resultado;
            }

            if (!producto.tieneStockSuficiente(item.getCantidad())) {
                resultado.put("valido", false);
                resultado.put("mensaje", "Stock insuficiente para: " + producto.getNombre() +
                        ". Disponible: " + producto.getStockDisponible());
                return resultado;
            }
        }

        return resultado;
    }

    @Override
    public String generarNumeroFactura() {
        // Formato: F-YYYYMMDD-000001
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String fecha = LocalDate.now().format(formatter);

        // Obtener último consecutivo del día
        Integer ultimoConsecutivo = ventaRepository.findAll().stream()
                .filter(v -> v.getNumeroFactura().contains(fecha))
                .map(Venta::getConsecutivo)
                .max(Integer::compareTo)
                .orElse(0);

        int nuevoConsecutivo = ultimoConsecutivo + 1;

        return String.format("F-%s-%06d", fecha, nuevoConsecutivo);
    }

    private Integer obtenerSiguienteConsecutivo() {
        Integer maxConsecutivo = ventaRepository.findAll().stream()
                .map(Venta::getConsecutivo)
                .max(Integer::compareTo)
                .orElse(0);

        return maxConsecutivo + 1;
    }

    @Override
    public BigDecimal calcularTotalVenta(VentaRequest request) {
        Map<String, BigDecimal> totales = calcularTotalesVenta(request);
        return totales.get("total");
    }

    @Override
    public Map<String, BigDecimal> calcularTotalesVenta(VentaRequest request) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal descuentoTotal = BigDecimal.ZERO;
        BigDecimal ivaTotal = BigDecimal.ZERO;

        for (VentaRequest.ItemVentaRequest item : request.getItems()) {
            Producto producto = productoRepository.findById(item.getProductoId())
                    .orElse(null);

            if (producto != null) {
                BigDecimal precio = item.getPrecioUnitario() != null ?
                        item.getPrecioUnitario() : producto.getPrecioVenta();
                BigDecimal cantidad = item.getCantidad();

                // Calcular subtotal del item
                BigDecimal itemSubtotal = precio.multiply(cantidad);

                // Calcular descuento
                BigDecimal descuento = BigDecimal.ZERO;
                if (item.getDescuentoPorcentaje() != null &&
                        item.getDescuentoPorcentaje().compareTo(BigDecimal.ZERO) > 0) {
                    descuento = itemSubtotal.multiply(
                            item.getDescuentoPorcentaje().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
                } else if (item.getDescuentoUnitario() != null) {
                    descuento = item.getDescuentoUnitario();
                }

                // Calcular IVA
                BigDecimal ivaPorcentaje = item.getIvaPorcentaje() != null ?
                        item.getIvaPorcentaje() : new BigDecimal("19");
                BigDecimal ivaItem = itemSubtotal.subtract(descuento)
                        .multiply(ivaPorcentaje.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));

                subtotal = subtotal.add(itemSubtotal);
                descuentoTotal = descuentoTotal.add(descuento);
                ivaTotal = ivaTotal.add(ivaItem);
            }
        }

        // Aplicar descuento total adicional si existe
        if (request.getDescuentoTotal() != null) {
            descuentoTotal = descuentoTotal.add(request.getDescuentoTotal());
        }

        // Aplicar IVA total adicional si existe
        if (request.getIvaTotal() != null) {
            ivaTotal = ivaTotal.add(request.getIvaTotal());
        }

        BigDecimal total = subtotal.subtract(descuentoTotal)
                .add(ivaTotal)
                .add(request.getOtrosImpuestos() != null ? request.getOtrosImpuestos() : BigDecimal.ZERO);

        Map<String, BigDecimal> totales = new HashMap<>();
        totales.put("subtotal", subtotal);
        totales.put("descuentoTotal", descuentoTotal);
        totales.put("ivaTotal", ivaTotal);
        totales.put("total", total);

        return totales;
    }

    // ============ REPORTES Y ESTADÍSTICAS ============

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticasDiarias(LocalDate fecha) {
        List<Venta> ventas = ventaRepository.findByFecha(fecha);

        BigDecimal totalVentas = ventas.stream()
                .filter(v -> v.getEstado() == Venta.EstadoVenta.COMPLETADA)
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Integer cantidadVentas = ventas.stream()
                .filter(v -> v.getEstado() == Venta.EstadoVenta.COMPLETADA)
                .mapToInt(v -> 1)
                .sum();

        BigDecimal ticketPromedio = cantidadVentas > 0 ?
                totalVentas.divide(new BigDecimal(cantidadVentas), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        Map<String, Object> estadisticas = new HashMap<>();
        estadisticas.put("fecha", fecha);
        estadisticas.put("totalVentas", totalVentas);
        estadisticas.put("cantidadVentas", cantidadVentas);
        estadisticas.put("ticketPromedio", ticketPromedio);
        estadisticas.put("ventasAnuladas", ventas.stream()
                .filter(v -> v.getEstado() == Venta.EstadoVenta.ANULADA)
                .count());

        return estadisticas;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal obtenerVentasDelDia() {
        LocalDate hoy = LocalDate.now();
        List<Venta> ventas = ventaRepository.findByFecha(hoy);

        return ventas.stream()
                .filter(v -> v.getEstado() == Venta.EstadoVenta.COMPLETADA)
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer obtenerCantidadVentasDelDia() {
        LocalDate hoy = LocalDate.now();
        List<Venta> ventas = ventaRepository.findByFecha(hoy);

        return (int) ventas.stream()
                .filter(v -> v.getEstado() == Venta.EstadoVenta.COMPLETADA)
                .count();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal obtenerTicketPromedioDelDia() {
        BigDecimal total = obtenerVentasDelDia();
        Integer cantidad = obtenerCantidadVentasDelDia();

        return cantidad > 0 ?
                total.divide(new BigDecimal(cantidad), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> obtenerTopProductosVendidos(LocalDate fechaInicio, LocalDate fechaFin) {
        // Esta consulta es más compleja, normalmente se haría con una consulta nativa
        // Por ahora retornamos una lista vacía
        return new ArrayList<>();
    }

    // ============ MÉTODOS INTERNOS ============

    @Override
    @Transactional(readOnly = true)
    public Venta obtenerEntidadVenta(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada con ID: " + id));
    }

    @Override
    public void procesarVenta(Venta venta) {
        // Implementación para procesar venta (puede incluir notificaciones, etc.)
        log.info("Procesando venta: {}", venta.getNumeroFactura());
    }

    // ============ MÉTODOS PRIVADOS ============

    private void procesarDetallesVenta(Venta venta, List<VentaRequest.ItemVentaRequest> items) {
        for (VentaRequest.ItemVentaRequest itemRequest : items) {
            Producto producto = productoRepository.findById(itemRequest.getProductoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

            BigDecimal precio = itemRequest.getPrecioUnitario() != null ?
                    itemRequest.getPrecioUnitario() : producto.getPrecioVenta();

            DetalleVenta detalle = DetalleVenta.builder()
                    .venta(venta)
                    .producto(producto)
                    .cantidad(itemRequest.getCantidad())
                    .precioUnitario(precio)
                    .costoUnitario(producto.getCostoPromedio())
                    .descuentoPorcentaje(itemRequest.getDescuentoPorcentaje())
                    .ivaPorcentaje(itemRequest.getIvaPorcentaje() != null ?
                            itemRequest.getIvaPorcentaje() : new BigDecimal("19"))
                    .build();

            detalleVentaRepository.save(detalle);
        }
    }

    private void actualizarStockVenta(List<VentaRequest.ItemVentaRequest> items) {
        for (VentaRequest.ItemVentaRequest item : items) {
            Producto producto = productoRepository.findById(item.getProductoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

            // Decrementar stock
            producto.setStockActual(producto.getStockActual().subtract(item.getCantidad()));
            producto.calcularEstadoStock();

            productoRepository.save(producto);
        }
    }

    private void registrarMovimientosInventario(Venta venta, List<VentaRequest.ItemVentaRequest> items, Usuario usuario) {
        for (VentaRequest.ItemVentaRequest item : items) {
            Producto producto = productoRepository.findById(item.getProductoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

            MovimientoInventario movimiento = MovimientoInventario.builder()
                    .producto(producto)
                    .tipoMovimiento("SALIDA")
                    .subtipo("VENTA")
                    .documentoId(venta.getId())
                    .documentoNumero(venta.getNumeroFactura())
                    .cantidad(item.getCantidad())
                    .cantidadAnterior(producto.getStockActual().add(item.getCantidad())) // Stock antes de la venta
                    .cantidadNueva(producto.getStockActual())
                    .costoUnitario(producto.getCostoPromedio())
                    .precioUnitario(producto.getPrecioVenta())
                    .motivo("Venta registrada - Factura: " + venta.getNumeroFactura())
                    .usuario(usuario)
                    .fechaMovimiento(LocalDateTime.now())
                    .creadoEn(LocalDateTime.now())
                    .build();

            movimientoInventarioRepository.save(movimiento);
        }
    }

    private void actualizarEstadisticasCliente(Cliente cliente, BigDecimal totalVenta) {
        cliente.setTotalCompras(cliente.getTotalCompras().add(totalVenta));
        cliente.setUltimaCompra(LocalDateTime.now());
        clienteRepository.save(cliente);
    }

    private void revertirStockVenta(Venta venta) {
        List<DetalleVenta> detalles = detalleVentaRepository.findByVentaId(venta.getId());

        for (DetalleVenta detalle : detalles) {
            Producto producto = detalle.getProducto();

            // Incrementar stock
            producto.setStockActual(producto.getStockActual().add(detalle.getCantidad()));
            producto.calcularEstadoStock();

            productoRepository.save(producto);

            // Registrar movimiento de reversión
            MovimientoInventario movimiento = MovimientoInventario.builder()
                    .producto(producto)
                    .tipoMovimiento("ENTRADA")
                    .subtipo("ANULACION")
                    .documentoId(venta.getId())
                    .documentoNumero(venta.getNumeroFactura())
                    .cantidad(detalle.getCantidad())
                    .cantidadAnterior(producto.getStockActual().subtract(detalle.getCantidad()))
                    .cantidadNueva(producto.getStockActual())
                    .costoUnitario(producto.getCostoPromedio())
                    .precioUnitario(detalle.getPrecioUnitario())
                    .motivo("Anulación de venta - Factura: " + venta.getNumeroFactura())
                    .usuario(venta.getUsuarioAnulacion())
                    .fechaMovimiento(LocalDateTime.now())
                    .creadoEn(LocalDateTime.now())
                    .build();

            movimientoInventarioRepository.save(movimiento);
        }
    }

    private VentaResponse convertirAResponse(Venta venta) {
        // Obtener detalles
        List<DetalleVenta> detalles = detalleVentaRepository.findByVentaId(venta.getId());
        List<DetalleVentaResponse> detallesResponse = detalles.stream()
                .map(this::convertirDetalleAResponse)
                .collect(Collectors.toList());

        return VentaResponse.builder()
                .id(venta.getId())
                .numeroFactura(venta.getNumeroFactura())
                .prefijoFactura(venta.getPrefijoFactura())
                .consecutivo(venta.getConsecutivo())
                .clienteId(venta.getCliente() != null ? venta.getCliente().getId() : null)
                .clienteNombre(venta.getClienteNombre())
                .clienteDocumento(venta.getClienteDocumento())
                .clienteTelefono(venta.getClienteTelefono())
                .subtotal(venta.getSubtotal())
                .descuentoTotal(venta.getDescuentoTotal())
                .ivaTotal(venta.getIvaTotal())
                .otrosImpuestos(venta.getOtrosImpuestos())
                .total(venta.getTotal())
                .formaPago(venta.getFormaPago())
                .estadoPago(venta.getEstadoPago())
                .efectivoRecibido(venta.getEfectivoRecibido())
                .cambio(venta.getCambio())
                .estado(venta.getEstado().name())
                .motivoAnulacion(venta.getMotivoAnulacion())
                .fechaAnulacion(venta.getFechaAnulacion())
                .vendedorId(venta.getVendedor() != null ? venta.getVendedor().getId() : null)
                .vendedorNombre(venta.getVendedorNombre())
                .cajaId(venta.getCaja() != null ? venta.getCaja().getId() : null)
                .cajaNombre(venta.getCaja() != null ? venta.getCaja().getNombre() : null)
                .sucursalId(venta.getSucursal() != null ? venta.getSucursal().getId() : null)
                .sucursalNombre(venta.getSucursal() != null ? venta.getSucursal().getNombre() : null)
                .fechaEmision(venta.getFechaEmision())
                .creadoEn(venta.getCreadoEn())
                .actualizadoEn(venta.getActualizadoEn())
                .detalles(detallesResponse)
                .build();
    }

    private DetalleVentaResponse convertirDetalleAResponse(DetalleVenta detalle) {
        return DetalleVentaResponse.builder()
                .id(detalle.getId())
                .productoId(detalle.getProducto() != null ? detalle.getProducto().getId() : null)
                .codigoProducto(detalle.getCodigoProducto())
                .nombreProducto(detalle.getNombreProducto())
                .unidadMedida(detalle.getUnidadMedida())
                .cantidad(detalle.getCantidad())
                .cantidadDevuelta(detalle.getCantidadDevuelta())
                .precioUnitario(detalle.getPrecioUnitario())
                .costoUnitario(detalle.getCostoUnitario())
                .descuentoUnitario(detalle.getDescuentoUnitario())
                .descuentoPorcentaje(detalle.getDescuentoPorcentaje())
                .ivaPorcentaje(detalle.getIvaPorcentaje())
                .ivaValor(detalle.getIvaValor())
                .subtotal(detalle.getSubtotal())
                .total(detalle.getTotal())
                .build();
    }

    // Métodos pendientes de implementación (puedes implementarlos después)

    @Override
    public Map<String, Object> obtenerEstadisticasMensuales(Integer mes, Integer anio) {
        // Implementación pendiente
        return new HashMap<>();
    }

    @Override
    public List<Map<String, Object>> obtenerTopClientes(LocalDate fechaInicio, LocalDate fechaFin) {
        // Implementación pendiente
        return new ArrayList<>();
    }
}