package com.rastreoiot.negocio.dto;

import com.rastreoiot.modelo.TipoAlerta;
import java.time.Instant;

public record AlertaResponse(
        String id,
        TipoAlerta tipo,
        String mensaje,
        Double latitud,
        Double longitud,
        Instant fechaHora,
        boolean leida,
        boolean inconsistente,
        boolean notificar,
        String dispositivoId,
        String serialDispositivo,
        String tipoDispositivo,
        String activoEtiqueta,
        String activoTipo,
        String usuarioNombre) {
}
