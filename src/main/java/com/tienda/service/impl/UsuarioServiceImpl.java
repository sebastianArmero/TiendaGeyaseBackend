package com.tienda.service.impl;

import com.tienda.dto.request.CrearUsuarioRequest;
import com.tienda.dto.request.ActualizarUsuarioRequest;
import com.tienda.dto.response.PaginacionResponse;
import com.tienda.dto.response.UsuarioResponse;
import com.tienda.exception.ResourceNotFoundException;
import com.tienda.exception.ValidacionException;
import com.tienda.model.*;
import com.tienda.repository.*;
import com.tienda.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final SucursalRepository sucursalRepository;
    private final CajaRepository cajaRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UsuarioResponse crearUsuario(CrearUsuarioRequest request) {
        // Validar unicidad
        if (existeUsuarioPorUsername(request.getUsername())) {
            throw new ValidacionException("El nombre de usuario ya está en uso");
        }
        if (existeUsuarioPorEmail(request.getEmail())) {
            throw new ValidacionException("El email ya está registrado");
        }

        // Buscar rol
        Rol rol = null;
        if (request.getRolId() != null) {
            rol = rolRepository.findById(request.getRolId())
                    .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
        }

        // Buscar sucursal
        Sucursal sucursal = null;
        if (request.getSucursalId() != null) {
            sucursal = sucursalRepository.findById(request.getSucursalId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
        }

        // Crear usuario
        Usuario usuario = Usuario.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nombreCompleto(request.getNombreCompleto())
                .documentoIdentidad(request.getDocumentoIdentidad())
                .telefono(request.getTelefono())
                .direccion(request.getDireccion())
                .rol(rol)
                .sucursal(sucursal)
                .esSuperAdmin(request.getEsSuperAdmin() != null ? request.getEsSuperAdmin() : false)
                .requiereCambioPassword(true) // Forzar cambio en primer login
                .estado(Usuario.EstadoUsuario.ACTIVO)
                .build();

        usuario = usuarioRepository.save(usuario);
        log.info("Usuario creado: {}", usuario.getUsername());

        return convertirAResponse(usuario);
    }

    @Override
    @Transactional
    public UsuarioResponse actualizarUsuario(Long id, ActualizarUsuarioRequest request) {
        Usuario usuario = obtenerEntidadUsuario(id);

        // Validar email único si se cambia
        if (request.getEmail() != null && !request.getEmail().equals(usuario.getEmail())) {
            if (existeUsuarioPorEmail(request.getEmail())) {
                throw new ValidacionException("El email ya está registrado");
            }
            usuario.setEmail(request.getEmail());
        }

        // Actualizar campos
        if (request.getNombreCompleto() != null) {
            usuario.setNombreCompleto(request.getNombreCompleto());
        }
        if (request.getDocumentoIdentidad() != null) {
            usuario.setDocumentoIdentidad(request.getDocumentoIdentidad());
        }
        if (request.getTelefono() != null) {
            usuario.setTelefono(request.getTelefono());
        }
        if (request.getDireccion() != null) {
            usuario.setDireccion(request.getDireccion());
        }

        // Actualizar rol
        if (request.getRolId() != null) {
            Rol rol = rolRepository.findById(request.getRolId())
                    .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
            usuario.setRol(rol);
        }

        // Actualizar sucursal
        if (request.getSucursalId() != null) {
            Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
            usuario.setSucursal(sucursal);
        }

        // Actualizar caja
        if (request.getCajaId() != null) {
            Caja caja = cajaRepository.findById(request.getCajaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Caja no encontrada"));
            usuario.setCaja(caja);
        }

        if (request.getTemaPreferido() != null) {
            usuario.setTemaPreferido(request.getTemaPreferido());
        }
        if (request.getIdiomaPreferido() != null) {
            usuario.setIdiomaPreferido(request.getIdiomaPreferido());
        }
        if (request.getEsSuperAdmin() != null) {
            usuario.setEsSuperAdmin(request.getEsSuperAdmin());
        }

        usuario = usuarioRepository.save(usuario);

        return convertirAResponse(usuario);
    }

    @Override
    @Transactional
    public void eliminarUsuario(Long id) {
        Usuario usuario = obtenerEntidadUsuario(id);

        // No permitir eliminar super admin
        if (usuario.getEsSuperAdmin()) {
            throw new ValidacionException("No se puede eliminar un super administrador");
        }

        // Marcar como eliminado (soft delete)
        usuario.setEstado(Usuario.EstadoUsuario.ELIMINADO);
        usuarioRepository.save(usuario);

        log.info("Usuario marcado como eliminado: {}", usuario.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerUsuarioPorId(Long id) {
        Usuario usuario = obtenerEntidadUsuario(id);
        return convertirAResponse(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> obtenerTodosUsuarios() {
        return usuarioRepository.findAll().stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginacionResponse<UsuarioResponse> obtenerUsuariosPaginados(Pageable pageable) {
        Page<Usuario> usuariosPage = usuarioRepository.findAll(pageable);

        List<UsuarioResponse> usuariosResponse = usuariosPage.getContent().stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());

        return PaginacionResponse.<UsuarioResponse>builder()
                .content(usuariosResponse)
                .pageNumber(usuariosPage.getNumber())
                .pageSize(usuariosPage.getSize())
                .totalElements(usuariosPage.getTotalElements())
                .totalPages(usuariosPage.getTotalPages())
                .last(usuariosPage.isLast())
                .first(usuariosPage.isFirst())
                .empty(usuariosPage.isEmpty())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerUsuarioPorUsername(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return convertirAResponse(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerUsuarioPorEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return convertirAResponse(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> buscarUsuariosPorNombre(String nombre) {
        Page<Usuario> usuariosPage = usuarioRepository.buscarUsuarios(
                null, nombre, null, Pageable.unpaged());
        return usuariosPage.getContent().stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void activarUsuario(Long id) {
        Usuario usuario = obtenerEntidadUsuario(id);
        usuario.setEstado(Usuario.EstadoUsuario.ACTIVO);
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void desactivarUsuario(Long id) {
        Usuario usuario = obtenerEntidadUsuario(id);
        if (usuario.getEsSuperAdmin()) {
            throw new ValidacionException("No se puede desactivar un super administrador");
        }
        usuario.setEstado(Usuario.EstadoUsuario.INACTIVO);
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void bloquearUsuario(Long id, String motivo) {
        Usuario usuario = obtenerEntidadUsuario(id);
        if (usuario.getEsSuperAdmin()) {
            throw new ValidacionException("No se puede bloquear un super administrador");
        }
        usuario.setEstado(Usuario.EstadoUsuario.BLOQUEADO);
        usuario.setFechaBloqueo(LocalDateTime.now());
        usuarioRepository.save(usuario);

        log.info("Usuario bloqueado: {} - Motivo: {}", usuario.getUsername(), motivo);
    }

    @Override
    @Transactional
    public void desbloquearUsuario(Long id) {
        Usuario usuario = obtenerEntidadUsuario(id);
        usuario.setEstado(Usuario.EstadoUsuario.ACTIVO);
        usuario.setFechaBloqueo(null);
        usuario.setIntentosLogin(0);
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void asignarRol(Long usuarioId, Long rolId) {
        Usuario usuario = obtenerEntidadUsuario(usuarioId);
        Rol rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));

        usuario.setRol(rol);
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void quitarRol(Long usuarioId) {
        Usuario usuario = obtenerEntidadUsuario(usuarioId);
        usuario.setRol(null);
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void resetearPassword(Long id) {
        Usuario usuario = obtenerEntidadUsuario(id);
        // Generar password temporal
        String tempPassword = "Temp123";
        usuario.setPasswordHash(passwordEncoder.encode(tempPassword));
        usuario.setRequiereCambioPassword(true);
        usuario.setFechaUltimoCambioPassword(LocalDateTime.now());
        usuarioRepository.save(usuario);

        // Aquí deberías enviar un email con la contraseña temporal
        log.info("Password reseteado para usuario: {}. Nueva password: {}",
                usuario.getUsername(), tempPassword);
    }

    @Override
    @Transactional
    public void cambiarPasswordUsuario(Long id, String nuevaPassword) {
        Usuario usuario = obtenerEntidadUsuario(id);
        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuario.setRequiereCambioPassword(false);
        usuario.setFechaUltimoCambioPassword(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void forzarCambioPassword(Long id) {
        Usuario usuario = obtenerEntidadUsuario(id);
        usuario.setRequiereCambioPassword(true);
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeUsuarioPorUsername(String username) {
        return usuarioRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeUsuarioPorEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario obtenerEntidadUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario obtenerEntidadUsuarioPorUsername(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));
    }

    // Método privado para convertir entidad a DTO
    private UsuarioResponse convertirAResponse(Usuario usuario) {
        return UsuarioResponse.builder()
                .id(usuario.getId())
                .username(usuario.getUsername())
                .email(usuario.getEmail())
                .nombreCompleto(usuario.getNombreCompleto())
                .documentoIdentidad(usuario.getDocumentoIdentidad())
                .telefono(usuario.getTelefono())
                .direccion(usuario.getDireccion())
                .estado(usuario.getEstado().name())
                .intentosLogin(usuario.getIntentosLogin())
                .fechaUltimoLogin(usuario.getFechaUltimoLogin())
                .fechaUltimoCambioPassword(usuario.getFechaUltimoCambioPassword())
                .rolNombre(usuario.getRol() != null ? usuario.getRol().getNombre() : null)
                .rolId(usuario.getRol() != null ? usuario.getRol().getId() : null)
                .esSuperAdmin(usuario.getEsSuperAdmin())
                .requiereCambioPassword(usuario.getRequiereCambioPassword())
                .sucursalNombre(usuario.getSucursal() != null ? usuario.getSucursal().getNombre() : null)
                .sucursalId(usuario.getSucursal() != null ? usuario.getSucursal().getId() : null)
                .cajaNombre(usuario.getCaja() != null ? usuario.getCaja().getNombre() : null)
                .cajaId(usuario.getCaja() != null ? usuario.getCaja().getId() : null)
                .temaPreferido(usuario.getTemaPreferido())
                .idiomaPreferido(usuario.getIdiomaPreferido())
                .creadoEn(usuario.getCreadoEn())
                .actualizadoEn(usuario.getActualizadoEn())
                .build();
    }
}