package com.tienda.service;

import com.tienda.dto.request.ProveedorRequest;
import com.tienda.dto.response.ProveedorResponse;
import com.tienda.dto.response.PaginacionResponse;
import com.tienda.model.Proveedor;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface ProveedorService {

    // CRUD
    ProveedorResponse crearProveedor(ProveedorRequest request);
    ProveedorResponse actualizarProveedor(Long id, ProveedorRequest request);
    void eliminarProveedor(Long id);
    ProveedorResponse obtenerProveedorPorId(Long id);
    ProveedorResponse obtenerProveedorPorCodigo(String codigo);
    ProveedorResponse obtenerProveedorPorRuc(String ruc);
    List<ProveedorResponse> obtenerTodosProveedores();
    PaginacionResponse<ProveedorResponse> obtenerProveedoresPaginados(Pageable pageable);

    // Búsquedas
    List<ProveedorResponse> buscarProveedoresPorNombre(String nombre);
    List<ProveedorResponse> obtenerProveedoresPorEstado(Proveedor.EstadoProveedor estado);

    // Gestión de estado
    void activarProveedor(Long id);
    void desactivarProveedor(Long id);
    void suspenderProveedor(Long id, String motivo);

    // Estadísticas
    Long contarProveedoresActivos();
    Map<String, Object> obtenerEstadisticasProveedores();

    // Validaciones
    boolean existeProveedorPorCodigo(String codigo);
    boolean existeProveedorPorRuc(String ruc);

    // Métodos internos
    Proveedor obtenerEntidadProveedor(Long id);
}