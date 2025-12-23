package com.tienda.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VentaRequest {

    @NotNull(message = "El ID del vendedor es obligatorio")
    private Long vendedorId;

    @NotNull(message = "El ID de la caja es obligatorio")
    private Long cajaId;

    private Long clienteId;

    @NotNull(message = "La forma de pago es obligatoria")
    private String formaPago;

    private BigDecimal efectivoRecibido;

    @Valid
    @NotEmpty(message = "Debe incluir al menos un producto en la venta")
    private List<DetalleVentaRequest> detalles;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetalleVentaRequest {

        @NotNull(message = "El ID del producto es obligatorio")
        private Long productoId;

        @NotNull(message = "La cantidad es obligatoria")
        private BigDecimal cantidad;

        @NotNull(message = "El precio unitario es obligatorio")
        private BigDecimal precioUnitario;

        private BigDecimal descuentoPorcentaje = BigDecimal.ZERO;
        private BigDecimal ivaPorcentaje = BigDecimal.ZERO;
    }
}