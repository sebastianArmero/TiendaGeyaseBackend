package com.tienda.model;

import lombok.*;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "proveedores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 100)
    private String contacto;

    @Column(length = 50)
    private String telefono;

    @Column(length = 100)
    private String email;

    @Column(length = 100)
    private String ruc;

    @Column(columnDefinition = "TEXT")
    private String direccion;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private EstadoProveedor estado = EstadoProveedor.ACTIVO;

    @Column(name = "dias_credito")
    @Builder.Default
    private Integer diasCredito = 0;

    @Column(name = "limite_credito", precision = 15, scale = 2)
    private java.math.BigDecimal limiteCredito;

    @OneToMany(mappedBy = "proveedor", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Producto> productos = new ArrayList<>();

    public enum EstadoProveedor {
        ACTIVO, INACTIVO, SUSPENDIDO
    }
}