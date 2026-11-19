package com.rastreoiot.negocio;

import com.rastreoiot.datos.ActivoRepository;
import com.rastreoiot.datos.AlertaRepository;
import com.rastreoiot.datos.DispositivoRepository;
import com.rastreoiot.datos.UsuarioRepository;
import com.rastreoiot.modelo.Activo;
import com.rastreoiot.modelo.Alerta;
import com.rastreoiot.modelo.Dispositivo;
import com.rastreoiot.modelo.TipoAlerta;
import com.rastreoiot.modelo.Usuario;
import com.rastreoiot.negocio.dto.AlertaResponse;
import com.rastreoiot.negocio.excepciones.PermisosInsuficientesException;
import com.rastreoiot.negocio.excepciones.RecursoNoEncontradoException;
import com.rastreoiot.seguridad.SesionUsuario;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Registro y consulta de alertas: geocercas, SOS y exceso de velocidad (HU-12, HU-13, HU-15). */
@Service
public class AlertaService {

    private static final int MAXIMO_LISTADO = 300;

    private final AlertaRepository alertaRepository;
    private final DispositivoRepository dispositivoRepository;
    private final ActivoRepository activoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ControlAcceso controlAcceso;

    public AlertaService(AlertaRepository alertaRepository, DispositivoRepository dispositivoRepository,
                         ActivoRepository activoRepository, UsuarioRepository usuarioRepository, ControlAcceso controlAcceso) {
        this.alertaRepository = alertaRepository;
        this.dispositivoRepository = dispositivoRepository;
        this.activoRepository = activoRepository;
        this.usuarioRepository = usuarioRepository;
        this.controlAcceso = controlAcceso;
    }

    /** Registra una alerta con su marca de tiempo para el dueño del dispositivo. */
    public Alerta generar(Dispositivo dispositivo, TipoAlerta tipo, String mensaje, Double latitud, Double longitud) {
        Alerta alerta = new Alerta(dispositivo.getId(), tipo, mensaje, latitud, longitud);
        alerta.setSerialDispositivo(dispositivo.getSerial());
        alerta.setUsuarioId(dispositivo.getUsuarioId());
        alerta.setNotificar(Optional.ofNullable(dispositivo.getUsuarioId())
                .flatMap(usuarioRepository::findById)
                .map(u -> u.quiereNotificacion(tipo))
                .orElse(true));
        return alertaRepository.save(alerta);
    }

    /** Evento SOS de un dispositivo inexistente o sin asociar: queda marcado para el equipo de soporte. */
    public Alerta registrarInconsistente(String dispositivoId, String serial, String motivo, Double latitud, Double longitud) {
        Alerta alerta = new Alerta(dispositivoId, TipoAlerta.SOS, motivo, latitud, longitud);
        alerta.setSerialDispositivo(serial);
        alerta.setInconsistente(true);
        return alertaRepository.save(alerta);
    }

    public List<AlertaResponse> listar(SesionUsuario sesion, TipoAlerta tipo, String dispositivoId, Boolean soloNoLeidas) {
        return consultar(sesion, dispositivoId).stream()
                .filter(a -> tipo == null || a.getTipo() == tipo)
                .filter(a -> soloNoLeidas == null || !soloNoLeidas || !a.isLeida())
                .limit(MAXIMO_LISTADO)
                .map(new Traductor()::respuesta)
                .toList();
    }

    public long contarNoLeidas(SesionUsuario sesion) {
        return consultar(sesion, null).stream().filter(a -> !a.isLeida() && a.isNotificar()).count();
    }

    public AlertaResponse marcarLeida(SesionUsuario sesion, String id) {
        Alerta alerta = alertaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La alerta no existe."));
        verificar(sesion, alerta);
        alerta.setLeida(true);
        return new Traductor().respuesta(alertaRepository.save(alerta));
    }

    public int marcarTodasLeidas(SesionUsuario sesion) {
        List<Alerta> pendientes = consultar(sesion, null).stream().filter(a -> !a.isLeida()).toList();
        pendientes.forEach(a -> a.setLeida(true));
        alertaRepository.saveAll(pendientes);
        return pendientes.size();
    }

    private List<Alerta> consultar(SesionUsuario sesion, String dispositivoId) {
        if (dispositivoId != null && !dispositivoId.isBlank()) {
            Dispositivo d = dispositivoRepository.findById(dispositivoId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("El dispositivo no existe."));
            controlAcceso.verificarDispositivo(sesion, d);
            return alertaRepository.findByDispositivoIdOrderByFechaHoraDesc(dispositivoId);
        }
        if (sesion.esSuperAdmin()) {
            return alertaRepository.findAllByOrderByFechaHoraDesc();
        }
        List<String> usuarios = controlAcceso.usuariosVisibles(sesion).orElse(List.of());
        List<String> dispositivos = dispositivoRepository.findByUsuarioIdIn(usuarios).stream().map(Dispositivo::getId).toList();
        return alertaRepository.findByDispositivoIdInOrderByFechaHoraDesc(dispositivos).stream()
                .filter(a -> !a.isInconsistente())
                .toList();
    }

    private void verificar(SesionUsuario sesion, Alerta alerta) {
        if (alerta.isInconsistente()) {
            if (!sesion.esSuperAdmin()) {
                throw new PermisosInsuficientesException();
            }
            return;
        }
        if (!controlAcceso.puedeVerRecursoDe(sesion, alerta.getUsuarioId())) {
            throw new PermisosInsuficientesException();
        }
    }

    /** Convierte alertas a respuesta reutilizando las consultas de dispositivos, activos y usuarios. */
    public class Traductor {
        private final Map<String, Optional<Dispositivo>> dispositivos = new HashMap<>();
        private final Map<String, Optional<Activo>> activos = new HashMap<>();
        private final Map<String, Optional<Usuario>> usuarios = new HashMap<>();

        public AlertaResponse respuesta(Alerta a) {
            Optional<Dispositivo> d = a.getDispositivoId() == null ? Optional.empty()
                    : dispositivos.computeIfAbsent(a.getDispositivoId(), dispositivoRepository::findById);
            Optional<Activo> activo = d.map(Dispositivo::getActivoId)
                    .flatMap(id -> activos.computeIfAbsent(id, activoRepository::findById));
            Optional<Usuario> usuario = a.getUsuarioId() == null ? Optional.empty()
                    : usuarios.computeIfAbsent(a.getUsuarioId(), usuarioRepository::findById);
            return new AlertaResponse(a.getId(), a.getTipo(), a.getMensaje(), a.getLatitud(), a.getLongitud(),
                    a.getFechaHora(), a.isLeida(), a.isInconsistente(), a.isNotificar(), a.getDispositivoId(),
                    a.getSerialDispositivo(), d.map(x -> x.getTipo().name()).orElse(null),
                    activo.map(Activo::getEtiqueta).orElse(a.isInconsistente() ? "Sin activo asociado" : null),
                    activo.map(DispositivoService::tipoActivo).orElse(null),
                    usuario.map(Usuario::getNombre).orElse(null));
        }
    }
}
