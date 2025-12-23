package com.tienda.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoResponse {

    private Long id;
    private String codigo;
    private String codigoBarras;
    private String nombre;
    private String descripcion;

    // Categoría
    private String categoriaNombre;
    private Long categoriaId;
    private String subcategoria;

    // Stock
    private BigDecimal stockActual;
    private BigDecimal stockDisponible;
    private BigDecimal stockReservado;
    private BigDecimal stockMinimo;
    private BigDecimal stockMaximo;

    // Precios
    private BigDecimal costoPromedio;
    private BigDecimal costoUltimo;
    private BigDecimal precioVenta;
    private BigDecimal precioVenta2;
    private BigDecimal precioVenta3;
    private BigDecimal margenGanancia;

    // Unidades
    private String unidadMedida;
    private String tipoProducto;
    private Boolean permiteDecimal;

    // Proveedor
    private String proveedorNombre;
    private Long proveedorId;

    // Ubicación
    private String ubicacion;

    // Estados
    private String estado;
    private String alertaStock;
    private String estadoCalculado;

    // Valoración
    private BigDecimal valorCosto;
    private BigDecimal valorVenta;
    private BigDecimal utilidadPotencial;

    // Indicadores
    private Boolean necesitaReorden;
    private Integer diasSinMovimiento;

    // Auditoría
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
    private String usuarioCreacion;
    private String usuarioModificacion;
}