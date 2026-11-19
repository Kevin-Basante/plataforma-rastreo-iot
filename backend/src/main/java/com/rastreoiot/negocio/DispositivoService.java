package com.rastreoiot.negocio;

import com.rastreoiot.datos.ActivoRepository;
import com.rastreoiot.datos.DispositivoRepository;
import com.rastreoiot.datos.UsuarioRepository;
import com.rastreoiot.modelo.Activo;
import com.rastreoiot.modelo.Dispositivo;
import com.rastreoiot.modelo.Familiar;
import com.rastreoiot.modelo.Mascota;
import com.rastreoiot.modelo.Usuario;
import com.rastreoiot.modelo.Vehiculo;
import com.rastreoiot.negocio.dto.DispositivoNuevoRequest;
import com.rastreoiot.negocio.dto.DispositivoResponse;
import com.rastreoiot.negocio.dto.EnlaceRequest;
import com.rastreoiot.negocio.excepciones.NegocioException;
import com.rastreoiot.negocio.excepciones.PermisosInsuficientesException;
import com.rastreoiot.negocio.excepciones.RecursoNoEncontradoException;
import com.rastreoiot.negocio.excepciones.ValidacionException;
import com.rastreoiot.seguridad.SesionUsuario;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Enlace, estado y configuración de dispositivos (HU-04, HU-06, HU-15). */
@Service
public class DispositivoService {

    private final DispositivoRepository dispositivoRepository;
    private final ActivoRepository activoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ControlAcceso controlAcceso;

    public DispositivoService(DispositivoRepository dispositivoRepository, ActivoRepository activoRepository,
                              UsuarioRepository usuarioRepository, ControlAcceso controlAcceso) {
        this.dispositivoRepository = dispositivoRepository;
        this.activoRepository = activoRepository;
        this.usuarioRepository = usuarioRepository;
        this.controlAcceso = controlAcceso;
    }

    public List<DispositivoResponse> listar(SesionUsuario sesion) {
        return visibles(sesion).stream()
                .sorted(Comparator.comparing(Dispositivo::getSerial))
                .map(this::respuesta)
                .toList();
    }

    /** Dispositivos enlazados que la sesión puede ver según su rol. */
    public List<Dispositivo> visibles(SesionUsuario sesion) {
        return controlAcceso.usuariosVisibles(sesion)
                .map(dispositivoRepository::findByUsuarioIdIn)
                .orElseGet(() -> dispositivoRepository.findAll().stream().filter(Dispositivo::estaEnlazado).toList());
    }

    public DispositivoResponse estado(SesionUsuario sesion, String id) {
        return respuesta(buscarConAcceso(sesion, id));
    }

    /** Enlaza un dispositivo del catálogo al perfil del usuario por QR o serial (HU-04). */
    public DispositivoResponse enlazar(SesionUsuario sesion, EnlaceRequest req) {
        Optional<Dispositivo> encontrado;
        String referencia;
        if (req.codigoQR() != null && !req.codigoQR().isBlank()) {
            referencia = req.codigoQR().trim();
            encontrado = dispositivoRepository.findByCodigoQR(referencia);
            if (encontrado.isEmpty()) {
                throw new ValidacionException("El código QR " + referencia + " no corresponde a ningún dispositivo del catálogo.");
            }
        } else if (req.serial() != null && !req.serial().isBlank()) {
            referencia = req.serial().trim().toUpperCase(Locale.ROOT);
            encontrado = dispositivoRepository.findBySerial(referencia);
            if (encontrado.isEmpty()) {
                throw new RecursoNoEncontradoException("No existe un dispositivo con el serial " + referencia + ". Revisa la etiqueta del equipo.");
            }
        } else {
            throw new ValidacionException("Escanea el código QR o ingresa el serial del dispositivo.");
        }
        Dispositivo dispositivo = encontrado.get();
        if (dispositivo.estaEnlazado()) {
            String mensaje = dispositivo.getUsuarioId().equals(sesion.id())
                    ? "El dispositivo " + dispositivo.getSerial() + " ya está enlazado a tu perfil."
                    : "El dispositivo " + dispositivo.getSerial() + " ya está asignado a otro perfil. No se puede enlazar.";
            throw new NegocioException(HttpStatus.CONFLICT, mensaje);
        }
        dispositivo.setUsuarioId(sesion.id());
        dispositivo = dispositivoRepository.save(dispositivo);
        if (req.activoId() != null && !req.activoId().isBlank()) {
            asociarActivo(sesion, dispositivo.getId(), req.activoId());
            dispositivo = dispositivoRepository.findById(dispositivo.getId()).orElseThrow();
        }
        return respuesta(dispositivo);
    }

    /** Asocia (o desasocia con activoId null) un activo monitoreado al dispositivo. */
    public DispositivoResponse asociarActivo(SesionUsuario sesion, String dispositivoId, String activoId) {
        Dispositivo dispositivo = buscarConAcceso(sesion, dispositivoId);
        if (dispositivo.getActivoId() != null) {
            activoRepository.findById(dispositivo.getActivoId()).ifPresent(anterior -> {
                anterior.setDispositivoId(null);
                activoRepository.save(anterior);
            });
            dispositivo.setActivoId(null);
        }
        if (activoId != null && !activoId.isBlank()) {
            Activo activo = activoRepository.findById(activoId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("El activo seleccionado no existe."));
            controlAcceso.verificarActivo(sesion, activo);
            if (activo.getDispositivoId() != null && !activo.getDispositivoId().equals(dispositivoId)) {
                dispositivoRepository.findById(activo.getDispositivoId()).ifPresent(otro -> {
                    otro.setActivoId(null);
                    dispositivoRepository.save(otro);
                });
            }
            activo.setDispositivoId(dispositivo.getId());
            activoRepository.save(activo);
            dispositivo.setActivoId(activo.getId());
        }
        return respuesta(dispositivoRepository.save(dispositivo));
    }

    /** Configura el límite de velocidad en km/h; null lo deja pendiente (HU-15). */
    public DispositivoResponse configurarLimite(SesionUsuario sesion, String id, Double limite) {
        Dispositivo dispositivo = buscarConAcceso(sesion, id);
        if (limite != null && (limite < 5 || limite > 250)) {
            throw new ValidacionException("El límite de velocidad debe estar entre 5 y 250 km/h.");
        }
        dispositivo.setLimiteVelocidad(limite);
        return respuesta(dispositivoRepository.save(dispositivo));
    }

    public DispositivoResponse desenlazar(SesionUsuario sesion, String id) {
        Dispositivo dispositivo = buscarConAcceso(sesion, id);
        if (dispositivo.getActivoId() != null) {
            asociarActivo(sesion, id, null);
            dispositivo = dispositivoRepository.findById(id).orElseThrow();
        }
        dispositivo.setUsuarioId(null);
        return respuesta(dispositivoRepository.save(dispositivo));
    }

    /** Alta de un equipo en el catálogo de dispositivos disponibles (solo SuperAdmin). */
    public DispositivoResponse registrarEnCatalogo(SesionUsuario sesion, DispositivoNuevoRequest req) {
        if (!sesion.esSuperAdmin()) {
            throw new PermisosInsuficientesException();
        }
        String serial = req.serial().trim().toUpperCase(Locale.ROOT);
        if (dispositivoRepository.findBySerial(serial).isPresent()) {
            throw new NegocioException(HttpStatus.CONFLICT, "Ya existe un dispositivo con el serial " + serial + ".");
        }
        String qr = req.codigoQR() == null || req.codigoQR().isBlank() ? "RIOT-" + serial : req.codigoQR().trim();
        return respuesta(dispositivoRepository.save(new Dispositivo(serial, qr, req.tipo())));
    }

    public Dispositivo buscarConAcceso(SesionUsuario sesion, String id) {
        Dispositivo dispositivo = dispositivoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El dispositivo no existe."));
        controlAcceso.verificarDispositivo(sesion, dispositivo);
        return dispositivo;
    }

    public Dispositivo buscarSinAcceso(String id) {
        return id == null ? null : dispositivoRepository.findById(id).orElse(null);
    }

    public DispositivoResponse respuesta(Dispositivo d) {
        Instant ahora = Instant.now();
        Activo activo = d.getActivoId() == null ? null : activoRepository.findById(d.getActivoId()).orElse(null);
        String nombreUsuario = d.getUsuarioId() == null ? null
                : usuarioRepository.findById(d.getUsuarioId()).map(Usuario::getNombre).orElse(null);
        boolean conectado = d.estaConectado(ahora);
        String mensaje;
        if (!conectado) {
            mensaje = d.getUltimaConexion() == null
                    ? "Desconectado: el dispositivo aún no ha reportado datos."
                    : "Desconectado: sin señal ni datos desde hace más de " + Dispositivo.TIEMPO_SIN_SENAL.toMinutes() + " minutos.";
        } else if (d.tieneBateriaBaja()) {
            mensaje = "Batería baja (" + d.getNivelBateria() + " %): recarga el dispositivo para no perder su ubicación ni sus alertas.";
        } else {
            mensaje = "Conectado y transmitiendo datos.";
        }
        return new DispositivoResponse(d.getId(), d.getSerial(), d.getCodigoQR(), d.getTipo(), conectado,
                d.getNivelBateria(), d.tieneBateriaBaja(), d.getUltimaConexion(), d.getLimiteVelocidad(),
                d.getLimiteVelocidad() == null, d.getUsuarioId(), nombreUsuario, d.getActivoId(),
                tipoActivo(activo), activo == null ? null : activo.getEtiqueta(),
                activo == null ? null : activo.getDescripcion(), mensaje);
    }

    public static String tipoActivo(Activo activo) {
        if (activo instanceof Vehiculo) {
            return "VEHICULO";
        }
        if (activo instanceof Familiar) {
            return "FAMILIAR";
        }
        if (activo instanceof Mascota) {
            return "MASCOTA";
        }
        return null;
    }
}
