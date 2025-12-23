package com.tienda.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CierreCajaResponse {

    private Long id;
    private String cajaNombre;
    private Long cajaId;
    private LocalDate fechaCierre;
    private LocalTime horaApertura;
    private LocalTime horaCierre;
    private String usuarioNombre;
    private Long usuarioId;

    // SALDOS
    private BigDecimal saldoInicial;
    private BigDecimal saldoFinalTeorico;
    private BigDecimal saldoFinalReal;
    private BigDecimal diferencia;

    // TOTALES
    private BigDecimal totalVentas;
    private BigDecimal totalCompras;
    private BigDecimal totalGastos;
    private BigDecimal totalIngresos;
    private BigDecimal totalEgresos;

    // FORMAS DE PAGO
    private BigDecimal efectivo;
    private BigDecimal tarjetas;
    private BigDecimal transferencias;
    private BigDecimal otrosMedios;

    // ESTADOS
    private String estado;
    private String observaciones;
    private String conciliadoPor;
    private LocalDateTime fechaConciliacion;

    // DETALLES
    private Integer numeroVentas;
    private Integer numeroCompras;
    private Integer numeroGastos;
    private Map<String, BigDecimal> ventasPorFormaPago;
    private Map<String, Object> resumen;

    // AUDITORÍA
    private LocalDateTime creadoEn;
}