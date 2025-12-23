package com.tienda.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarritoResponse {

    private List<ItemCarritoResponse> items;
    private BigDecimal subtotal;
    private BigDecimal descuentoTotal;
    private BigDecimal ivaTotal;
    private BigDecimal total;
    private Integer totalItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemCarritoResponse {
        private Long productoId;
        private String codigo;
        private String nombre;
        private BigDecimal cantidad;
        private BigDecimal precioUnitario;
        private BigDecimal descuentoUnitario;
        private BigDecimal subtotal;
        private Boolean stockSuficiente;
        private BigDecimal stockDisponible;
    }
}