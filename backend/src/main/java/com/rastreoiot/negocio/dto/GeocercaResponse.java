package com.rastreoiot.negocio.dto;

import com.rastreoiot.modelo.EstadoGeocerca;
import com.rastreoiot.modelo.Geocerca;
import java.time.Instant;

public record GeocercaResponse(
        String id,
        String dispositivoId,
        String serialDispositivo,
        String activoEtiqueta,
        String activoTipo,
        String nombre,
        double latitudCentro,
        double longitudCentro,
        double radioMetros,
        EstadoGeocerca estado,
        Instant silenciadaHasta) {

    public static GeocercaResponse de(Geocerca g, String serial, String etiqueta, String tipo) {
        return new GeocercaResponse(g.getId(), g.getDispositivoId(), serial, etiqueta, tipo, g.getNombre(),
                g.getLatitudCentro(), g.getLongitudCentro(), g.getRadioMetros(), g.getEstado(), g.getSilenciadaHasta());
    }
}
