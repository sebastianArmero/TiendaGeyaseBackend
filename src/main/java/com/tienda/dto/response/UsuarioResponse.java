package com.tienda.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {

    private Long id;
    private String username;
    private String email;
    private String nombreCompleto;
    private String documentoIdentidad;
    private String telefono;
    private String direccion;
    private String estado;
    private Integer intentosLogin;
    private LocalDateTime fechaUltimoLogin;
    private LocalDateTime fechaUltimoCambioPassword;
    private String rolNombre;
    private Long rolId;
    private Boolean esSuperAdmin;
    private Boolean requiereCambioPassword;
    private String sucursalNombre;
    private Long sucursalId;
    private String cajaNombre;
    private Long cajaId;
    private String temaPreferido;
    private String idiomaPreferido;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
}