package com.tienda.repository;

import com.tienda.model.TokenRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {

    // ✅ CORREGIDO: Consulta por token
    @Query("SELECT t FROM TokenRecuperacion t WHERE t.token = ?1")
    Optional<TokenRecuperacion> findByToken(String token);

    List<TokenRecuperacion> findByUsuarioId(Long usuarioId);

    // ✅ Tokens válidos no utilizados
    @Query("SELECT t FROM TokenRecuperacion t WHERE t.usuario.id = ?1 AND t.utilizado = false AND t.fechaExpiracion > ?2")
    List<TokenRecuperacion> findTokensValidosUsuario(Long usuarioId, LocalDateTime ahora);

    // ✅ Tokens expirados
    @Query("SELECT t FROM TokenRecuperacion t WHERE t.fechaExpiracion <= ?1 AND t.utilizado = false")
    List<TokenRecuperacion> findTokensExpirados(LocalDateTime ahora);

    // ✅ Limpiar tokens expirados
    @Query("DELETE FROM TokenRecuperacion t WHERE t.fechaExpiracion <= ?1")
    void eliminarTokensExpirados(LocalDateTime fechaLimite);
}