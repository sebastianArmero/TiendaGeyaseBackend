package com.tienda.service;

import com.tienda.dto.request.CrearUsuarioRequest;
import com.tienda.dto.request.ActualizarUsuarioRequest;
import com.tienda.dto.response.UsuarioResponse;
import com.tienda.dto.response.PaginacionResponse;
import com.tienda.model.Usuario;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UsuarioService {

    // CRUD
    UsuarioResponse crearUsuario(CrearUsuarioRequest request);
    UsuarioResponse actualizarUsuario(Long id, ActualizarUsuarioRequest request);
    void eliminarUsuario(Long id);
    UsuarioResponse obtenerUsuarioPorId(Long id);
    List<UsuarioResponse> obtenerTodosUsuarios();
    PaginacionResponse<UsuarioResponse> obtenerUsuariosPaginados(Pageable pageable);

    // Búsquedas
    UsuarioResponse obtenerUsuarioPorUsername(String username);
    UsuarioResponse obtenerUsuarioPorEmail(String email);
    List<UsuarioResponse> buscarUsuariosPorNombre(String nombre);

    // Gestión de estado
    void activarUsuario(Long id);
    void desactivarUsuario(Long id);
    void bloquearUsuario(Long id, String motivo);
    void desbloquearUsuario(Long id);

    // Roles y permisos
    void asignarRol(Long usuarioId, Long rolId);
    void quitarRol(Long usuarioId);

    // Contraseñas
    void resetearPassword(Long id);
    void cambiarPasswordUsuario(Long id, String nuevaPassword);
    void forzarCambioPassword(Long id);

    // Validaciones
    boolean existeUsuarioPorUsername(String username);
    boolean existeUsuarioPorEmail(String email);

    // Métodos internos (para otros services)
    Usuario obtenerEntidadUsuario(Long id);
    Usuario obtenerEntidadUsuarioPorUsername(String username);
}