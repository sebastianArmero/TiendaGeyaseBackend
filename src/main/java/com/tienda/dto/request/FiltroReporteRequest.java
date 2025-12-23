package com.tienda.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FiltroReporteRequest {

    private String tipoReporte;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;
    private Long sucursalId;
    private Long cajaId;
    private Long vendedorId;
    private Long categoriaId;
    private Long productoId;
    private Long clienteId;
    private String grupoPor; // dia, semana, mes, año, vendedor, producto, categoria
    private String ordenarPor;
    private Boolean incluirDetalles;
    private Boolean exportar;
    private String formatoExportacion; // PDF, EXCEL, CSV
}