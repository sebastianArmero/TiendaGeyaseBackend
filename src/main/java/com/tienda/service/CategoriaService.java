package com.tienda.service;

import com.tienda.dto.request.CategoriaRequest;
import com.tienda.dto.response.CategoriaResponse;
import com.tienda.model.Categoria;

import java.util.List;
import java.util.Map;

public interface CategoriaService {

    // CRUD
    CategoriaResponse crearCategoria(CategoriaRequest request);
    CategoriaResponse actualizarCategoria(Long id, CategoriaRequest request);
    void eliminarCategoria(Long id);
    CategoriaResponse obtenerCategoriaPorId(Long id);
    List<CategoriaResponse> obtenerTodasCategorias();
    List<CategoriaResponse> obtenerCategoriasPadre();
    List<CategoriaResponse> obtenerSubcategorias(Long categoriaPadreId);

    // Búsquedas
    List<CategoriaResponse> buscarCategoriasPorNombre(String nombre);
    List<CategoriaResponse> obtenerCategoriasPorEstado(String estado);

    // Árbol de categorías
    Map<String, Object> obtenerArbolCategorias();
    List<CategoriaResponse> obtenerCategoriasConProductos();

    // Validaciones
    boolean existeCategoriaPorNombre(String nombre);

    // Métodos internos
    Categoria obtenerEntidadCategoria(Long id);
}