package com.tienda.service;

import com.tienda.dto.request.ClienteRequest;
import com.tienda.dto.response.ClienteResponse;
import com.tienda.dto.response.PaginacionResponse;
import com.tienda.model.Cliente;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ClienteService {

    // CRUD
    ClienteResponse crearCliente(ClienteRequest request);
    ClienteResponse actualizarCliente(Long id, ClienteRequest request);
    void eliminarCliente(Long id);
    ClienteResponse obtenerClientePorId(Long id);
    ClienteResponse obtenerClientePorDocumento(String documento);
    List<ClienteResponse> obtenerTodosClientes();
    PaginacionResponse<ClienteResponse> obtenerClientesPaginados(Pageable pageable);

    // Búsquedas
    List<ClienteResponse> buscarClientesPorNombre(String nombre);
    List<ClienteResponse> obtenerClientesPorTipo(Cliente.TipoCliente tipo);
    List<ClienteResponse> obtenerClientesPorEstado(String estado);

    // Gestión
    void actualizarTotalCompras(Long clienteId, BigDecimal monto);
    void registrarUltimaCompra(Long clienteId);

    // Reportes
    List<ClienteResponse> obtenerTopClientes(int limite);
    List<ClienteResponse> obtenerClientesInactivos(int diasInactividad);
    Map<String, Object> obtenerEstadisticasClientes();

    // Validaciones
    boolean existeClientePorDocumento(String documento);

    // Métodos internos
    Cliente obtenerEntidadCliente(Long id);
}