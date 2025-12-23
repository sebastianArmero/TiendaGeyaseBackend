package com.tienda.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarUsuarioRequest {

    @Email(message = "El email debe ser válido")
    private String email;

    private String nombreCompleto;
    private String documentoIdentidad;
    private String telefono;
    private String direccion;
    private Long rolId;
    private Long sucursalId;
    private Long cajaId;
    private String temaPreferido;
    private String idiomaPreferido;
    private Boolean esSuperAdmin;
}