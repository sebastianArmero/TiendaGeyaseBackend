package com.tienda.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnularVentaRequest {

    @NotBlank(message = "El motivo de anulación es requerido")
    private String motivo;
}