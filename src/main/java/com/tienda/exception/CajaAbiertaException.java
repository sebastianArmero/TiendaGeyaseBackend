package com.tienda.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class CajaAbiertaException extends RuntimeException {

    public CajaAbiertaException(String message) {
        super(message);
    }

    public CajaAbiertaException(String cajaNombre, String usuario) {
        super(String.format("La caja %s ya está abierta por el usuario %s",
                cajaNombre, usuario));
    }
}