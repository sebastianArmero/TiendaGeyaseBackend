package com.tienda.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaResponse {

    private Long id;
    private String numeroFactura;

    // Cliente
    private Long clienteId;
    private String clienteNombre;
    private String clienteDocumento;

    // Vendedor
    private Long vendedorId;
    private String vendedorNombre;

    // Caja y sucursal
    private Long cajaId;
    private String cajaNombre;
    private Long sucursalId;
    private String sucursalNombre;

    // Fechas
    private LocalDateTime fechaEmision;

    // Totales
    private BigDecimal subtotal;
    private BigDecimal descuentoTotal;
    private BigDecimal ivaTotal;
    private BigDecimal total;
    private BigDecimal efectivoRecibido;
    private BigDecimal cambio;

    // Estado
    private String formaPago;
    private String estadoPago;
    private String estado;
    private String motivoAnulacion;

    // Detalles
    private List<DetalleVentaResponse> detalles;

    // Auditoría
    private LocalDateTime creadoEn;
}