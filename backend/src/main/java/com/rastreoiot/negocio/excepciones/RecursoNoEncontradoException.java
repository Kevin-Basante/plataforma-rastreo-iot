package com.rastreoiot.negocio.excepciones;

import org.springframework.http.HttpStatus;

public class RecursoNoEncontradoException extends NegocioException {

    public RecursoNoEncontradoException(String mensaje) {
        super(HttpStatus.NOT_FOUND, mensaje);
    }
}
