package com.tienda.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarritoRequest {

    @NotNull(message = "El producto es requerido")
    private Long productoId;

    @NotNull(message = "La cantidad es requerida")
    private BigDecimal cantidad;

    private BigDecimal precioUnitario;
    private BigDecimal descuentoPorcentaje;
    private BigDecimal ivaPorcentaje;
}