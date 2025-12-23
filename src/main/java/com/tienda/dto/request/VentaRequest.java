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

    @NotNull(message = "El cliente es requerido")
    private Long clienteId;

    private String clienteNombre;
    private String clienteDocumento;
    private String clienteDireccion;
    private String clienteTelefono;
    private String clienteEmail;

    @NotEmpty(message = "Debe haber al menos un producto en la venta")
    @Valid
    private List<ItemVentaRequest> items;

    private BigDecimal descuentoTotal;
    private BigDecimal ivaTotal;
    private BigDecimal otrosImpuestos;

    @NotNull(message = "La forma de pago es requerida")
    private String formaPago;

    private BigDecimal efectivoRecibido;
    private Long cajaId;
    private String observaciones;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemVentaRequest {
        @NotNull(message = "El producto es requerido")
        private Long productoId;

        @NotNull(message = "La cantidad es requerida")
        private BigDecimal cantidad;

        private BigDecimal precioUnitario;
        private BigDecimal descuentoPorcentaje;
        private BigDecimal descuentoUnitario;
        private BigDecimal ivaPorcentaje;
    }
}