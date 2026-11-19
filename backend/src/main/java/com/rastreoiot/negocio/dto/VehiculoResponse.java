package com.rastreoiot.negocio.dto;

import java.util.List;

public record VehiculoResponse(
        String id,
        String placa,
        String modelo,
        Boolean motorEncendido,
        Double kilometraje,
        String dispositivoId,
        String serialDispositivo,
        boolean conectado,
        String usuarioId,
        List<String> subFlotas) {
}
