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
public class EstadoStockResponse {
    private Long id;
    private String codigo;
    private String nombre;
    private String descripcion;
    private String categoria;
    private String subcategoria;

    // STOCK
    private BigDecimal stockActual;
    private BigDecimal stockDisponible;
    private BigDecimal stockReservado;
    private BigDecimal stockMinimo;
    private BigDecimal stockMaximo;

    // ESTADOS
    private String estadoProducto;
    private String alertaStock;
    private String estadoCalculado;

    // VALORACIÓN
    private BigDecimal costoPromedio;
    private BigDecimal precioVenta;
    private BigDecimal valorCosto;
    private BigDecimal valorVenta;
    private BigDecimal utilidadPotencial;
    private BigDecimal margenGanancia;

    // UNIDADES
    private String unidadMedida;
    private String tipoProducto;

    // UBICACIÓN
    private String ubicacion;
    private String proveedor;

    // INDICADORES
    private Boolean necesitaReorden;
    private Integer diasInventario;
    private BigDecimal rotacion;

    // AUDITORÍA
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
}