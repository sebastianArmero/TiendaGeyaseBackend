package com.tienda.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CajaRequest {

    @NotBlank(message = "El código es obligatorio")
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String descripcion;

    @NotBlank(message = "La sucursal es obligatoria")
    private Long sucursalId;

    private BigDecimal saldoInicial;
    private String estado;
    private Long usuarioAsignadoId;
}