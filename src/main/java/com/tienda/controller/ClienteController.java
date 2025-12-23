package com.tienda.controller;

import com.tienda.dto.request.ClienteRequest;
import com.tienda.dto.response.ClienteResponse;
import com.tienda.dto.response.PaginacionResponse;
import com.tienda.model.Cliente;
import com.tienda.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "API para gestión de clientes")
public class ClienteController {

    private final ClienteService clienteService;

    @Operation(summary = "Crear un nuevo cliente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cliente creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "409", description = "Ya existe un cliente con ese documento")
    })
    @PostMapping
    public ResponseEntity<ClienteResponse> crearCliente(
            @Valid @RequestBody ClienteRequest request) {
        ClienteResponse response = clienteService.crearCliente(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Obtener cliente por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> obtenerClientePorId(
            @Parameter(description = "ID del cliente") @PathVariable Long id) {
        ClienteResponse response = clienteService.obtenerClientePorId(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener cliente por número de documento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    @GetMapping("/documento/{documento}")
    public ResponseEntity<ClienteResponse> obtenerClientePorDocumento(
            @Parameter(description = "Número de documento") @PathVariable String documento) {
        ClienteResponse response = clienteService.obtenerClientePorDocumento(documento);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Actualizar cliente existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
            @ApiResponse(responseCode = "409", description = "Ya existe un cliente con ese documento")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponse> actualizarCliente(
            @Parameter(description = "ID del cliente") @PathVariable Long id,
            @Valid @RequestBody ClienteRequest request) {
        ClienteResponse response = clienteService.actualizarCliente(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Eliminar cliente (soft delete si tiene ventas)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cliente eliminado/marcado como inactivo"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCliente(
            @Parameter(description = "ID del cliente") @PathVariable Long id) {
        clienteService.eliminarCliente(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Obtener todos los clientes")
    @GetMapping
    public ResponseEntity<List<ClienteResponse>> obtenerTodosClientes() {
        List<ClienteResponse> response = clienteService.obtenerTodosClientes();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener clientes paginados")
    @GetMapping("/paginados")
    public ResponseEntity<PaginacionResponse<ClienteResponse>> obtenerClientesPaginados(
            @Parameter(description = "Número de página (0-index)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo por el cual ordenar") @RequestParam(defaultValue = "nombre") String sortBy,
            @Parameter(description = "Dirección de orden (asc/desc)") @RequestParam(defaultValue = "asc") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ?
                Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        PaginacionResponse<ClienteResponse> response = clienteService.obtenerClientesPaginados(pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Buscar clientes por nombre")
    @GetMapping("/buscar")
    public ResponseEntity<List<ClienteResponse>> buscarClientesPorNombre(
            @Parameter(description = "Nombre o parte del nombre") @RequestParam String nombre) {
        List<ClienteResponse> response = clienteService.buscarClientesPorNombre(nombre);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener clientes por tipo")
    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<List<ClienteResponse>> obtenerClientesPorTipo(
            @Parameter(description = "Tipo de cliente") @PathVariable Cliente.TipoCliente tipo) {
        List<ClienteResponse> response = clienteService.obtenerClientesPorTipo(tipo);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener clientes por estado")
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<ClienteResponse>> obtenerClientesPorEstado(
            @Parameter(description = "Estado del cliente (ACTIVO/INACTIVO)") @PathVariable String estado) {
        List<ClienteResponse> response = clienteService.obtenerClientesPorEstado(estado);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener top N clientes con más compras")
    @GetMapping("/top/{limite}")
    public ResponseEntity<List<ClienteResponse>> obtenerTopClientes(
            @Parameter(description = "Número de clientes a retornar") @PathVariable int limite) {
        List<ClienteResponse> response = clienteService.obtenerTopClientes(limite);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener clientes inactivos")
    @GetMapping("/inactivos")
    public ResponseEntity<List<ClienteResponse>> obtenerClientesInactivos(
            @Parameter(description = "Días de inactividad") @RequestParam(defaultValue = "30") int dias) {
        List<ClienteResponse> response = clienteService.obtenerClientesInactivos(dias);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener estadísticas de clientes")
    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticasClientes() {
        Map<String, Object> response = clienteService.obtenerEstadisticasClientes();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Verificar si existe un cliente por documento")
    @GetMapping("/existe/{documento}")
    public ResponseEntity<Boolean> existeClientePorDocumento(
            @Parameter(description = "Número de documento") @PathVariable String documento) {
        boolean existe = clienteService.existeClientePorDocumento(documento);
        return ResponseEntity.ok(existe);
    }
}