package com.rastreoiot.seguridad;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Acceso al usuario autenticado de la petición en curso. */
public final class SesionActual {

    private SesionActual() {
    }

    public static Optional<SesionUsuario> opcional() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SesionUsuario sesion) {
            return Optional.of(sesion);
        }
        return Optional.empty();
    }

    public static SesionUsuario obtener() {
        return opcional().orElseThrow(() -> new IllegalStateException("No hay un usuario autenticado"));
    }
}
