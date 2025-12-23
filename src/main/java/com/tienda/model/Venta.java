package com.tienda.model;

import lombok.*;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "ventas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_factura", unique = true, nullable = false, length = 50)
    private String numeroFactura;

    @Column(name = "prefijo_factura", length = 10)
    @Builder.Default
    private String prefijoFactura = "F";

    @Column(nullable = false)
    private Integer consecutivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "cliente_nombre", length = 200)
    private String clienteNombre;

    @Column(name = "cliente_documento", length = 50)
    private String clienteDocumento;

    @Column(name = "cliente_direccion", columnDefinition = "TEXT")
    private String clienteDireccion;

    @Column(name = "cliente_telefono", length = 50)
    private String clienteTelefono;

    @Column(name = "cliente_email", length = 100)
    private String clienteEmail;

    @Column(name = "fecha_emision")
    private LocalDateTime fechaEmision;

    @Column(name = "fecha_vencimiento")
    private java.time.LocalDate fechaVencimiento;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "descuento_total", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal descuentoTotal = BigDecimal.ZERO;

    @Column(name = "iva_total", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal ivaTotal = BigDecimal.ZERO;

    @Column(name = "otros_impuestos", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal otrosImpuestos = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "efectivo_recibido", precision = 15, scale = 2)
    private BigDecimal efectivoRecibido;

    @Column(precision = 15, scale = 2)
    private BigDecimal cambio;

    @Column(name = "forma_pago", length = 50)
    @Builder.Default
    private String formaPago = "EFECTIVO";

    @Column(name = "estado_pago", length = 50)
    @Builder.Default
    private String estadoPago = "PAGADO";

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @Builder.Default
    private EstadoVenta estado = EstadoVenta.COMPLETADA;

    @Column(name = "motivo_anulacion", columnDefinition = "TEXT")
    private String motivoAnulacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id")
    private Usuario vendedor;

    @Column(name = "vendedor_nombre", length = 200)
    private String vendedorNombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caja_id")
    private Caja caja;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_anulacion_id")
    private Usuario usuarioAnulacion;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DetalleVenta> detalles = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        fechaEmision = LocalDateTime.now();
        creadoEn = LocalDateTime.now();
        actualizadoEn = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        actualizadoEn = LocalDateTime.now();
    }

    // ============ MÉTODOS NUEVOS ============

    public Long getVendedorId() {
        return vendedor != null ? vendedor.getId() : null;
    }

    public Long getClienteId() {
        return cliente != null ? cliente.getId() : null;
    }

    public Long getSucursalId() {
        return sucursal != null ? sucursal.getId() : null;
    }

    public Long getCajaId() {
        return caja != null ? caja.getId() : null;
    }

    public void calcularTotales() {
        this.subtotal = detalles.stream()
                .map(DetalleVenta::getSubtotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.total = this.subtotal
                .subtract(this.descuentoTotal != null ? this.descuentoTotal : BigDecimal.ZERO)
                .add(this.ivaTotal != null ? this.ivaTotal : BigDecimal.ZERO)
                .add(this.otrosImpuestos != null ? this.otrosImpuestos : BigDecimal.ZERO);

        if (this.efectivoRecibido != null && this.total != null) {
            this.cambio = this.efectivoRecibido.subtract(this.total);
        }
    }

    public boolean esAnulable() {
        return estado == EstadoVenta.COMPLETADA || estado == EstadoVenta.PENDIENTE;
    }

    public enum EstadoVenta {
        PENDIENTE, COMPLETADA, ANULADA, DEVUELTA, CANCELADA
    }

    public enum FormaPago {
        EFECTIVO, TARJETA_CREDITO, TARJETA_DEBITO, TRANSFERENCIA, CHEQUE, CREDITO
    }
}