package com.tienda.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProveedorResponse {

    private Long id;
    private String codigo;
    private String nombre;
    private String contacto;
    private String telefono;
    private String email;
    private String ruc;
    private String direccion;
    private String observaciones;
    private String estado;
    private Integer diasCredito;
    private BigDecimal limiteCredito;
    private Integer totalProductos;
    private BigDecimal totalCompras;
    private BigDecimal saldoPendiente;
}