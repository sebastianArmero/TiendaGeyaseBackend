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
public class DetalleVentaResponse {

    private Long id;
    private Long productoId;
    private String codigoProducto;
    private String nombreProducto;
    private String unidadMedida;
    private BigDecimal cantidad;
    private BigDecimal cantidadDevuelta;
    private BigDecimal precioUnitario;
    private BigDecimal costoUnitario;
    private BigDecimal descuentoUnitario;
    private BigDecimal descuentoPorcentaje;
    private BigDecimal ivaPorcentaje;
    private BigDecimal ivaValor;
    private BigDecimal subtotal;
    private BigDecimal total;
}