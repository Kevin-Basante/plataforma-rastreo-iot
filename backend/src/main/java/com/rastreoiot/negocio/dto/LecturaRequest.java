package com.rastreoiot.negocio.dto;

import java.time.Instant;

/** Lectura enviada por un dispositivo o por el simulador. Se identifica por serial o por id. */
public record LecturaRequest(
        String serial,
        String dispositivoId,
        Double latitud,
        Double longitud,
        Double velocidad,
        Integer nivelBateria,
        Boolean motorEncendido,
        Double kilometraje,
        Instant fechaHora) {
}
