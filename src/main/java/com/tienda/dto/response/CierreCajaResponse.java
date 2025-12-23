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

    // Información de caja
    private Long cajaId;
    private String cajaNombre;
    private String cajaCodigo;

    // Fechas y horarios
    private LocalDate fechaCierre;
    private LocalTime horaApertura;
    private LocalTime horaCierre;

    // Usuario
    private Long usuarioId;
    private String usuarioNombre;

    // Saldos
    private BigDecimal saldoInicial;
    private BigDecimal saldoFinalTeorico;
    private BigDecimal saldoFinalReal;
    private BigDecimal diferencia;

    // Totales por tipo de operación
    private BigDecimal totalVentas;
    private BigDecimal totalCompras;
    private BigDecimal totalGastos;
    private BigDecimal totalIngresos;
    private BigDecimal totalEgresos;

    // Totales por forma de pago
    private BigDecimal efectivo;
    private BigDecimal tarjetas;
    private BigDecimal transferencias;
    private BigDecimal otrosMedios;

    // Estado y observaciones
    private String estado;
    private String observaciones;

    // Conciliación
    private Long conciliadoPorId;
    private String conciliadoPorNombre;
    private LocalDateTime fechaConciliacion;

    // Auditoría
    private LocalDateTime creadoEn;
}