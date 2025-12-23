package com.tienda.service.impl;

import com.tienda.dto.request.ProductoRequest;
import com.tienda.dto.response.PaginacionResponse;
import com.tienda.dto.response.ProductoResponse;
import com.tienda.exception.ResourceNotFoundException;
import com.tienda.exception.ValidacionException;
import com.tienda.model.*;
import com.tienda.repository.*;
import com.tienda.service.ProductoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProveedorRepository proveedorRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public ProductoResponse crearProducto(ProductoRequest request) {
        // Validar código único
        if (existeProductoPorCodigo(request.getCodigo())) {
            throw new ValidacionException("Ya existe un producto con ese código");
        }

        // Validar código de barras único si se proporciona
        if (request.getCodigoBarras() != null && !request.getCodigoBarras().isEmpty()) {
            if (existeProductoPorCodigoBarras(request.getCodigoBarras())) {
                throw new ValidacionException("Ya existe un producto con ese código de barras");
            }
        }

        // Buscar categoría
        Categoria categoria = null;
        if (request.getCategoriaId() != null) {
            categoria = categoriaRepository.findById(request.getCategoriaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
        }

        // Buscar proveedor
        Proveedor proveedor = null;
        if (request.getProveedorId() != null) {
            proveedor = proveedorRepository.findById(request.getProveedorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));
        }

        // Convertir estado
        Producto.EstadoProducto estado = Producto.EstadoProducto.ACTIVO;
        if (request.getEstado() != null) {
            try {
                estado = Producto.EstadoProducto.valueOf(request.getEstado().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ValidacionException("Estado de producto inválido");
            }
        }

        // Crear producto
        Producto producto = Producto.builder()
                .codigo(request.getCodigo())
                .codigoBarras(request.getCodigoBarras())
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .categoria(categoria)
                .subcategoria(request.getSubcategoria())
                .marca(request.getMarca())
                .modelo(request.getModelo())
                .stockActual(request.getStockActual() != null ? request.getStockActual() : BigDecimal.ZERO)
                .stockMinimo(request.getStockMinimo() != null ? request.getStockMinimo() : new BigDecimal("5"))
                .stockMaximo(request.getStockMaximo())
                .precioVenta(request.getPrecioVenta())
                .costoPromedio(request.getCostoPromedio() != null ? request.getCostoPromedio() : BigDecimal.ZERO)
                .precioVenta2(request.getPrecioVenta2())
                .precioVenta3(request.getPrecioVenta3())
                .unidadMedida(request.getUnidadMedida() != null ? request.getUnidadMedida() : "UNIDAD")
                .tipoProducto(request.getTipoProducto() != null ?
                        Producto.TipoProducto.valueOf(request.getTipoProducto().toUpperCase()) :
                        Producto.TipoProducto.NORMAL)
                .permiteDecimal(request.getPermiteDecimal() != null ? request.getPermiteDecimal() : false)
                .proveedor(proveedor)
                .ubicacion(request.getUbicacion())
                .estado(estado)
                .build();

        // Calcular estado de stock
        producto.calcularEstadoStock();

        producto = productoRepository.save(producto);
        log.info("Producto creado: {} - {}", producto.getCodigo(), producto.getNombre());

        return convertirAResponse(producto);
    }

    @Override
    @Transactional
    public ProductoResponse actualizarProducto(Long id, ProductoRequest request) {
        Producto producto = obtenerEntidadProducto(id);

        // Validar código único si se cambia
        if (request.getCodigo() != null && !request.getCodigo().equals(producto.getCodigo())) {
            if (existeProductoPorCodigo(request.getCodigo())) {
                throw new ValidacionException("Ya existe un producto con ese código");
            }
            producto.setCodigo(request.getCodigo());
        }

        // Validar código de barras único si se cambia
        if (request.getCodigoBarras() != null &&
                !request.getCodigoBarras().equals(producto.getCodigoBarras())) {
            if (existeProductoPorCodigoBarras(request.getCodigoBarras())) {
                throw new ValidacionException("Ya existe un producto con ese código de barras");
            }
            producto.setCodigoBarras(request.getCodigoBarras());
        }

        // Actualizar campos básicos
        if (request.getNombre() != null) producto.setNombre(request.getNombre());
        if (request.getDescripcion() != null) producto.setDescripcion(request.getDescripcion());
        if (request.getSubcategoria() != null) producto.setSubcategoria(request.getSubcategoria());
        if (request.getMarca() != null) producto.setMarca(request.getMarca());
        if (request.getModelo() != null) producto.setModelo(request.getModelo());

        // Actualizar categoría
        if (request.getCategoriaId() != null) {
            Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
            producto.setCategoria(categoria);
        }

        // Actualizar proveedor
        if (request.getProveedorId() != null) {
            Proveedor proveedor = proveedorRepository.findById(request.getProveedorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));
            producto.setProveedor(proveedor);
        }

        // Actualizar stock
        if (request.getStockActual() != null) {
            producto.setStockActual(request.getStockActual());
        }
        if (request.getStockMinimo() != null) {
            producto.setStockMinimo(request.getStockMinimo());
        }
        if (request.getStockMaximo() != null) {
            producto.setStockMaximo(request.getStockMaximo());
        }

        // Actualizar precios
        if (request.getPrecioVenta() != null) {
            producto.setPrecioVenta(request.getPrecioVenta());
        }
        if (request.getCostoPromedio() != null) {
            producto.setCostoPromedio(request.getCostoPromedio());
        }
        if (request.getPrecioVenta2() != null) {
            producto.setPrecioVenta2(request.getPrecioVenta2());
        }
        if (request.getPrecioVenta3() != null) {
            producto.setPrecioVenta3(request.getPrecioVenta3());
        }

        // Actualizar otros campos
        if (request.getUnidadMedida() != null) {
            producto.setUnidadMedida(request.getUnidadMedida());
        }
        if (request.getTipoProducto() != null) {
            producto.setTipoProducto(Producto.TipoProducto.valueOf(request.getTipoProducto().toUpperCase()));
        }
        if (request.getPermiteDecimal() != null) {
            producto.setPermiteDecimal(request.getPermiteDecimal());
        }
        if (request.getUbicacion() != null) {
            producto.setUbicacion(request.getUbicacion());
        }
        if (request.getEstado() != null) {
            producto.setEstado(Producto.EstadoProducto.valueOf(request.getEstado().toUpperCase()));
        }

        // Recalcular estado
        producto.calcularEstadoStock();

        producto = productoRepository.save(producto);

        return convertirAResponse(producto);
    }

    @Override
    @Transactional
    public void eliminarProducto(Long id) {
        Producto producto = obtenerEntidadProducto(id);

        // Verificar si tiene movimientos o ventas asociadas
        // (En una implementación real, verificarías dependencias)

        // Soft delete - marcar como inactivo
        producto.setEstado(Producto.EstadoProducto.INACTIVO);
        productoRepository.save(producto);

        log.info("Producto marcado como inactivo: {}", producto.getCodigo());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponse obtenerProductoPorId(Long id) {
        Producto producto = obtenerEntidadProducto(id);
        return convertirAResponse(producto);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponse obtenerProductoPorCodigo(String codigo) {
        Producto producto = productoRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        return convertirAResponse(producto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> obtenerTodosProductos() {
        return productoRepository.findAll().stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginacionResponse<ProductoResponse> obtenerProductosPaginados(Pageable pageable) {
        Page<Producto> productosPage = productoRepository.findAll(pageable);

        List<ProductoResponse> productosResponse = productosPage.getContent().stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());

        return PaginacionResponse.<ProductoResponse>builder()
                .content(productosResponse)
                .pageNumber(productosPage.getNumber())
                .pageSize(productosPage.getSize())
                .totalElements(productosPage.getTotalElements())
                .totalPages(productosPage.getTotalPages())
                .last(productosPage.isLast())
                .first(productosPage.isFirst())
                .empty(productosPage.isEmpty())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> buscarProductosPorNombre(String nombre) {
        Page<Producto> productos = productoRepository.buscarConFiltros(
                null, nombre, null, null, Pageable.unpaged());
        return productos.getContent().stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> buscarProductosPorCodigo(String codigo) {
        Page<Producto> productos = productoRepository.buscarConFiltros(
                codigo, null, null, null, Pageable.unpaged());
        return productos.getContent().stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginacionResponse<ProductoResponse> filtrarProductos(
            String codigo, String nombre, Long categoriaId,
            Producto.EstadoProducto estado, Pageable pageable) {

        Page<Producto> productosPage = productoRepository.buscarConFiltros(
                codigo, nombre, categoriaId, estado, pageable);

        List<ProductoResponse> productosResponse = productosPage.getContent().stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());

        return PaginacionResponse.<ProductoResponse>builder()
                .content(productosResponse)
                .pageNumber(productosPage.getNumber())
                .pageSize(productosPage.getSize())
                .totalElements(productosPage.getTotalElements())
                .totalPages(productosPage.getTotalPages())
                .last(productosPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public ProductoResponse ajustarStock(Long productoId, BigDecimal cantidad, String motivo) {
        Producto producto = obtenerEntidadProducto(productoId); // ✅ Cambié 'id' por 'productoId'
        BigDecimal cantidadAnterior = producto.getStockActual();
        BigDecimal cantidadNueva = cantidad;

        producto.setStockActual(cantidadNueva);
        producto.calcularEstadoStock();

        // Registrar movimiento
        registrarMovimiento(producto, "AJUSTE", cantidad, cantidadAnterior,
                cantidadNueva, motivo, 1L);

        producto = productoRepository.save(producto);

        return convertirAResponse(producto);
    }

    @Override
    @Transactional
    public ProductoResponse incrementarStock(Long productoId, BigDecimal cantidad, String motivo) {
        Producto producto = obtenerEntidadProducto(productoId); // ✅ Cambié 'id' por 'productoId'
        BigDecimal cantidadAnterior = producto.getStockActual();
        BigDecimal cantidadNueva = cantidadAnterior.add(cantidad);

        producto.setStockActual(cantidadNueva);
        producto.calcularEstadoStock();

        registrarMovimiento(producto, "ENTRADA", cantidad, cantidadAnterior,
                cantidadNueva, motivo, 1L);

        producto = productoRepository.save(producto);

        return convertirAResponse(producto);
    }

    @Override
    @Transactional
    public ProductoResponse decrementarStock(Long productoId, BigDecimal cantidad, String motivo) {
        Producto producto = obtenerEntidadProducto(productoId); // ✅ Cambié 'id' por 'productoId'

        if (!producto.tieneStockSuficiente(cantidad)) {
            throw new ValidacionException("Stock insuficiente. Disponible: " +
                    producto.getStockDisponible());
        }

        BigDecimal cantidadAnterior = producto.getStockActual();
        BigDecimal cantidadNueva = cantidadAnterior.subtract(cantidad);

        producto.setStockActual(cantidadNueva);
        producto.calcularEstadoStock();

        registrarMovimiento(producto, "SALIDA", cantidad, cantidadAnterior,
                cantidadNueva, motivo, 1L);

        producto = productoRepository.save(producto);

        return convertirAResponse(producto);
    }

    @Override
    @Transactional
    public ProductoResponse actualizarPrecio(Long productoId, BigDecimal nuevoPrecio) {
        Producto producto = obtenerEntidadProducto(productoId); // ✅ Cambié 'id' por 'productoId'

        if (nuevoPrecio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacionException("El precio debe ser mayor a cero");
        }

        BigDecimal precioAnterior = producto.getPrecioVenta();
        producto.setPrecioVenta(nuevoPrecio);
        producto.calcularEstadoStock();

        producto = productoRepository.save(producto);
        log.info("Precio actualizado para producto {}: {} -> {}",
                producto.getCodigo(), precioAnterior, nuevoPrecio);

        return convertirAResponse(producto);
    }

    @Override
    public Map<String, Object> importarProductosDesdeExcel(MultipartFile archivo) {
        // Implementación básica - en producción usarías Apache POI
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("mensaje", "Importación de Excel pendiente de implementar");
        resultado.put("archivo", archivo.getOriginalFilename());
        return resultado;
    }

    @Override
    public byte[] exportarProductosAExcel() {
        // Implementación básica - en producción usarías Apache POI
        return new byte[0];
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeProductoPorCodigo(String codigo) {
        return productoRepository.existsByCodigo(codigo);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeProductoPorCodigoBarras(String codigoBarras) {
        return productoRepository.existsByCodigoBarras(codigoBarras);
    }

    @Override
    @Transactional(readOnly = true)
    public Producto obtenerEntidadProducto(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verificarStockDisponible(Long productoId, BigDecimal cantidad) {
        Producto producto = obtenerEntidadProducto(productoId);
        return producto.tieneStockSuficiente(cantidad);
    }

    @Override
    @Transactional(readOnly = true)
    public Long contarProductosActivos() {
        return productoRepository.findAll().stream()
                .filter(p -> p.getEstado() == Producto.EstadoProducto.ACTIVO)
                .count();
    }

    @Override
    @Transactional(readOnly = true)
    public Long contarProductosBajoStock() {
        return (long) productoRepository.findProductosStockBajo().size();
    }

    // Métodos privados auxiliares
    private ProductoResponse convertirAResponse(Producto producto) {
        return ProductoResponse.builder()
                .id(producto.getId())
                .codigo(producto.getCodigo())
                .codigoBarras(producto.getCodigoBarras())
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .categoriaNombre(producto.getCategoria() != null ? producto.getCategoria().getNombre() : null)
                .categoriaId(producto.getCategoria() != null ? producto.getCategoria().getId() : null)
                .subcategoria(producto.getSubcategoria())
                .stockActual(producto.getStockActual())
                .stockDisponible(producto.getStockDisponible())
                .stockReservado(producto.getStockReservado())
                .stockMinimo(producto.getStockMinimo())
                .stockMaximo(producto.getStockMaximo())
                .costoPromedio(producto.getCostoPromedio())
                .costoUltimo(producto.getCostoUltimo())
                .precioVenta(producto.getPrecioVenta())
                .precioVenta2(producto.getPrecioVenta2())
                .precioVenta3(producto.getPrecioVenta3())
                .margenGanancia(producto.getMargenGanancia())
                .unidadMedida(producto.getUnidadMedida())
                .tipoProducto(producto.getTipoProducto().name())
                .permiteDecimal(producto.getPermiteDecimal())
                .proveedorNombre(producto.getProveedor() != null ? producto.getProveedor().getNombre() : null)
                .proveedorId(producto.getProveedor() != null ? producto.getProveedor().getId() : null)
                .ubicacion(producto.getUbicacion())
                .estado(producto.getEstado().name())
                .alertaStock(producto.getAlertaStock().name())
                .estadoCalculado(calcularEstado(producto))
                .valorCosto(producto.getValorCosto())
                .valorVenta(producto.getValorVenta())
                .utilidadPotencial(producto.getUtilidadPotencial())
                .necesitaReorden(producto.getStockActual().compareTo(producto.getStockMinimo()) <= 0)
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

    private void registrarMovimiento(Producto producto, String tipo, BigDecimal cantidad,
                                     BigDecimal cantidadAnterior, BigDecimal cantidadNueva,
                                     String motivo, Long usuarioId) {

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElse(null);

        MovimientoInventario movimiento = MovimientoInventario.builder()
                .producto(producto)
                .tipoMovimiento(tipo)
                .cantidad(cantidad)
                .cantidadAnterior(cantidadAnterior)
                .cantidadNueva(cantidadNueva)
                .costoUnitario(producto.getCostoPromedio())
                .precioUnitario(producto.getPrecioVenta())
                .motivo(motivo)
                .usuario(usuario)
                .build();

        movimientoInventarioRepository.save(movimiento);

        log.info("Movimiento registrado: {} - Producto: {} - Cantidad: {}",
                tipo, producto.getCodigo(), cantidad);
    }
}