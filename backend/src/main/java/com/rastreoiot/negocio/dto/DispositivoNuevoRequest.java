package com.rastreoiot.negocio.dto;

import com.rastreoiot.modelo.TipoDispositivo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Registro de un equipo en el catálogo (SuperAdmin). */
public record DispositivoNuevoRequest(
        @NotBlank(message = "El serial es obligatorio") String serial,
        String codigoQR,
        @NotNull(message = "El tipo de dispositivo es obligatorio") TipoDispositivo tipo) {
}
