package com.tienda.service.impl;

import com.tienda.dto.request.ProveedorRequest;
import com.tienda.dto.response.PaginacionResponse;
import com.tienda.dto.response.ProveedorResponse;
import com.tienda.exception.ResourceNotFoundException;
import com.tienda.exception.ValidacionException;
import com.tienda.model.Proveedor;
import com.tienda.repository.ProveedorRepository;
import com.tienda.service.ProveedorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository proveedorRepository;

    @Override
    @Transactional
    public ProveedorResponse crearProveedor(ProveedorRequest request) {
        // Validar código único
        if (existeProveedorPorCodigo(request.getCodigo())) {
            throw new ValidacionException("Ya existe un proveedor con ese código");
        }

        // Validar RUC único si se proporciona
        if (request.getRuc() != null && !request.getRuc().isEmpty()) {
            if (existeProveedorPorRuc(request.getRuc())) {
                throw new ValidacionException("Ya existe un proveedor con ese RUC");
            }
        }

        // Convertir estado
        Proveedor.EstadoProveedor estado = Proveedor.EstadoProveedor.ACTIVO;
        if (request.getEstado() != null) {
            try {
                estado = Proveedor.EstadoProveedor.valueOf(request.getEstado().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ValidacionException("Estado de proveedor inválido");
            }
        }

        // Crear proveedor
        Proveedor proveedor = Proveedor.builder()
                .codigo(request.getCodigo())
                .nombre(request.getNombre())
                .contacto(request.getContacto())
                .telefono(request.getTelefono())
                .email(request.getEmail())
                .ruc(request.getRuc())
                .direccion(request.getDireccion())
                .observaciones(request.getObservaciones())
                .estado(estado)
                .diasCredito(request.getDiasCredito() != null ? request.getDiasCredito() : 0)
                .limiteCredito(request.getLimiteCredito())
                .build();

        proveedor = proveedorRepository.save(proveedor);
        log.info("Proveedor creado: {} - {}", proveedor.getCodigo(), proveedor.getNombre());

        return convertirAResponse(proveedor);
    }

    @Override
    @Transactional
    public ProveedorResponse actualizarProveedor(Long id, ProveedorRequest request) {
        Proveedor proveedor = obtenerEntidadProveedor(id);

        // Validar código único si se cambia
        if (request.getCodigo() != null && !request.getCodigo().equals(proveedor.getCodigo())) {
            if (existeProveedorPorCodigo(request.getCodigo())) {
                throw new ValidacionException("Ya existe un proveedor con ese código");
            }
            proveedor.setCodigo(request.getCodigo());
        }

        // Validar RUC único si se cambia
        if (request.getRuc() != null && !request.getRuc().equals(proveedor.getRuc())) {
            if (existeProveedorPorRuc(request.getRuc())) {
                throw new ValidacionException("Ya existe un proveedor con ese RUC");
            }
            proveedor.setRuc(request.getRuc());
        }

        // Actualizar campos
        if (request.getNombre() != null) proveedor.setNombre(request.getNombre());
        if (request.getContacto() != null) proveedor.setContacto(request.getContacto());
        if (request.getTelefono() != null) proveedor.setTelefono(request.getTelefono());
        if (request.getEmail() != null) proveedor.setEmail(request.getEmail());
        if (request.getDireccion() != null) proveedor.setDireccion(request.getDireccion());
        if (request.getObservaciones() != null) proveedor.setObservaciones(request.getObservaciones());
        if (request.getDiasCredito() != null) proveedor.setDiasCredito(request.getDiasCredito());
        if (request.getLimiteCredito() != null) proveedor.setLimiteCredito(request.getLimiteCredito());

        // Actualizar estado
        if (request.getEstado() != null) {
            try {
                proveedor.setEstado(Proveedor.EstadoProveedor.valueOf(request.getEstado().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new ValidacionException("Estado de proveedor inválido");
            }
        }

        proveedor = proveedorRepository.save(proveedor);

        return convertirAResponse(proveedor);
    }

    @Override
    @Transactional
    public void eliminarProveedor(Long id) {
        Proveedor proveedor = obtenerEntidadProveedor(id);

        // Verificar si tiene productos asociados
        if (!proveedor.getProductos().isEmpty()) {
            throw new ValidacionException(
                    "No se puede eliminar el proveedor porque tiene productos asociados");
        }

        // Soft delete - cambiar estado
        proveedor.setEstado(Proveedor.EstadoProveedor.INACTIVO);
        proveedorRepository.save(proveedor);

        log.info("Proveedor marcado como inactivo: {}", proveedor.getNombre());
    }

    @Override
    @Transactional(readOnly = true)
    public ProveedorResponse obtenerProveedorPorId(Long id) {
        Proveedor proveedor = obtenerEntidadProveedor(id);
        return convertirAResponse(proveedor);
    }

    @Override
    @Transactional(readOnly = true)
    public ProveedorResponse obtenerProveedorPorCodigo(String codigo) {
        Proveedor proveedor = proveedorRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));
        return convertirAResponse(proveedor);
    }

    @Override
    @Transactional(readOnly = true)
    public ProveedorResponse obtenerProveedorPorRuc(String ruc) {
        Proveedor proveedor = proveedorRepository.findByRuc(ruc)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));
        return convertirAResponse(proveedor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorResponse> obtenerTodosProveedores() {
        return proveedorRepository.findAll().stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginacionResponse<ProveedorResponse> obtenerProveedoresPaginados(Pageable pageable) {
        Page<Proveedor> proveedoresPage = proveedorRepository.findAll(pageable);

        List<ProveedorResponse> proveedoresResponse = proveedoresPage.getContent().stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());

        return PaginacionResponse.<ProveedorResponse>builder()
                .content(proveedoresResponse)
                .pageNumber(proveedoresPage.getNumber())
                .pageSize(proveedoresPage.getSize())
                .totalElements(proveedoresPage.getTotalElements())
                .totalPages(proveedoresPage.getTotalPages())
                .last(proveedoresPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorResponse> buscarProveedoresPorNombre(String nombre) {
        Page<Proveedor> proveedoresPage = proveedorRepository.buscarConFiltros(
                nombre, null, null, Pageable.unpaged());
        return proveedoresPage.getContent().stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorResponse> obtenerProveedoresPorEstado(Proveedor.EstadoProveedor estado) {
        return proveedorRepository.findByEstado(estado).stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void activarProveedor(Long id) {
        Proveedor proveedor = obtenerEntidadProveedor(id);
        proveedor.setEstado(Proveedor.EstadoProveedor.ACTIVO);
        proveedorRepository.save(proveedor);
    }

    @Override
    @Transactional
    public void desactivarProveedor(Long id) {
        Proveedor proveedor = obtenerEntidadProveedor(id);
        proveedor.setEstado(Proveedor.EstadoProveedor.INACTIVO);
        proveedorRepository.save(proveedor);
    }

    @Override
    @Transactional
    public void suspenderProveedor(Long id, String motivo) {
        Proveedor proveedor = obtenerEntidadProveedor(id);
        proveedor.setEstado(Proveedor.EstadoProveedor.SUSPENDIDO);
        proveedorRepository.save(proveedor);

        log.info("Proveedor suspendido: {} - Motivo: {}", proveedor.getNombre(), motivo);
    }

    @Override
    @Transactional(readOnly = true)
    public Long contarProveedoresActivos() {
        return proveedorRepository.contarProveedoresActivos();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticasProveedores() {
        Map<String, Object> estadisticas = new HashMap<>();

        List<Proveedor> proveedores = proveedorRepository.findAll();

        long totalProveedores = proveedores.size();
        long proveedoresActivos = proveedores.stream()
                .filter(p -> p.getEstado() == Proveedor.EstadoProveedor.ACTIVO)
                .count();
        long proveedoresConCredito = proveedores.stream()
                .filter(p -> p.getDiasCredito() > 0)
                .count();

        BigDecimal totalLimiteCredito = proveedores.stream()
                .map(Proveedor::getLimiteCredito)
                .filter(limite -> limite != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        estadisticas.put("totalProveedores", totalProveedores);
        estadisticas.put("proveedoresActivos", proveedoresActivos);
        estadisticas.put("proveedoresInactivos", totalProveedores - proveedoresActivos);
        estadisticas.put("proveedoresConCredito", proveedoresConCredito);
        estadisticas.put("totalLimiteCredito", totalLimiteCredito);
        estadisticas.put("promedioProductosPorProveedor", calcularPromedioProductos(proveedores));

        return estadisticas;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeProveedorPorCodigo(String codigo) {
        return proveedorRepository.existsByCodigo(codigo);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeProveedorPorRuc(String ruc) {
        return proveedorRepository.existsByRuc(ruc);
    }

    @Override
    @Transactional(readOnly = true)
    public Proveedor obtenerEntidadProveedor(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con ID: " + id));
    }

    // Métodos privados auxiliares
    private ProveedorResponse convertirAResponse(Proveedor proveedor) {
        return ProveedorResponse.builder()
                .id(proveedor.getId())
                .codigo(proveedor.getCodigo())
                .nombre(proveedor.getNombre())
                .contacto(proveedor.getContacto())
                .telefono(proveedor.getTelefono())
                .email(proveedor.getEmail())
                .ruc(proveedor.getRuc())
                .direccion(proveedor.getDireccion())
                .observaciones(proveedor.getObservaciones())
                .estado(proveedor.getEstado().name())
                .diasCredito(proveedor.getDiasCredito())
                .limiteCredito(proveedor.getLimiteCredito())
                .totalProductos(proveedor.getProductos().size())
                .build();
    }

    private double calcularPromedioProductos(List<Proveedor> proveedores) {
        if (proveedores.isEmpty()) {
            return 0.0;
        }

        int totalProductos = proveedores.stream()
                .mapToInt(p -> p.getProductos().size())
                .sum();

        return (double) totalProductos / proveedores.size();
    }
}