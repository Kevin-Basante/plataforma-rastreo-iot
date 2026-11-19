package com.rastreoiot.negocio.excepciones;

import java.util.List;
import org.springframework.http.HttpStatus;

/** Error de negocio con el estado HTTP y el mensaje que debe ver el usuario. */
public class NegocioException extends RuntimeException {

    private final HttpStatus estado;
    private final List<String> detalles;

    public NegocioException(HttpStatus estado, String mensaje) {
        this(estado, mensaje, List.of());
    }

    public NegocioException(HttpStatus estado, String mensaje, List<String> detalles) {
        super(mensaje);
        this.estado = estado;
        this.detalles = detalles;
    }

    public HttpStatus getEstado() { return estado; }
    public List<String> getDetalles() { return detalles; }
}
