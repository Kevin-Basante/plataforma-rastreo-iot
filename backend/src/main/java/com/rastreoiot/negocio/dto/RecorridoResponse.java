package com.rastreoiot.negocio.dto;

import com.rastreoiot.modelo.Alerta;
import com.rastreoiot.modelo.LecturaTelemetria;
import java.time.Instant;
import java.util.List;

public record RecorridoResponse(
        String dispositivoId,
        String activoEtiqueta,
        Instant desde,
        Instant hasta,
        boolean hayDatos,
        String mensaje,
        double distanciaKm,
        Double velocidadMaxima,
        Double velocidadPromedio,
        Instant inicioReal,
        Instant finReal,
        List<LecturaTelemetria> puntos,
        List<Alerta> eventos) {
}
