package com.tienda.service.impl;

import com.tienda.dto.request.LoginRequest;
import com.tienda.dto.request.ResetPasswordRequest;
import com.tienda.dto.response.LoginResponse;
import com.tienda.model.*;
import com.tienda.repository.*;
import com.tienda.security.JwtUtil;
import com.tienda.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;
    private final SesionUsuarioRepository sesionUsuarioRepository;
    private final LogAutenticacionRepository logAutenticacionRepository;
    private final TokenRecuperacionRepository tokenRecuperacionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest loginRequest) {
        try {
            // Autenticar con Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Obtener usuario
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            Usuario usuario = usuarioRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Resetear intentos de login si es exitoso
            usuario.resetearIntentosLogin();
            usuario.setFechaUltimoLogin(LocalDateTime.now());
            usuarioRepository.save(usuario);

            // Generar tokens
            String token = jwtUtil.generateToken(authentication);
            String refreshToken = jwtUtil.generateRefreshToken(loginRequest.getUsername());

            // Registrar sesión
            SesionUsuario sesion = SesionUsuario.builder()
                    .usuario(usuario)
                    .token(token)
                    .refreshToken(refreshToken)
                    .ipAddress("127.0.0.1") // En producción obtendrías de la request
                    .fechaExpiracion(LocalDateTime.now().plusHours(24))
                    .estado(SesionUsuario.EstadoSesion.ACTIVA)
                    .build();

            sesionUsuarioRepository.save(sesion);

            // Registrar log de autenticación
            LogAutenticacion logAuth = LogAutenticacion.builder()
                    .usuario(usuario)
                    .username(usuario.getUsername())
                    .tipoEvento(LogAutenticacion.TipoEvento.LOGIN_EXITOSO.toString())
                    .detalles("Login exitoso desde IP: 127.0.0.1")
                    .build();

            logAutenticacionRepository.save(logAuth);

            // Construir respuesta
            return LoginResponse.builder()
                    .token(token)
                    .refreshToken(refreshToken)
                    .tipoToken("Bearer")
                    .id(usuario.getId())
                    .username(usuario.getUsername())
                    .email(usuario.getEmail())
                    .nombreCompleto(usuario.getNombreCompleto())
                    .rol(usuario.getRol() != null ? usuario.getRol().getNombre() : null)
                    .requiereCambioPassword(usuario.getRequiereCambioPassword())
                    .expiresIn(jwtUtil.getExpirationTime())
                    .build();

        } catch (BadCredentialsException e) {
            // Incrementar intentos fallidos
            Usuario usuario = usuarioRepository.findByUsername(loginRequest.getUsername())
                    .orElse(null);

            if (usuario != null) {
                usuario.incrementarIntentosLogin();
                usuarioRepository.save(usuario);

                // Registrar log de fallo
                LogAutenticacion logAuth = LogAutenticacion.builder()
                        .usuario(usuario)
                        .username(usuario.getUsername())
                        .tipoEvento(LogAutenticacion.TipoEvento.LOGIN_FALLIDO.toString())
                        .detalles("Login fallido - Intentos: " + usuario.getIntentosLogin())
                        .build();

                logAutenticacionRepository.save(logAuth);
            }

            throw new RuntimeException("Credenciales inválidas");
        }
    }

    @Override
    @Transactional
    public void logout(String token) {
        try {
            String username = jwtUtil.getUsernameFromToken(token);
            Usuario usuario = usuarioRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Buscar y marcar sesión como cerrada
            SesionUsuario sesion = sesionUsuarioRepository.findByToken(token)
                    .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

            sesion.setEstado(SesionUsuario.EstadoSesion.CERRADA);
            sesion.setMotivoRevocacion("Logout manual");
            sesionUsuarioRepository.save(sesion);

            // Registrar log
            LogAutenticacion logAuth = LogAutenticacion.builder()
                    .usuario(usuario)
                    .username(username)
                    .tipoEvento(LogAutenticacion.TipoEvento.LOGOUT.toString())
                    .detalles("Logout manual del sistema")
                    .build();

            logAutenticacionRepository.save(logAuth);

        } catch (Exception e) {
            log.error("Error durante logout: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public LoginResponse refreshToken(String refreshToken) {
        try {
            String username = jwtUtil.getUsernameFromToken(refreshToken);

            // Verificar que el refresh token sea válido
            SesionUsuario sesion = sesionUsuarioRepository.findByRefreshToken(refreshToken)
                    .orElseThrow(() -> new RuntimeException("Refresh token inválido"));

            if (!sesion.isActiva()) {
                throw new RuntimeException("Sesión inactiva");
            }

            // Generar nuevo token
            String newToken = jwtUtil.generateTokenFromUsername(username);

            // Actualizar sesión
            sesion.setToken(newToken);
            sesion.setFechaExpiracion(LocalDateTime.now().plusHours(24));
            sesion.actualizarActividad();
            sesionUsuarioRepository.save(sesion);

            // Obtener usuario para respuesta
            Usuario usuario = usuarioRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            return LoginResponse.builder()
                    .token(newToken)
                    .refreshToken(refreshToken)
                    .tipoToken("Bearer")
                    .id(usuario.getId())
                    .username(usuario.getUsername())
                    .email(usuario.getEmail())
                    .nombreCompleto(usuario.getNombreCompleto())
                    .rol(usuario.getRol() != null ? usuario.getRol().getNombre() : null)
                    .expiresIn(jwtUtil.getExpirationTime())
                    .build();

        } catch (Exception e) {
            log.error("Error refrescando token: {}", e.getMessage());
            throw new RuntimeException("No se pudo refrescar el token");
        }
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ese email"));

        // Generar token de recuperación
        String token = UUID.randomUUID().toString();

        TokenRecuperacion tokenRecuperacion = TokenRecuperacion.builder()
                .usuario(usuario)
                .token(token)
                .tipo("RECUPERACION")
                .fechaExpiracion(LocalDateTime.now().plusHours(24))
                .utilizado(false)
                .build();

        tokenRecuperacionRepository.save(tokenRecuperacion);

        // Aquí deberías enviar un email con el token
        // Por ahora solo lo logueamos
        log.info("Token de recuperación para {}: {}", email, token);

        // Registrar log
        LogAutenticacion logAuth = LogAutenticacion.builder()
                .usuario(usuario)
                .username(usuario.getUsername())
                .tipoEvento(LogAutenticacion.TipoEvento.CAMBIO_PASSWORD.toString())
                .detalles("Solicitud de recuperación de contraseña")
                .build();

        logAutenticacionRepository.save(logAuth);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        TokenRecuperacion tokenRecuperacion = tokenRecuperacionRepository.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Token inválido"));

        if (!tokenRecuperacion.isValido()) {
            throw new RuntimeException("Token expirado o ya utilizado");
        }

        if (!tokenRecuperacion.getUsuario().getEmail().equals(request.getEmail())) {
            throw new RuntimeException("Email no coincide con el token");
        }

        // Actualizar contraseña
        Usuario usuario = tokenRecuperacion.getUsuario();
        usuario.setPasswordHash(passwordEncoder.encode(request.getNuevaPassword()));
        usuario.setFechaUltimoCambioPassword(LocalDateTime.now());
        usuario.setRequiereCambioPassword(false);
        usuarioRepository.save(usuario);

        // Marcar token como utilizado
        tokenRecuperacion.marcarComoUtilizado();
        tokenRecuperacionRepository.save(tokenRecuperacion);

        // Registrar log
        LogAutenticacion logAuth = LogAutenticacion.builder()
                .usuario(usuario)
                .username(usuario.getUsername())
                .tipoEvento(LogAutenticacion.TipoEvento.CAMBIO_PASSWORD.toString())
                .detalles("Contraseña restablecida exitosamente")
                .build();

        logAutenticacionRepository.save(logAuth);
    }

    @Override
    @Transactional
    public void cambiarPassword(Long usuarioId, String oldPassword, String newPassword) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Verificar contraseña actual
        if (!passwordEncoder.matches(oldPassword, usuario.getPasswordHash())) {
            throw new RuntimeException("Contraseña actual incorrecta");
        }

        // Validar que no sea la misma contraseña
        if (passwordEncoder.matches(newPassword, usuario.getPasswordHash())) {
            throw new RuntimeException("La nueva contraseña no puede ser igual a la actual");
        }

        // Actualizar contraseña
        usuario.setPasswordHash(passwordEncoder.encode(newPassword));
        usuario.setFechaUltimoCambioPassword(LocalDateTime.now());
        usuario.setRequiereCambioPassword(false);
        usuarioRepository.save(usuario);

        // Registrar log
        LogAutenticacion logAuth = LogAutenticacion.builder()
                .usuario(usuario)
                .username(usuario.getUsername())
                .tipoEvento(LogAutenticacion.TipoEvento.CAMBIO_PASSWORD.toString())
                .detalles("Cambio de contraseña desde perfil")
                .build();

        logAutenticacionRepository.save(logAuth);
    }
}