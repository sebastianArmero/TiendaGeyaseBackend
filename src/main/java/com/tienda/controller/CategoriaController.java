package com.tienda.controller;

import com.tienda.dto.request.CategoriaRequest;
import com.tienda.dto.response.CategoriaResponse;
import com.tienda.service.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
@Tag(name = "Categorías", description = "Controlador para gestión de categorías de productos")
public class CategoriaController {

    private final CategoriaService categoriaService;

    @Operation(summary = "Crear nueva categoría")
    @PostMapping
    public ResponseEntity<CategoriaResponse> crearCategoria(@Valid @RequestBody CategoriaRequest request) {
        CategoriaResponse response = categoriaService.crearCategoria(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Obtener categoría por ID")
    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponse> obtenerCategoriaPorId(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaService.obtenerCategoriaPorId(id));
    }

    @Operation(summary = "Actualizar categoría")
    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponse> actualizarCategoria(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.ok(categoriaService.actualizarCategoria(id, request));
    }

    @Operation(summary = "Eliminar categoría")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCategoria(@PathVariable Long id) {
        categoriaService.eliminarCategoria(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Obtener todas las categorías")
    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> obtenerTodasCategorias() {
        return ResponseEntity.ok(categoriaService.obtenerTodasCategorias());
    }

    @Operation(summary = "Obtener categorías padre (nivel 1)")
    @GetMapping("/padres")
    public ResponseEntity<List<CategoriaResponse>> obtenerCategoriasPadre() {
        return ResponseEntity.ok(categoriaService.obtenerCategoriasPadre());
    }

    @Operation(summary = "Obtener subcategorías de una categoría padre")
    @GetMapping("/{categoriaPadreId}/subcategorias")
    public ResponseEntity<List<CategoriaResponse>> obtenerSubcategorias(
            @PathVariable Long categoriaPadreId) {
        return ResponseEntity.ok(categoriaService.obtenerSubcategorias(categoriaPadreId));
    }

    @Operation(summary = "Buscar categorías por nombre")
    @GetMapping("/buscar")
    public ResponseEntity<List<CategoriaResponse>> buscarCategoriasPorNombre(
            @RequestParam String nombre) {
        return ResponseEntity.ok(categoriaService.buscarCategoriasPorNombre(nombre));
    }

    @Operation(summary = "Obtener categorías por estado")
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<CategoriaResponse>> obtenerCategoriasPorEstado(
            @PathVariable String estado) {
        return ResponseEntity.ok(categoriaService.obtenerCategoriasPorEstado(estado));
    }

    @Operation(summary = "Obtener árbol completo de categorías")
    @GetMapping("/arbol")
    public ResponseEntity<Map<String, Object>> obtenerArbolCategorias() {
        return ResponseEntity.ok(categoriaService.obtenerArbolCategorias());
    }

    @Operation(summary = "Obtener categorías con información de productos")
    @GetMapping("/con-productos")
    public ResponseEntity<List<CategoriaResponse>> obtenerCategoriasConProductos() {
        return ResponseEntity.ok(categoriaService.obtenerCategoriasConProductos());
    }

    @Operation(summary = "Verificar si existe categoría por nombre")
    @GetMapping("/existe-nombre")
    public ResponseEntity<Boolean> existeCategoriaPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(categoriaService.existeCategoriaPorNombre(nombre));
    }
}