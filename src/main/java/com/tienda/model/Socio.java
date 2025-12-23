package com.tienda.model;

import lombok.*;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "socios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Socio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 100)
    private String documento;

    @Column(length = 100)
    private String telefono;

    @Column(length = 100)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String direccion;

    @Column(name = "porcentaje_participacion", precision = 5, scale = 2)
    private BigDecimal porcentajeParticipacion;

    @Column(name = "capital_inicial", precision = 15, scale = 2)
    private BigDecimal capitalInicial;

    @Column(name = "fecha_ingreso")
    private LocalDateTime fechaIngreso;

    @Column(length = 20)
    @Builder.Default
    private String estado = "ACTIVO";

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @PrePersist
    protected void onCreate() {
        fechaIngreso = LocalDateTime.now();
    }
}