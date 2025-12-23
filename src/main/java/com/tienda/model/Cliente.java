package com.tienda.model;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_documento", length = 20)
    @Builder.Default
    private String tipoDocumento = "DNI";

    @Column(name = "numero_documento", length = 50)
    private String numeroDocumento;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 100)
    private String email;

    @Column(length = 50)
    private String telefono;

    @Column(columnDefinition = "TEXT")
    private String direccion;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private TipoCliente tipo = TipoCliente.OCASIONAL;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    @Column(name = "total_compras", precision = 15, scale = 2)
    @Builder.Default
    private java.math.BigDecimal totalCompras = java.math.BigDecimal.ZERO;

    @Column(name = "ultima_compra")
    private LocalDateTime ultimaCompra;

    @Column(length = 20)
    @Builder.Default
    private String estado = "ACTIVO";

    @OneToMany(mappedBy = "cliente", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Venta> ventas = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        fechaRegistro = LocalDateTime.now();
    }

    public enum TipoCliente {
        OCASIONAL, REGULAR, PREMIUM, CORPORATIVO
    }
}