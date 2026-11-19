package com.rastreoiot.negocio.dto;

import com.rastreoiot.modelo.TipoDispositivo;
import java.time.Instant;

/** Estado operativo de un dispositivo (HU-06) con su activo y su límite de velocidad (HU-15). */
public record DispositivoResponse(
        String id,
        String serial,
        String codigoQR,
        TipoDispositivo tipo,
        boolean conectado,
        Integer nivelBateria,
        boolean bateriaBaja,
        Instant ultimaConexion,
        Double limiteVelocidad,
        boolean configuracionPendiente,
        String usuarioId,
        String nombreUsuario,
        String activoId,
        String activoTipo,
        String activoEtiqueta,
        String activoDescripcion,
        String mensajeEstado) {
}
