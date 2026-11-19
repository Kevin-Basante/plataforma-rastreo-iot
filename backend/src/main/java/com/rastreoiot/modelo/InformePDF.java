package com.rastreoiot.modelo;

import java.time.Instant;

/** Informe generado en memoria para su descarga (HU-17). */
public class InformePDF {

    private final String nombreArchivo;
    private final byte[] contenido;
    private final Instant fechaGeneracion;

    public InformePDF(String nombreArchivo, byte[] contenido) {
        this.nombreArchivo = nombreArchivo;
        this.contenido = contenido;
        this.fechaGeneracion = Instant.now();
    }

    public String getNombreArchivo() { return nombreArchivo; }
    public byte[] getContenido() { return contenido; }
    public Instant getFechaGeneracion() { return fechaGeneracion; }
}
