package com.tienda.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CierreCajaRequest {

    @NotNull(message = "La caja es obligatoria")
    private Long cajaId;

    @NotNull(message = "La fecha de cierre es obligatoria")
    private LocalDate fechaCierre;

    private LocalTime horaApertura;
    private LocalTime horaCierre;
    private BigDecimal saldoFinalReal;
    private BigDecimal efectivo;
    private BigDecimal tarjetas;
    private BigDecimal transferencias;
    private BigDecimal otrosMedios;
    private String observaciones;
}