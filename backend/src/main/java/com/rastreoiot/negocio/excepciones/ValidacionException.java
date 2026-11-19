package com.rastreoiot.negocio.excepciones;

import java.util.List;
import org.springframework.http.HttpStatus;

public class ValidacionException extends NegocioException {

    public ValidacionException(String mensaje) {
        super(HttpStatus.BAD_REQUEST, mensaje);
    }

    public ValidacionException(String mensaje, List<String> detalles) {
        super(HttpStatus.BAD_REQUEST, mensaje, detalles);
    }
}
