package com.rastreoiot.negocio;

import com.rastreoiot.datos.UsuarioRepository;
import com.rastreoiot.modelo.Activo;
import com.rastreoiot.modelo.Dispositivo;
import com.rastreoiot.modelo.Usuario;
import com.rastreoiot.negocio.excepciones.PermisosInsuficientesException;
import com.rastreoiot.seguridad.SesionUsuario;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Reglas de acceso por rol y pertenencia (HU-02):
 * SuperAdmin ve todo, Admin de Flota B2B solo lo de su empresa y Usuario Hogar B2C solo lo propio.
 */
@Component
public class ControlAcceso {

    private final UsuarioRepository usuarioRepository;

    public ControlAcceso(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /** Ids de los usuarios cuyos recursos puede ver la sesión; vacío significa "todos" (SuperAdmin). */
    public Optional<List<String>> usuariosVisibles(SesionUsuario sesion) {
        if (sesion.esSuperAdmin()) {
            return Optional.empty();
        }
        if (sesion.esAdminFlota() && sesion.empresaId() != null) {
            return Optional.of(usuarioRepository.findByEmpresaId(sesion.empresaId()).stream().map(Usuario::getId).toList());
        }
        return Optional.of(List.of(sesion.id()));
    }

    public boolean puedeVerRecursoDe(SesionUsuario sesion, String usuarioDuenoId) {
        if (sesion.esSuperAdmin()) {
            return true;
        }
        if (usuarioDuenoId == null) {
            return false;
        }
        if (usuarioDuenoId.equals(sesion.id())) {
            return true;
        }
        if (sesion.esAdminFlota() && sesion.empresaId() != null) {
            return usuarioRepository.findById(usuarioDuenoId)
                    .map(u -> Objects.equals(u.getEmpresaId(), sesion.empresaId()))
                    .orElse(false);
        }
        return false;
    }

    public void verificarDispositivo(SesionUsuario sesion, Dispositivo dispositivo) {
        if (!puedeVerRecursoDe(sesion, dispositivo.getUsuarioId())) {
            throw new PermisosInsuficientesException(
                    "Permisos insuficientes: no tienes acceso al dispositivo " + dispositivo.getSerial() + ".");
        }
    }

    public void verificarActivo(SesionUsuario sesion, Activo activo) {
        if (!puedeVerRecursoDe(sesion, activo.getUsuarioId())) {
            throw new PermisosInsuficientesException(
                    "Permisos insuficientes: no tienes acceso a " + activo.getEtiqueta() + ".");
        }
    }

    public void verificarUsuario(SesionUsuario sesion, String usuarioId) {
        if (!puedeVerRecursoDe(sesion, usuarioId)) {
            throw new PermisosInsuficientesException("Permisos insuficientes: no puedes acceder a otras cuentas.");
        }
    }
}
