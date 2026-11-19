package com.rastreoiot.negocio.dto;

import com.rastreoiot.modelo.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Alta de usuarios por parte del SuperAdmin (por ejemplo, administradores de flota). */
public record UsuarioNuevoRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no es válido") String correo,
        @NotBlank(message = "La contraseña es obligatoria")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$",
                message = "La contraseña debe tener al menos 8 caracteres e incluir letras y números")
        String contrasena,
        @NotBlank(message = "El teléfono es obligatorio") String telefono,
        @NotNull(message = "El rol es obligatorio") Rol rol,
        String empresaId) {
}
