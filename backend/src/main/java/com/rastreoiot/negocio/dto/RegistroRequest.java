package com.rastreoiot.negocio.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String nombre,
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico no es válido")
        String correo,
        @NotBlank(message = "La contraseña es obligatoria")
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{8,64}$",
                message = "La contraseña debe tener al menos 8 caracteres, una mayúscula y un número")
        String contrasena,
        @NotBlank(message = "El teléfono celular es obligatorio")
        @Pattern(regexp = "^[0-9 +()-]{7,20}$", message = "El teléfono celular no es válido")
        String telefono,
        @AssertTrue(message = "Debes aceptar los términos y condiciones")
        boolean aceptaTerminos) {
}
