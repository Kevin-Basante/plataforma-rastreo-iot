package com.rastreoiot.negocio.dto;

import jakarta.validation.constraints.NotBlank;

public record EmpresaRequest(
        @NotBlank(message = "El nombre de la empresa es obligatorio") String nombre,
        @NotBlank(message = "El NIT es obligatorio") String nit) {
}
