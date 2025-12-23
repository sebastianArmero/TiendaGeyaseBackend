package com.tienda.repository;

import com.tienda.model.SesionUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SesionUsuarioRepository extends JpaRepository<SesionUsuario, Long> {

    List<SesionUsuario> findByUsuarioId(Long usuarioId);

    @Query("SELECT s FROM SesionUsuario s WHERE s.token = ?1")
    Optional<SesionUsuario> findByToken(String token);

    // MÉTODO NUEVO: Buscar por refresh token
    @Query("SELECT s FROM SesionUsuario s WHERE s.refreshToken = ?1")
    Optional<SesionUsuario> findByRefreshToken(String refreshToken);

    @Query("SELECT s FROM SesionUsuario s WHERE s.estado = 'ACTIVA' AND s.fechaExpiracion > ?1")
    List<SesionUsuario> findSesionesActivas(LocalDateTime ahora);

    // ✅ Sesiones expiradas
    @Query("SELECT s FROM SesionUsuario s WHERE s.fechaExpiracion <= ?1 AND s.estado = 'ACTIVA'")
    List<SesionUsuario> findSesionesExpiradas(LocalDateTime ahora);

    // ✅ Revocar todas las sesiones de un usuario
    @Query("UPDATE SesionUsuario s SET s.estado = 'REVOCADA', s.motivoRevocacion = ?2 WHERE s.usuario.id = ?1 AND s.estado = 'ACTIVA'")
    void revocarSesionesUsuario(Long usuarioId, String motivo);

    // ✅ Contar sesiones activas por usuario
    @Query("SELECT COUNT(s) FROM SesionUsuario s WHERE s.usuario.id = ?1 AND s.estado = 'ACTIVA'")
    Long contarSesionesActivasUsuario(Long usuarioId);
}