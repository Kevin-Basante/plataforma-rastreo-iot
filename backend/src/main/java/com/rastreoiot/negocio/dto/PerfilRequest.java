package com.rastreoiot.negocio.dto;

import com.rastreoiot.modelo.ContactoEmergencia;
import com.rastreoiot.modelo.PreferenciaNotificacion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PerfilRequest(
        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String nombre,
        @NotBlank(message = "El teléfono celular es obligatorio")
        @Pattern(regexp = "^[0-9 +()-]{7,20}$", message = "El teléfono celular no es válido")
        String telefono,
        @Size(max = 60, message = "La ciudad no puede superar 60 caracteres")
        String ciudad,
        @Valid @Size(max = 5, message = "Puedes registrar máximo 5 contactos de emergencia")
        List<ContactoEmergencia> contactosEmergencia,
        @Valid
        List<PreferenciaNotificacion> preferencias) {
}
