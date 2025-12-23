package com.tienda.service.impl;

import com.tienda.dto.response.EstadoStockResponse;
import com.tienda.dto.response.PaginacionResponse;
import com.tienda.exception.ResourceNotFoundException;
import com.tienda.exception.StockInsuficienteException;
import com.tienda.exception.ValidacionException;
import com.tienda.model.*;
import com.tienda.repository.*;
import com.tienda.service.InventarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventarioServiceImpl implements InventarioService {

    private final ProductoRepository productoRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EstadoStockResponse> obtenerInventarioConEstados() {
        List<Producto> productos = productoRepository.findAll();
        return productos.stream()
                .map(this::convertirAEstadoStockResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginacionResponse<EstadoStockResponse> obtenerInventarioPaginado(Pageable pageable) {
        Page<Producto> productosPage = productoRepository.findAll(pageable);

        List<EstadoStockResponse> estados = productosPage.getContent().stream()
                .map(this::convertirAEstadoStockResponse)
                .collect(Collectors.toList());

        return PaginacionResponse.<EstadoStockResponse>builder()
                .content(estados)
                .pageNumber(productosPage.getNumber())
                .pageSize(productosPage.getSize())
                .totalElements(productosPage.getTotalElements())
                .totalPages(productosPage.getTotalPages())
                .last(productosPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstadoStockResponse> obtenerAlertasStock() {
        // ✅ CORREGIDO: Usando Arrays.asList() para Java 8
        List<Producto.AlertaStock> alertas = Arrays.asList(
                Producto.AlertaStock.BAJO,
                Producto.AlertaStock.CRITICO,
                Producto.AlertaStock.AGOTADO
        );

        List<Producto> productos = productoRepository.findByAlertaStockIn(alertas);

        return productos.stream()
                .map(this::convertirAEstadoStockResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerValoracionInventario() {
        List<Producto> productosActivos = productoRepository.findAll().stream()
                .filter(p -> p.getEstado() == Producto.EstadoProducto.ACTIVO)
                .collect(Collectors.toList());

        BigDecimal valorTotalCosto = BigDecimal.ZERO;
        BigDecimal valorTotalVenta = BigDecimal.ZERO;
        int productosConStock = 0;
        int productosBajoStock = 0;
        int productosCriticoStock = 0;
        int productosSobreStock = 0;
        int productosAgotados = 0;

        for (Producto producto : productosActivos) {
            BigDecimal valorCosto = producto.getStockActual()
                    .multiply(producto.getCostoPromedio() != null ?
                            producto.getCostoPromedio() : BigDecimal.ZERO);
            BigDecimal valorVenta = producto.getStockActual()
                    .multiply(producto.getPrecioVenta());

            valorTotalCosto = valorTotalCosto.add(valorCosto);
            valorTotalVenta = valorTotalVenta.add(valorVenta);

            if (producto.getStockActual().compareTo(BigDecimal.ZERO) > 0) {
                productosConStock++;
            }

            switch (producto.getAlertaStock()) {
                case BAJO:
                    productosBajoStock++;
                    break;
                case CRITICO:
                    productosCriticoStock++;
                    break;
                case SOBRE:
                    productosSobreStock++;
                    break;
                case AGOTADO:
                    productosAgotados++;
                    break;
            }
        }

        BigDecimal utilidadPotencial = valorTotalVenta.subtract(valorTotalCosto);
        BigDecimal margenPromedio = BigDecimal.ZERO;

        if (valorTotalCosto.compareTo(BigDecimal.ZERO) > 0) {
            margenPromedio = utilidadPotencial
                    .divide(valorTotalCosto, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        Map<String, Object> valoracion = new HashMap<>();
        valoracion.put("valorTotalCosto", valorTotalCosto);
        valoracion.put("valorTotalVenta", valorTotalVenta);
        valoracion.put("utilidadPotencial", utilidadPotencial);
        valoracion.put("margenPromedio", margenPromedio);
        valoracion.put("totalProductos", productosActivos.size());
        valoracion.put("productosConStock", productosConStock);
        valoracion.put("productosBajoStock", productosBajoStock);
        valoracion.put("productosCriticoStock", productosCriticoStock);
        valoracion.put("productosSobreStock", productosSobreStock);
        valoracion.put("productosAgotados", productosAgotados);
        valoracion.put("productosActivos", productosActivos.size());

        return valoracion;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginacionResponse<EstadoStockResponse> filtrarInventario(
            String codigo, String nombre, Long categoriaId,
            Producto.AlertaStock alertaStock, Pageable pageable) {

        // Convertir alertaStock a estado si es necesario
        Producto.EstadoProducto estado = Producto.EstadoProducto.ACTIVO;

        Page<Producto> productosPage = productoRepository.buscarConFiltros(
                codigo, nombre, categoriaId, estado, pageable);

        // Filtrar por alertaStock si se especifica
        List<Producto> productosFiltrados = productosPage.getContent();
        if (alertaStock != null) {
            productosFiltrados = productosFiltrados.stream()
                    .filter(p -> p.getAlertaStock() == alertaStock)
                    .collect(Collectors.toList());
        }

        List<EstadoStockResponse> estados = productosFiltrados.stream()
                .map(this::convertirAEstadoStockResponse)
                .collect(Collectors.toList());

        return PaginacionResponse.<EstadoStockResponse>builder()
                .content(estados)
                .pageNumber(productosPage.getNumber())
                .pageSize(productosPage.getSize())
                .totalElements((long) productosFiltrados.size())
                .totalPages((int) Math.ceil((double) productosFiltrados.size() / pageable.getPageSize()))
                .last(true)
                .build();
    }

    @Override
    @Transactional
    public EstadoStockResponse ajustarStock(Long productoId, BigDecimal cantidad,
                                            String motivo, String tipoAjuste, Long usuarioId) {

        Producto producto = obtenerProducto(productoId);
        Usuario usuario = obtenerUsuario(usuarioId);

        BigDecimal cantidadAnterior = producto.getStockActual();
        BigDecimal cantidadNueva;

        String tipoUpper = tipoAjuste != null ? tipoAjuste.toUpperCase() : "";

        switch (tipoUpper) {
            case "INCREMENTO":
                cantidadNueva = cantidadAnterior.add(cantidad);
                break;
            case "DECREMENTO":
                if (cantidadAnterior.compareTo(cantidad) < 0) {
                    throw new StockInsuficienteException(
                            "No hay suficiente stock para decrementar. Disponible: " +
                                    cantidadAnterior + ", Requerido: " + cantidad);
                }
                cantidadNueva = cantidadAnterior.subtract(cantidad);
                break;
            case "FIJO":
                cantidadNueva = cantidad;
                break;
            default:
                throw new ValidacionException("Tipo de ajuste inválido: " + tipoAjuste);
        }

        producto.setStockActual(cantidadNueva);
        producto.calcularEstadoStock();

        // Registrar movimiento
        registrarMovimientoInventario(producto, tipoUpper, cantidad,
                cantidadAnterior, cantidadNueva, motivo, usuario);

        Producto productoActualizado = productoRepository.save(producto);

        return convertirAEstadoStockResponse(productoActualizado);
    }

    @Override
    @Transactional
    public EstadoStockResponse incrementarStock(Long productoId, BigDecimal cantidad,
                                                String motivo, Long usuarioId) {
        return ajustarStock(productoId, cantidad, motivo, "INCREMENTO", usuarioId);
    }

    @Override
    @Transactional
    public EstadoStockResponse decrementarStock(Long productoId, BigDecimal cantidad,
                                                String motivo, Long usuarioId) {
        return ajustarStock(productoId, cantidad, motivo, "DECREMENTO", usuarioId);
    }

    @Override
    @Transactional
    public void reservarStock(Long productoId, BigDecimal cantidad) {
        Producto producto = obtenerProducto(productoId);

        if (!producto.tieneStockSuficiente(cantidad)) {
            throw new StockInsuficienteException(
                    "Stock insuficiente para reservar. Disponible: " +
                            producto.getStockDisponible() + ", Requerido: " + cantidad);
        }

        producto.reservarStock(cantidad);
        productoRepository.save(producto);

        log.info("Stock reservado: Producto {}, Cantidad: {}",
                producto.getCodigo(), cantidad);
    }

    @Override
    @Transactional
    public void liberarStock(Long productoId, BigDecimal cantidad) {
        Producto producto = obtenerProducto(productoId);

        BigDecimal stockReservado = producto.getStockReservado() != null ?
                producto.getStockReservado() : BigDecimal.ZERO;

        if (stockReservado.compareTo(cantidad) < 0) {
            throw new ValidacionException(
                    "No hay suficiente stock reservado para liberar");
        }

        producto.liberarStock(cantidad);
        productoRepository.save(producto);

        log.info("Stock liberado: Producto {}, Cantidad: {}",
                producto.getCodigo(), cantidad);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verificarStockDisponible(Long productoId, BigDecimal cantidad) {
        Producto producto = obtenerProducto(productoId);
        return producto.tieneStockSuficiente(cantidad);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstadoStockResponse> obtenerProductosParaReorden() {
        List<Producto> productos = productoRepository.findProductosParaReorden();
        return productos.stream()
                .map(this::convertirAEstadoStockResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstadoStockResponse> obtenerProductosAgotados() {
        List<Producto> productos = productoRepository.findProductosAgotados();
        return productos.stream()
                .map(this::convertirAEstadoStockResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> generarReporteStock() {
        Map<String, Object> reporte = new HashMap<>();

        // Obtener datos básicos
        List<EstadoStockResponse> inventario = obtenerInventarioConEstados();
        Map<String, Object> valoracion = obtenerValoracionInventario();
        List<EstadoStockResponse> alertas = obtenerAlertasStock();
        List<EstadoStockResponse> paraReorden = obtenerProductosParaReorden();

        // Calcular estadísticas adicionales
        long productosActivos = inventario.stream()
                .filter(p -> "ACTIVO".equals(p.getEstadoProducto()))
                .count();

        long productosInactivos = inventario.stream()
                .filter(p -> "INACTIVO".equals(p.getEstadoProducto()))
                .count();

        BigDecimal valorTotalInventario = inventario.stream()
                .map(EstadoStockResponse::getValorCosto)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Construir reporte
        reporte.put("fechaGeneracion", LocalDateTime.now());
        reporte.put("totalProductos", inventario.size());
        reporte.put("productosActivos", productosActivos);
        reporte.put("productosInactivos", productosInactivos);
        reporte.put("valorTotalInventario", valorTotalInventario);
        reporte.put("valoracion", valoracion);
        reporte.put("totalAlertas", alertas.size());
        reporte.put("alertas", alertas);
        reporte.put("productosParaReorden", paraReorden.size());
        reporte.put("listaReorden", paraReorden);
        reporte.put("topProductosValor", inventario.stream()
                .filter(p -> p.getValorCosto() != null)
                .sorted((p1, p2) -> p2.getValorCosto().compareTo(p1.getValorCosto()))
                .limit(10)
                .collect(Collectors.toList()));

        return reporte;
    }

    // Métodos privados auxiliares
    private Producto obtenerProducto(Long productoId) {
        return productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
    }

    private Usuario obtenerUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private EstadoStockResponse convertirAEstadoStockResponse(Producto producto) {
        return EstadoStockResponse.builder()
                .id(producto.getId())
                .codigo(producto.getCodigo())
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .categoria(producto.getCategoria() != null ?
                        producto.getCategoria().getNombre() : null)
                .subcategoria(producto.getSubcategoria())
                .stockActual(producto.getStockActual())
                .stockDisponible(producto.getStockDisponible())
                .stockReservado(producto.getStockReservado())
                .stockMinimo(producto.getStockMinimo())
                .stockMaximo(producto.getStockMaximo())
                .estadoProducto(producto.getEstado().name())
                .alertaStock(producto.getAlertaStock().name())
                .estadoCalculado(calcularEstado(producto))
                .costoPromedio(producto.getCostoPromedio())
                .precioVenta(producto.getPrecioVenta())
                .valorCosto(producto.getValorCosto())
                .valorVenta(producto.getValorVenta())
                .utilidadPotencial(producto.getUtilidadPotencial())
                .margenGanancia(producto.getMargenGanancia())
                .unidadMedida(producto.getUnidadMedida())
                .tipoProducto(producto.getTipoProducto().name())
                .ubicacion(producto.getUbicacion())
                .proveedor(producto.getProveedor() != null ?
                        producto.getProveedor().getNombre() : null)
                .necesitaReorden(producto.getStockActual()
                        .compareTo(producto.getStockMinimo()) <= 0)
                .creadoEn(producto.getCreadoEn())
                .actualizadoEn(producto.getActualizadoEn())
                .build();
    }

    private String calcularEstado(Producto producto) {
        if (producto.getStockActual().compareTo(BigDecimal.ZERO) <= 0) {
            return "AGOTADO";
        } else if (producto.getStockActual().compareTo(
                producto.getStockMinimo().multiply(new BigDecimal("0.3"))) <= 0) {
            return "CRÍTICO";
        } else if (producto.getStockActual().compareTo(producto.getStockMinimo()) <= 0) {
            return "BAJO";
        } else if (producto.getStockMaximo() != null &&
                producto.getStockActual().compareTo(producto.getStockMaximo()) > 0) {
            return "SOBRE";
        } else {
            return "NORMAL";
        }
    }

    private void registrarMovimientoInventario(Producto producto, String tipoMovimiento,
                                               BigDecimal cantidad, BigDecimal cantidadAnterior,
                                               BigDecimal cantidadNueva, String motivo, Usuario usuario) {

        MovimientoInventario movimiento = MovimientoInventario.builder()
                .producto(producto)
                .tipoMovimiento(tipoMovimiento)
                .cantidad(cantidad)
                .cantidadAnterior(cantidadAnterior)
                .cantidadNueva(cantidadNueva)
                .costoUnitario(producto.getCostoPromedio())
                .precioUnitario(producto.getPrecioVenta())
                .motivo(motivo)
                .usuario(usuario)
                .fechaMovimiento(LocalDateTime.now())
                .creadoEn(LocalDateTime.now())
                .build();

        movimientoInventarioRepository.save(movimiento);

        log.info("Movimiento de inventario registrado: {} - Producto: {} - Cantidad: {} - Usuario: {}",
                tipoMovimiento, producto.getCodigo(), cantidad, usuario.getUsername());
    }
}