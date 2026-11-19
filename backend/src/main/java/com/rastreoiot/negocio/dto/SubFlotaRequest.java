package com.rastreoiot.negocio.dto;

import java.util.List;

public record SubFlotaRequest(String nombre, String descripcion, String empresaId, List<String> vehiculoIds) {
}
