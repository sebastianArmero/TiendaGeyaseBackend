package com.tienda.service.impl;

import com.tienda.dto.request.ClienteRequest;
import com.tienda.dto.response.ClienteResponse;
import com.tienda.dto.response.PaginacionResponse;
import com.tienda.exception.ResourceNotFoundException;
import com.tienda.exception.ValidacionException;
import com.tienda.model.Cliente;
import com.tienda.repository.ClienteRepository;
import com.tienda.service.ClienteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;

    @Override
    @Transactional
    public ClienteResponse crearCliente(ClienteRequest request) {
        // Validar documento único si se proporciona
        if (request.getNumeroDocumento() != null && !request.getNumeroDocumento().isEmpty()) {
            if (existeClientePorDocumento(request.getNumeroDocumento())) {
                throw new ValidacionException("Ya existe un cliente con ese documento");
            }
        }

        // Convertir tipo de cliente
        Cliente.TipoCliente tipo = Cliente.TipoCliente.OCASIONAL;
        if (request.getTipo() != null) {
            try {
                tipo = Cliente.TipoCliente.valueOf(request.getTipo().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ValidacionException("Tipo de cliente inválido");
            }
        }

        // Crear cliente
        Cliente cliente = Cliente.builder()
                .tipoDocumento(request.getTipoDocumento() != null ?
                        request.getTipoDocumento() : "DNI")
                .numeroDocumento(request.getNumeroDocumento())
                .nombre(request.getNombre())
                .email(request.getEmail())
                .telefono(request.getTelefono())
                .direccion(request.getDireccion())
                .tipo(tipo)
                .estado(request.getEstado() != null ? request.getEstado() : "ACTIVO")
                .totalCompras(BigDecimal.ZERO)
                .build();

        cliente = clienteRepository.save(cliente);
        log.info("Cliente creado: {} - {}", cliente.getNombre(), cliente.getNumeroDocumento());

        return convertirAResponse(cliente);
    }

    @Override
    @Transactional
    public ClienteResponse actualizarCliente(Long id, ClienteRequest request) {
        Cliente cliente = obtenerEntidadCliente(id);

        // Validar documento único si se cambia
        if (request.getNumeroDocumento() != null &&
                !request.getNumeroDocumento().equals(cliente.getNumeroDocumento())) {
            if (existeClientePorDocumento(request.getNumeroDocumento())) {
                throw new ValidacionException("Ya existe un cliente con ese documento");
            }
            cliente.setNumeroDocumento(request.getNumeroDocumento());
        }

        // Actualizar campos
        if (request.getNombre() != null) cliente.setNombre(request.getNombre());
        if (request.getEmail() != null) cliente.setEmail(request.getEmail());
        if (request.getTelefono() != null) cliente.setTelefono(request.getTelefono());
        if (request.getDireccion() != null) cliente.setDireccion(request.getDireccion());
        if (request.getTipoDocumento() != null) cliente.setTipoDocumento(request.getTipoDocumento());
        if (request.getEstado() != null) cliente.setEstado(request.getEstado());

        // Actualizar tipo
        if (request.getTipo() != null) {
            try {
                cliente.setTipo(Cliente.TipoCliente.valueOf(request.getTipo().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new ValidacionException("Tipo de cliente inválido");
            }
        }

        cliente = clienteRepository.save(cliente);

        return convertirAResponse(cliente);
    }

    @Override
    @Transactional
    public void eliminarCliente(Long id) {
        Cliente cliente = obtenerEntidadCliente(id);

        // Verificar si tiene ventas asociadas
        if (!cliente.getVentas().isEmpty()) {
            // Soft delete - cambiar estado
            cliente.setEstado("INACTIVO");
            clienteRepository.save(cliente);
            log.info("Cliente marcado como inactivo: {}", cliente.getNombre());
        } else {
            // Hard delete si no tiene ventas
            clienteRepository.delete(cliente);
            log.info("Cliente eliminado: {}", cliente.getNombre());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse obtenerClientePorId(Long id) {
        Cliente cliente = obtenerEntidadCliente(id);
        return convertirAResponse(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse obtenerClientePorDocumento(String documento) {
        Cliente cliente = clienteRepository.findByNumeroDocumento(documento)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        return convertirAResponse(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> obtenerTodosClientes() {
        return clienteRepository.findAll().stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginacionResponse<ClienteResponse> obtenerClientesPaginados(Pageable pageable) {
        Page<Cliente> clientesPage = clienteRepository.findAll(pageable);

        List<ClienteResponse> clientesResponse = clientesPage.getContent().stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());

        return PaginacionResponse.<ClienteResponse>builder()
                .content(clientesResponse)
                .pageNumber(clientesPage.getNumber())
                .pageSize(clientesPage.getSize())
                .totalElements(clientesPage.getTotalElements())
                .totalPages(clientesPage.getTotalPages())
                .last(clientesPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> buscarClientesPorNombre(String nombre) {
        Page<Cliente> clientesPage = clienteRepository.buscarConFiltros(
                nombre, null, null, Pageable.unpaged());
        return clientesPage.getContent().stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> obtenerClientesPorTipo(Cliente.TipoCliente tipo) {
        return clienteRepository.findByTipo(tipo).stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> obtenerClientesPorEstado(String estado) {
        return clienteRepository.findByEstado(estado).stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void actualizarTotalCompras(Long clienteId, BigDecimal monto) {
        Cliente cliente = obtenerEntidadCliente(clienteId);

        BigDecimal nuevoTotal = cliente.getTotalCompras().add(monto);
        cliente.setTotalCompras(nuevoTotal);

        clienteRepository.save(cliente);

        log.debug("Total de compras actualizado para cliente {}: {}",
                cliente.getNombre(), nuevoTotal);
    }

    @Override
    @Transactional
    public void registrarUltimaCompra(Long clienteId) {
        Cliente cliente = obtenerEntidadCliente(clienteId);
        cliente.setUltimaCompra(LocalDateTime.now());
        clienteRepository.save(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> obtenerTopClientes(int limite) {
        Page<Cliente> clientesPage = clienteRepository.findTopClientes(
                Pageable.ofSize(limite));

        return clientesPage.getContent().stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> obtenerClientesInactivos(int diasInactividad) {
        LocalDateTime fechaLimite = LocalDateTime.now().minusDays(diasInactividad);
        return clienteRepository.findClientesInactivos(fechaLimite).stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticasClientes() {
        Map<String, Object> estadisticas = new HashMap<>();

        List<Cliente> clientes = clienteRepository.findAll();

        long totalClientes = clientes.size();
        long clientesActivos = clienteRepository.contarClientesActivos();

        // Calcular por tipo
        Map<Cliente.TipoCliente, Long> clientesPorTipo = clientes.stream()
                .collect(Collectors.groupingBy(Cliente::getTipo, Collectors.counting()));

        // Calcular ventas totales
        BigDecimal ventasTotales = clientes.stream()
                .map(Cliente::getTotalCompras)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calcular promedio de compras
        BigDecimal promedioCompras = totalClientes > 0 ?
                ventasTotales.divide(new BigDecimal(totalClientes), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        // Clientes con más compras
        List<ClienteResponse> topClientes = obtenerTopClientes(5);

        estadisticas.put("totalClientes", totalClientes);
        estadisticas.put("clientesActivos", clientesActivos);
        estadisticas.put("clientesInactivos", totalClientes - clientesActivos);
        estadisticas.put("clientesPorTipo", clientesPorTipo);
        estadisticas.put("ventasTotales", ventasTotales);
        estadisticas.put("promedioComprasPorCliente", promedioCompras);
        estadisticas.put("topClientes", topClientes);
        estadisticas.put("fechaActualizacion", LocalDateTime.now());

        return estadisticas;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeClientePorDocumento(String documento) {
        return clienteRepository.existsByNumeroDocumento(documento);
    }

    @Override
    @Transactional(readOnly = true)
    public Cliente obtenerEntidadCliente(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id));
    }

    // Métodos privados auxiliares
    private ClienteResponse convertirAResponse(Cliente cliente) {
        // Calcular días desde última compra
        Integer diasUltimaCompra = null;
        if (cliente.getUltimaCompra() != null) {
            diasUltimaCompra = (int) ChronoUnit.DAYS.between(
                    cliente.getUltimaCompra(), LocalDateTime.now());
        }

        // Calcular promedio del ticket
        BigDecimal promedioTicket = BigDecimal.ZERO;
        Integer totalVentas = cliente.getVentas().size();
        if (totalVentas > 0 && cliente.getTotalCompras().compareTo(BigDecimal.ZERO) > 0) {
            promedioTicket = cliente.getTotalCompras()
                    .divide(new BigDecimal(totalVentas), 2, RoundingMode.HALF_UP);
        }

        return ClienteResponse.builder()
                .id(cliente.getId())
                .tipoDocumento(cliente.getTipoDocumento())
                .numeroDocumento(cliente.getNumeroDocumento())
                .nombre(cliente.getNombre())
                .email(cliente.getEmail())
                .telefono(cliente.getTelefono())
                .direccion(cliente.getDireccion())
                .tipo(cliente.getTipo().name())
                .fechaRegistro(cliente.getFechaRegistro())
                .totalCompras(cliente.getTotalCompras())
                .ultimaCompra(cliente.getUltimaCompra())
                .estado(cliente.getEstado())
                .totalVentas(totalVentas)
                .promedioTicket(promedioTicket)
                .diasUltimaCompra(diasUltimaCompra)
                .build();
    }
}