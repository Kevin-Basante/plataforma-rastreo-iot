package com.rastreoiot.negocio.dto;

/** Señal del botón de pánico (HU-13). */
public record SosRequest(String serial, String dispositivoId, Double latitud, Double longitud) {
}
