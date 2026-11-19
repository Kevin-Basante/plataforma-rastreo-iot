package com.rastreoiot.negocio.dto;

import com.rastreoiot.modelo.TipoDispositivo;
import java.time.Instant;

/** Posición actual y estado del activo para el mapa de monitoreo (HU-06, HU-07, HU-08). */
public record UbicacionResponse(
        String dispositivoId,
        String serial,
        TipoDispositivo tipoDispositivo,
        String activoId,
        String activoTipo,
        String activoEtiqueta,
        String activoDescripcion,
        boolean conectado,
        Integer nivelBateria,
        boolean bateriaBaja,
        boolean ubicacionDisponible,
        Double latitud,
        Double longitud,
        Double velocidad,
        Instant fechaHora,
        Boolean motorEncendido,
        Double kilometraje,
        boolean datosMotorDisponibles,
        Double limiteVelocidad,
        String mensaje) {
}
