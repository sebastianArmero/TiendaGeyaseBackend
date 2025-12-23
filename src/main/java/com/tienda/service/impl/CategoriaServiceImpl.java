package com.tienda.service.impl;

import com.tienda.dto.request.CategoriaRequest;
import com.tienda.dto.response.CategoriaResponse;
import com.tienda.exception.ResourceNotFoundException;
import com.tienda.exception.ValidacionException;
import com.tienda.model.Categoria;
import com.tienda.model.Producto;
import com.tienda.repository.CategoriaRepository;
import com.tienda.repository.ProductoRepository;
import com.tienda.service.CategoriaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    @Override
    @Transactional
    public CategoriaResponse crearCategoria(CategoriaRequest request) {
        // Validar nombre único
        if (existeCategoriaPorNombre(request.getNombre())) {
            throw new ValidacionException("Ya existe una categoría con ese nombre");
        }

        // Buscar categoría padre si se especifica
        Categoria categoriaPadre = null;
        if (request.getCategoriaPadreId() != null) {
            categoriaPadre = categoriaRepository.findById(request.getCategoriaPadreId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoría padre no encontrada"));
        }

        // Calcular nivel
        Integer nivel = 1;
        if (categoriaPadre != null) {
            nivel = categoriaPadre.getNivel() + 1;
        }

        // Crear categoría
        Categoria categoria = Categoria.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .categoriaPadre(categoriaPadre)
                .nivel(nivel)
                .icono(request.getIcono())
                .orden(request.getOrden() != null ? request.getOrden() : 0)
                .estado(request.getEstado() != null ? request.getEstado() : "ACTIVO")
                .build();

        categoria = categoriaRepository.save(categoria);
        log.info("Categoría creada: {} - Nivel: {}", categoria.getNombre(), categoria.getNivel());

        return convertirAResponse(categoria);
    }

    @Override
    @Transactional
    public CategoriaResponse actualizarCategoria(Long id, CategoriaRequest request) {
        Categoria categoria = obtenerEntidadCategoria(id);

        // Validar nombre único si se cambia
        if (request.getNombre() != null && !request.getNombre().equals(categoria.getNombre())) {
            if (existeCategoriaPorNombre(request.getNombre())) {
                throw new ValidacionException("Ya existe una categoría con ese nombre");
            }
            categoria.setNombre(request.getNombre());
        }

        // Actualizar campos
        if (request.getDescripcion() != null) {
            categoria.setDescripcion(request.getDescripcion());
        }
        if (request.getIcono() != null) {
            categoria.setIcono(request.getIcono());
        }
        if (request.getOrden() != null) {
            categoria.setOrden(request.getOrden());
        }
        if (request.getEstado() != null) {
            categoria.setEstado(request.getEstado());
        }

        // Actualizar categoría padre (con validación de ciclos)
        if (request.getCategoriaPadreId() != null) {
            if (request.getCategoriaPadreId().equals(id)) {
                throw new ValidacionException("Una categoría no puede ser padre de sí misma");
            }

            Categoria nuevaCategoriaPadre = categoriaRepository.findById(request.getCategoriaPadreId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoría padre no encontrada"));

            // Validar que no se cree un ciclo
            if (esDescendiente(nuevaCategoriaPadre, id)) {
                throw new ValidacionException("No se puede asignar una categoría descendiente como padre");
            }

            categoria.setCategoriaPadre(nuevaCategoriaPadre);
            categoria.setNivel(nuevaCategoriaPadre.getNivel() + 1);

            // Actualizar niveles de subcategorías
            actualizarNivelesSubcategorias(categoria);
        }

        categoria = categoriaRepository.save(categoria);

        return convertirAResponse(categoria);
    }

    @Override
    @Transactional
    public void eliminarCategoria(Long id) {
        Categoria categoria = obtenerEntidadCategoria(id);

        // Verificar si tiene productos asociados
        List<Producto> productos = productoRepository.findByCategoriaId(id);
        if (!productos.isEmpty()) {
            throw new ValidacionException(
                    "No se puede eliminar la categoría porque tiene " + productos.size() + " productos asociados");
        }

        // Verificar si tiene subcategorías
        List<Categoria> subcategorias = categoriaRepository.findByCategoriaPadreId(id);
        if (!subcategorias.isEmpty()) {
            // Opción 1: Eliminar subcategorías también
            for (Categoria subcategoria : subcategorias) {
                eliminarCategoria(subcategoria.getId());
            }
            // Opción 2: O lanzar error
            // throw new ValidacionException("No se puede eliminar porque tiene subcategorías");
        }

        categoriaRepository.delete(categoria);
        log.info("Categoría eliminada: {}", categoria.getNombre());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaResponse obtenerCategoriaPorId(Long id) {
        Categoria categoria = obtenerEntidadCategoria(id);
        return convertirAResponseConSubcategorias(categoria);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponse> obtenerTodasCategorias() {
        return categoriaRepository.findAll().stream()
                .map(this::convertirAResponse)
                .sorted(Comparator.comparing(CategoriaResponse::getNivel)
                        .thenComparing(CategoriaResponse::getOrden))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponse> obtenerCategoriasPadre() {
        return categoriaRepository.findByCategoriaPadreIsNull().stream()
                .map(this::convertirAResponse)
                .sorted(Comparator.comparing(CategoriaResponse::getOrden))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponse> obtenerSubcategorias(Long categoriaPadreId) {
        return categoriaRepository.findByCategoriaPadreId(categoriaPadreId).stream()
                .map(this::convertirAResponse)
                .sorted(Comparator.comparing(CategoriaResponse::getOrden))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponse> buscarCategoriasPorNombre(String nombre) {
        return categoriaRepository.buscarPorNombre(nombre).stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponse> obtenerCategoriasPorEstado(String estado) {
        return categoriaRepository.findByEstado(estado).stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerArbolCategorias() {
        List<Categoria> categoriasPadre = categoriaRepository.findByCategoriaPadreIsNull();

        List<CategoriaResponse> arbol = categoriasPadre.stream()
                .map(this::convertirAResponseConSubcategoriasRecursivo)
                .sorted(Comparator.comparing(CategoriaResponse::getOrden))
                .collect(Collectors.toList());

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("categorias", arbol);
        resultado.put("totalCategorias", categoriaRepository.count());
        resultado.put("totalNiveles", calcularMaximoNivel());

        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponse> obtenerCategoriasConProductos() {
        List<Categoria> categorias = categoriaRepository.findAll();

        return categorias.stream()
                .map(categoria -> {
                    CategoriaResponse response = convertirAResponse(categoria);
                    // Contar productos en esta categoría
                    List<Producto> productos = productoRepository.findByCategoriaId(categoria.getId());
                    response.setTotalProductos(productos.size());
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeCategoriaPorNombre(String nombre) {
        List<Categoria> categorias = categoriaRepository.buscarPorNombre(nombre);
        return !categorias.isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public Categoria obtenerEntidadCategoria(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));
    }

    // Métodos privados auxiliares
    private CategoriaResponse convertirAResponse(Categoria categoria) {
        return CategoriaResponse.builder()
                .id(categoria.getId())
                .nombre(categoria.getNombre())
                .descripcion(categoria.getDescripcion())
                .categoriaPadreId(categoria.getCategoriaPadre() != null ?
                        categoria.getCategoriaPadre().getId() : null)
                .categoriaPadreNombre(categoria.getCategoriaPadre() != null ?
                        categoria.getCategoriaPadre().getNombre() : null)
                .nivel(categoria.getNivel())
                .icono(categoria.getIcono())
                .orden(categoria.getOrden())
                .estado(categoria.getEstado())
                .build();
    }

    private CategoriaResponse convertirAResponseConSubcategorias(Categoria categoria) {
        CategoriaResponse response = convertirAResponse(categoria);

        // Obtener subcategorías directas
        List<Categoria> subcategorias = categoriaRepository.findByCategoriaPadreId(categoria.getId());
        List<CategoriaResponse> subcategoriasResponse = subcategorias.stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());

        response.setSubcategorias(subcategoriasResponse);

        return response;
    }

    private CategoriaResponse convertirAResponseConSubcategoriasRecursivo(Categoria categoria) {
        CategoriaResponse response = convertirAResponse(categoria);

        // Obtener subcategorías recursivamente
        List<Categoria> subcategorias = categoriaRepository.findByCategoriaPadreId(categoria.getId());
        List<CategoriaResponse> subcategoriasResponse = subcategorias.stream()
                .map(this::convertirAResponseConSubcategoriasRecursivo)
                .sorted(Comparator.comparing(CategoriaResponse::getOrden))
                .collect(Collectors.toList());

        response.setSubcategorias(subcategoriasResponse);

        return response;
    }

    private boolean esDescendiente(Categoria categoria, Long idCategoria) {
        if (categoria.getId().equals(idCategoria)) {
            return true;
        }

        if (categoria.getCategoriaPadre() == null) {
            return false;
        }

        return esDescendiente(categoria.getCategoriaPadre(), idCategoria);
    }

    private void actualizarNivelesSubcategorias(Categoria categoria) {
        List<Categoria> subcategorias = categoriaRepository.findByCategoriaPadreId(categoria.getId());

        for (Categoria subcategoria : subcategorias) {
            subcategoria.setNivel(categoria.getNivel() + 1);
            categoriaRepository.save(subcategoria);

            // Actualizar recursivamente
            actualizarNivelesSubcategorias(subcategoria);
        }
    }

    private Integer calcularMaximoNivel() {
        return categoriaRepository.findAll().stream()
                .map(Categoria::getNivel)
                .max(Integer::compare)
                .orElse(0);
    }
}