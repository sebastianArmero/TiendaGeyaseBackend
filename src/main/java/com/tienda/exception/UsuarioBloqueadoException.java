package com.tienda.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.LOCKED)
public class UsuarioBloqueadoException extends RuntimeException {

    public UsuarioBloqueadoException(String message) {
        super(message);
    }

    public UsuarioBloqueadoException(String username, String motivo) {
        super(String.format("Usuario %s bloqueado. Motivo: %s", username, motivo));
    }
}