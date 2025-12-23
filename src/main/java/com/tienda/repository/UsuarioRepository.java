package com.tienda.repository;

import com.tienda.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);
    Optional<Usuario> findByEmail(String email);
    List<Usuario> findByEstado(Usuario.EstadoUsuario estado);
    List<Usuario> findByRolId(Long rolId);
    List<Usuario> findBySucursalId(Long sucursalId);

    @Query("SELECT u FROM Usuario u WHERE u.fechaUltimoLogin < :fecha")
    List<Usuario> findUsuariosConUltimoLoginAnterior(@Param("fecha") LocalDateTime fecha);

    // ✅ CORREGIDO: Consulta con @Param
    @Query("SELECT u FROM Usuario u WHERE " +
            "(:username IS NULL OR u.username LIKE %:username%) AND " +
            "(:nombreCompleto IS NULL OR u.nombreCompleto LIKE %:nombreCompleto%) AND " +
            "(:email IS NULL OR u.email LIKE %:email%)")
    Page<Usuario> buscarUsuarios(
            @Param("username") String username,
            @Param("nombreCompleto") String nombreCompleto,
            @Param("email") String email,
            Pageable pageable);

    @Query("SELECT u FROM Usuario u JOIN u.rol r WHERE r.codigo = :codigo")
    List<Usuario> findByRolCodigo(@Param("codigo") String rolCodigo);

    @Query("SELECT u FROM Usuario u WHERE u.intentosLogin >= :intentos")
    List<Usuario> findUsuariosConIntentosExcedidos(@Param("intentos") int intentos);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}