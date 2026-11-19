package com.rastreoiot.negocio.dto;

import java.util.List;

public record SubFlotaResponse(String id, String empresaId, String nombre, String descripcion, List<VehiculoResponse> vehiculos) {
}
