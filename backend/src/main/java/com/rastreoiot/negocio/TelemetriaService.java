package com.rastreoiot.negocio;

import com.rastreoiot.datos.ActivoRepository;
import com.rastreoiot.datos.DispositivoRepository;
import com.rastreoiot.datos.LecturaTelemetriaRepository;
import com.rastreoiot.modelo.Activo;
import com.rastreoiot.modelo.Alerta;
import com.rastreoiot.modelo.Dispositivo;
import com.rastreoiot.modelo.Geo;
import com.rastreoiot.modelo.LecturaTelemetria;
import com.rastreoiot.modelo.TipoAlerta;
import com.rastreoiot.modelo.Vehiculo;
import com.rastreoiot.negocio.dto.LecturaRequest;
import com.rastreoiot.negocio.dto.ResultadoLectura;
import com.rastreoiot.negocio.dto.SosRequest;
import com.rastreoiot.negocio.excepciones.NegocioException;
import com.rastreoiot.negocio.excepciones.RecursoNoEncontradoException;
import com.rastreoiot.negocio.excepciones.ValidacionException;
import com.rastreoiot.seguridad.SesionUsuario;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Recepción de telemetría y señales SOS de los dispositivos (HU-06, HU-07, HU-08, HU-13, HU-15). */
@Service
public class TelemetriaService {

    private final DispositivoRepository dispositivoRepository;
    private final LecturaTelemetriaRepository lecturaRepository;
    private final ActivoRepository activoRepository;
    private final GeocercaService geocercaService;
    private final AlertaService alertaService;
    private final ControlAcceso controlAcceso;

    public TelemetriaService(DispositivoRepository dispositivoRepository, LecturaTelemetriaRepository lecturaRepository,
                             ActivoRepository activoRepository, GeocercaService geocercaService,
                             AlertaService alertaService, ControlAcceso controlAcceso) {
        this.dispositivoRepository = dispositivoRepository;
        this.lecturaRepository = lecturaRepository;
        this.activoRepository = activoRepository;
        this.geocercaService = geocercaService;
        this.alertaService = alertaService;
        this.controlAcceso = controlAcceso;
    }

    /**
     * Procesa una lectura: la guarda, actualiza el estado del dispositivo y del vehículo,
     * evalúa el límite de velocidad y las geocercas.
     *
     * @param sesion usuario que envía la lectura desde la aplicación; vacío si la envía el propio dispositivo
     */
    public ResultadoLectura procesarLectura(LecturaRequest req, Optional<SesionUsuario> sesion) {
        Dispositivo dispositivo = buscar(req.serial(), req.dispositivoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("El dispositivo que envía la lectura no está registrado."));
        sesion.ifPresent(s -> controlAcceso.verificarDispositivo(s, dispositivo));
        if (!dispositivo.estaEnlazado()) {
            throw new NegocioException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "El dispositivo " + dispositivo.getSerial() + " no está enlazado a ningún perfil; la lectura no se procesó.");
        }
        validar(req);

        LecturaTelemetria lectura = new LecturaTelemetria();
        lectura.setDispositivoId(dispositivo.getId());
        lectura.setLatitud(req.latitud());
        lectura.setLongitud(req.longitud());
        lectura.setVelocidad(req.velocidad());
        lectura.setNivelBateria(req.nivelBateria());
        lectura.setMotorEncendido(req.motorEncendido());
        lectura.setKilometraje(req.kilometraje());
        lectura.setFechaHora(req.fechaHora() != null ? req.fechaHora() : Instant.now());
        lectura = lecturaRepository.save(lectura);

        dispositivo.setConectado(true);
        dispositivo.setUltimaConexion(lectura.getFechaHora());
        if (req.nivelBateria() != null) {
            dispositivo.setNivelBateria(req.nivelBateria());
        }
        dispositivoRepository.save(dispositivo);
        actualizarVehiculo(dispositivo, lectura);

        List<Alerta> alertas = new ArrayList<>();
        boolean pendiente = dispositivo.getLimiteVelocidad() == null;
        evaluarVelocidad(dispositivo, lectura).ifPresent(alertas::add);
        alertas.addAll(geocercaService.evaluar(dispositivo, lectura));

        String mensaje = pendiente
                ? "Lectura registrada. Configuración pendiente: el dispositivo no tiene límite de velocidad, no se evalúa el exceso."
                : "Lectura registrada.";
        return new ResultadoLectura(lectura.getId(), alertas, pendiente, mensaje);
    }

    /** Registra la señal de auxilio. Si el dispositivo no existe o no está asociado, la marca como inconsistente. */
    public Alerta registrarSos(SosRequest req, Optional<SesionUsuario> sesion) {
        Optional<Dispositivo> encontrado = buscar(req.serial(), req.dispositivoId());
        String serial = encontrado.map(Dispositivo::getSerial)
                .orElse(req.serial() == null ? req.dispositivoId() : req.serial().trim().toUpperCase(Locale.ROOT));
        if (encontrado.isEmpty() || !encontrado.get().estaEnlazado()) {
            String motivo = encontrado.isEmpty()
                    ? "SOS recibido de un dispositivo inexistente (" + serial + ")"
                    : "SOS recibido de un dispositivo no asociado a ningún perfil (" + serial + ")";
            alertaService.registrarInconsistente(encontrado.map(Dispositivo::getId).orElse(null), serial, motivo,
                    req.latitud(), req.longitud());
            throw new NegocioException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Evento SOS inconsistente: " + motivo.substring(4) + ". Se notificó al equipo de soporte.");
        }
        Dispositivo dispositivo = encontrado.get();
        sesion.ifPresent(s -> controlAcceso.verificarDispositivo(s, dispositivo));
        Double lat = req.latitud();
        Double lon = req.longitud();
        if (!Geo.coordenadasValidas(lat, lon)) {
            Optional<LecturaTelemetria> ultima = lecturaRepository.findTopByDispositivoIdAndLatitudNotNullOrderByFechaHoraDesc(dispositivo.getId());
            lat = ultima.map(LecturaTelemetria::getLatitud).orElse(null);
            lon = ultima.map(LecturaTelemetria::getLongitud).orElse(null);
        }
        String sujeto = etiqueta(dispositivo);
        dispositivo.setConectado(true);
        dispositivo.setUltimaConexion(Instant.now());
        dispositivoRepository.save(dispositivo);
        return alertaService.generar(dispositivo, TipoAlerta.SOS, "Botón de pánico presionado por " + sujeto, lat, lon);
    }

    private Optional<Alerta> evaluarVelocidad(Dispositivo d, LecturaTelemetria l) {
        Double limite = d.getLimiteVelocidad();
        if (limite == null || l.getVelocidad() == null || l.getVelocidad() <= limite) {
            return Optional.empty();
        }
        String sujeto = etiqueta(d);
        String mensaje = String.format(Locale.ROOT, "%s a %.0f km/h con límite de %.0f km/h", sujeto, l.getVelocidad(), limite);
        return Optional.of(alertaService.generar(d, TipoAlerta.EXCESO_VELOCIDAD, mensaje, l.getLatitud(), l.getLongitud()));
    }

    private String etiqueta(Dispositivo d) {
        return d.getActivoId() == null ? d.getSerial()
                : activoRepository.findById(d.getActivoId()).map(Activo::getEtiqueta).orElse(d.getSerial());
    }

    private void actualizarVehiculo(Dispositivo d, LecturaTelemetria l) {
        if (d.getActivoId() == null || (l.getMotorEncendido() == null && l.getKilometraje() == null)) {
            return;
        }
        activoRepository.findById(d.getActivoId()).ifPresent(activo -> {
            if (activo instanceof Vehiculo v) {
                if (l.getMotorEncendido() != null) {
                    v.setMotorEncendido(l.getMotorEncendido());
                }
                if (l.getKilometraje() != null) {
                    v.setKilometraje(l.getKilometraje());
                }
                activoRepository.save(v);
            }
        });
    }

    private void validar(LecturaRequest req) {
        List<String> errores = new ArrayList<>();
        if ((req.latitud() == null) != (req.longitud() == null)
                || (req.latitud() != null && !Geo.coordenadasValidas(req.latitud(), req.longitud()))) {
            errores.add("Coordenadas inválidas");
        }
        if (req.velocidad() != null && (req.velocidad() < 0 || req.velocidad() > 400)) {
            errores.add("Velocidad fuera de rango (0 a 400 km/h)");
        }
        if (req.nivelBateria() != null && (req.nivelBateria() < 0 || req.nivelBateria() > 100)) {
            errores.add("Nivel de batería fuera de rango (0 a 100 %)");
        }
        if (req.kilometraje() != null && req.kilometraje() < 0) {
            errores.add("El kilometraje no puede ser negativo");
        }
        if (!errores.isEmpty()) {
            throw new ValidacionException("La lectura de telemetría no es válida.", errores);
        }
    }

    private Optional<Dispositivo> buscar(String serial, String id) {
        if (serial != null && !serial.isBlank()) {
            return dispositivoRepository.findBySerial(serial.trim().toUpperCase(Locale.ROOT));
        }
        if (id != null && !id.isBlank()) {
            return dispositivoRepository.findById(id);
        }
        return Optional.empty();
    }
}
