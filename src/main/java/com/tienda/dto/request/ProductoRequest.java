package com.tienda.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoRequest {

    @NotBlank(message = "El código es obligatorio")
    private String codigo;

    private String codigoBarras;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String descripcion;
    private Long categoriaId;
    private String subcategoria;
    private String marca;
    private String modelo;

    // Stock
    private BigDecimal stockActual;
    private BigDecimal stockMinimo;
    private BigDecimal stockMaximo;

    // Precios
    @NotNull(message = "El precio de venta es obligatorio")
    private BigDecimal precioVenta;

    private BigDecimal costoPromedio;
    private BigDecimal precioVenta2;
    private BigDecimal precioVenta3;

    // Unidades
    private String unidadMedida;
    private String tipoProducto;
    private Boolean permiteDecimal;

    // Proveedor
    private Long proveedorId;
    private String ubicacion;

    // Estados
    private String estado;
    private String alertaStock;
}