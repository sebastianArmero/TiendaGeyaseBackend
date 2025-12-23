package com.tienda.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private Long categoriaPadreId;
    private String categoriaPadreNombre;
    private Integer nivel;
    private String icono;
    private Integer orden;
    private String estado;
    private Integer totalProductos;
    private List<CategoriaResponse> subcategorias;
}