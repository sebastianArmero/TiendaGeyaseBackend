package com.tienda.model;

import lombok.*;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sucursales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sucursal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String direccion;

    @Column(length = 50)
    private String telefono;

    @Column(length = 100)
    private String email;

    @Column(length = 100)
    private String responsable;

    @Column(length = 20)
    @Builder.Default
    private String estado = "ACTIVA";

    @OneToMany(mappedBy = "sucursal", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Usuario> usuarios = new ArrayList<>();

    @OneToMany(mappedBy = "sucursal", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Caja> cajas = new ArrayList<>();

    @OneToMany(mappedBy = "sucursal", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Venta> ventas = new ArrayList<>();
}