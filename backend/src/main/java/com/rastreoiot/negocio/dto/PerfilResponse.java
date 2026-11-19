package com.rastreoiot.negocio.dto;

import com.rastreoiot.modelo.ContactoEmergencia;
import com.rastreoiot.modelo.PreferenciaNotificacion;
import com.rastreoiot.modelo.Rol;
import com.rastreoiot.modelo.Usuario;
import java.time.Instant;
import java.util.List;

public record PerfilResponse(
        String id,
        String nombre,
        String correo,
        String telefono,
        String ciudad,
        String fotoPerfil,
        Rol rol,
        String empresaId,
        String empresaNombre,
        Instant fechaRegistro,
        List<ContactoEmergencia> contactosEmergencia,
        List<PreferenciaNotificacion> preferencias) {

    public static PerfilResponse de(Usuario u, String empresaNombre) {
        return new PerfilResponse(u.getId(), u.getNombre(), u.getCorreo(), u.getTelefono(), u.getCiudad(),
                u.getFotoPerfil(), u.getRol(), u.getEmpresaId(), empresaNombre, u.getFechaRegistro(),
                u.getContactosEmergencia(), u.getPreferencias());
    }
}
