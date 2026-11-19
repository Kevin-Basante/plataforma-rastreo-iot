package com.rastreoiot.negocio.dto;

import com.rastreoiot.modelo.TipoOrden;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrdenRequest(
        @NotBlank(message = "Debes seleccionar un vehículo") String vehiculoId,
        @NotNull(message = "Debes seleccionar el tipo de orden") TipoOrden tipo) {
}
