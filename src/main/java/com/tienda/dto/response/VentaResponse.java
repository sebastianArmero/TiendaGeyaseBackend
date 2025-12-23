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
    private String prefijoFactura;
    private Integer consecutivo;

    // Cliente
    private Long clienteId;
    private String clienteNombre;
    private String clienteDocumento;
    private String clienteTelefono;

    // Totales
    private BigDecimal subtotal;
    private BigDecimal descuentoTotal;
    private BigDecimal ivaTotal;
    private BigDecimal otrosImpuestos;
    private BigDecimal total;

    // Pago
    private String formaPago;
    private String estadoPago;
    private BigDecimal efectivoRecibido;
    private BigDecimal cambio;

    // Estado
    private String estado;
    private String motivoAnulacion;
    private LocalDateTime fechaAnulacion;

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
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;

    // Detalles
    private List<DetalleVentaResponse> detalles;
}