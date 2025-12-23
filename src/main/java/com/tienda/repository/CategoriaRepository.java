package com.tienda.repository;

import com.tienda.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByCategoriaPadreIsNull();

    List<Categoria> findByCategoriaPadreId(Long parentId);

    List<Categoria> findByEstado(String estado);

    List<Categoria> findByNivel(Integer nivel);

    @Query("SELECT c FROM Categoria c WHERE c.nombre LIKE %?1%")
    List<Categoria> buscarPorNombre(String nombre);

    @Query("SELECT c FROM Categoria c WHERE c.nivel = ?1 AND c.estado = 'ACTIVO' ORDER BY c.orden")
    List<Categoria> findByNivelAndActivo(Integer nivel);

    // ✅ Contar productos por categoría
    @Query("SELECT c.nombre, COUNT(p) FROM Categoria c LEFT JOIN c.productos p GROUP BY c.id, c.nombre")
    List<Object[]> contarProductosPorCategoria();
}