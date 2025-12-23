package com.tienda.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String message) {
        super(message);
    }

    public AccesoDenegadoException(String recurso, String accion) {
        super(String.format("No tiene permiso para %s %s", accion, recurso));
    }
}