package com.rastreoiot.negocio.excepciones;

import org.springframework.http.HttpStatus;

public class PermisosInsuficientesException extends NegocioException {

    public PermisosInsuficientesException() {
        this("Permisos insuficientes para realizar esta acción.");
    }

    public PermisosInsuficientesException(String mensaje) {
        super(HttpStatus.FORBIDDEN, mensaje);
    }
}
