package com.rastreoiot.seguridad;

import com.rastreoiot.modelo.Rol;

/** Datos del usuario autenticado que viajan en el token JWT. */
public record SesionUsuario(String id, String correo, String nombre, Rol rol, String empresaId) {

    public boolean esSuperAdmin() {
        return rol == Rol.SUPERADMIN;
    }

    public boolean esAdminFlota() {
        return rol == Rol.ADMIN_FLOTA_B2B;
    }

    public boolean esUsuarioHogar() {
        return rol == Rol.USUARIO_HOGAR_B2C;
    }
}
