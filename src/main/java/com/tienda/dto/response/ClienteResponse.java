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
public class ClienteResponse {

    private Long id;
    private String tipoDocumento;
    private String numeroDocumento;
    private String nombre;
    private String email;
    private String telefono;
    private String direccion;
    private String tipo;
    private LocalDateTime fechaRegistro;
    private BigDecimal totalCompras;
    private LocalDateTime ultimaCompra;
    private String estado;
    private Integer totalVentas;
    private BigDecimal promedioTicket;
    private Integer diasUltimaCompra;
}