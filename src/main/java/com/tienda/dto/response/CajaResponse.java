package com.tienda.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CajaResponse {

    private Long id;
    private String codigo;
    private String nombre;
    private String descripcion;

    // Sucursal
    private Long sucursalId;
    private String sucursalNombre;

    // Saldos
    private BigDecimal saldoInicial;
    private BigDecimal saldoActual;

    // Estado
    private String estado;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;

    // Usuario asignado
    private Long usuarioAsignadoId;
    private String usuarioAsignadoNombre;

    // Estadísticas
    private Integer totalVentasHoy;
    private BigDecimal totalVentasHoyMonto;
    private Integer totalCierres;
    private LocalDateTime ultimoCierre;

    // Auditoría
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
}