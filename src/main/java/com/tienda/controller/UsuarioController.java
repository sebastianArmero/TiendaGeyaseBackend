package com.tienda.controller;

import com.tienda.dto.request.CrearUsuarioRequest;
import com.tienda.dto.request.ActualizarUsuarioRequest;
import com.tienda.dto.response.ApiResponse;
import com.tienda.dto.response.PaginacionResponse;
import com.tienda.dto.response.UsuarioResponse;
import com.tienda.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> crearUsuario(@Valid @RequestBody CrearUsuarioRequest request) {
        try {
            UsuarioResponse usuario = usuarioService.crearUsuario(request);
            return ResponseEntity.ok(ApiResponse.success("Usuario creado exitosamente", usuario));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.esPropietario(#id)")
    public ResponseEntity<ApiResponse> actualizarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarUsuarioRequest request) {
        try {
            UsuarioResponse usuario = usuarioService.actualizarUsuario(id, request);
            return ResponseEntity.ok(ApiResponse.success("Usuario actualizado exitosamente", usuario));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> eliminarUsuario(@PathVariable Long id) {
        try {
            usuarioService.eliminarUsuario(id);
            return ResponseEntity.ok(ApiResponse.success("Usuario eliminado exitosamente", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.esPropietario(#id)")
    public ResponseEntity<ApiResponse> obtenerUsuario(@PathVariable Long id) {
        try {
            UsuarioResponse usuario = usuarioService.obtenerUsuarioPorId(id);
            return ResponseEntity.ok(ApiResponse.success("Usuario obtenido", usuario));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> obtenerTodosUsuarios(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        try {
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ?
                    Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            PaginacionResponse<UsuarioResponse> usuarios =
                    usuarioService.obtenerUsuariosPaginados(pageable);

            return ResponseEntity.ok(ApiResponse.success("Usuarios obtenidos", usuarios));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/buscar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> buscarUsuarios(@RequestParam String nombre) {
        try {
            List<UsuarioResponse> usuarios = usuarioService.buscarUsuariosPorNombre(nombre);
            return ResponseEntity.ok(ApiResponse.success("Usuarios encontrados", usuarios));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> activarUsuario(@PathVariable Long id) {
        try {
            usuarioService.activarUsuario(id);
            return ResponseEntity.ok(ApiResponse.success("Usuario activado", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> desactivarUsuario(@PathVariable Long id) {
        try {
            usuarioService.desactivarUsuario(id);
            return ResponseEntity.ok(ApiResponse.success("Usuario desactivado", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/bloquear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> bloquearUsuario(
            @PathVariable Long id,
            @RequestParam String motivo) {
        try {
            usuarioService.bloquearUsuario(id, motivo);
            return ResponseEntity.ok(ApiResponse.success("Usuario bloqueado", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/desbloquear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> desbloquearUsuario(@PathVariable Long id) {
        try {
            usuarioService.desbloquearUsuario(id);
            return ResponseEntity.ok(ApiResponse.success("Usuario desbloqueado", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> resetearPassword(@PathVariable Long id) {
        try {
            usuarioService.resetearPassword(id);
            return ResponseEntity.ok(ApiResponse.success("Contraseña reseteada", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/asignar-rol")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> asignarRol(
            @PathVariable Long id,
            @RequestParam Long rolId) {
        try {
            usuarioService.asignarRol(id, rolId);
            return ResponseEntity.ok(ApiResponse.success("Rol asignado", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/perfil")
    public ResponseEntity<ApiResponse> obtenerPerfil() {
        try {
            // Obtener ID del usuario autenticado del contexto de seguridad
            // Por ahora retornamos un usuario de prueba
            UsuarioResponse usuario = usuarioService.obtenerUsuarioPorId(1L);
            return ResponseEntity.ok(ApiResponse.success("Perfil obtenido", usuario));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}