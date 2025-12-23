package com.tienda.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CierreCajaResponse {

    private Long id;
    private Long cajaId;
    private String cajaNombre;
    private LocalDate fechaCierre;
    private LocalTime horaApertura;
    private LocalTime horaCierre;

    private Long usuarioId;
    private String usuarioNombre;

    private BigDecimal saldoInicial;
    private BigDecimal saldoFinalTeorico;
    private BigDecimal saldoFinalReal;
    private BigDecimal diferencia;

    private BigDecimal totalVentas;
    private BigDecimal totalCompras;
    private BigDecimal totalGastos;
    private BigDecimal totalIngresos;
    private BigDecimal totalEgresos;

    private BigDecimal efectivo;
    private BigDecimal tarjetas;
    private BigDecimal transferencias;
    private BigDecimal otrosMedios;

    private String estado;
    private String observaciones;

    private LocalDateTime creadoEn;
    private Long conciliadoPor;
    private String conciliadoPorNombre;
    private LocalDateTime fechaConciliacion;
}