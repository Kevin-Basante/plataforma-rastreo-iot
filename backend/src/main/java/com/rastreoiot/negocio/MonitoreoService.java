package com.rastreoiot.negocio;

import com.rastreoiot.datos.ActivoRepository;
import com.rastreoiot.datos.AlertaRepository;
import com.rastreoiot.datos.LecturaTelemetriaRepository;
import com.rastreoiot.modelo.Activo;
import com.rastreoiot.modelo.Alerta;
import com.rastreoiot.modelo.Dispositivo;
import com.rastreoiot.modelo.LecturaTelemetria;
import com.rastreoiot.modelo.Recorrido;
import com.rastreoiot.modelo.Vehiculo;
import com.rastreoiot.negocio.dto.RecorridoResponse;
import com.rastreoiot.negocio.dto.UbicacionResponse;
import com.rastreoiot.negocio.excepciones.ValidacionException;
import com.rastreoiot.seguridad.SesionUsuario;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Ubicación en tiempo real, estado del vehículo y recorridos históricos (HU-06 a HU-09, HU-16). */
@Service
public class MonitoreoService {

    /** Capas de mapa disponibles en la vista de monitoreo (HU-09). */
    public static final List<String> CAPAS = List.of("ESTANDAR", "SATELITAL");
    /** Rango máximo de consulta de recorrido para no sobrecargar el mapa. */
    public static final Duration RANGO_MAXIMO = Duration.ofDays(31);

    private final DispositivoService dispositivoService;
    private final LecturaTelemetriaRepository lecturaRepository;
    private final ActivoRepository activoRepository;
    private final AlertaRepository alertaRepository;

    public MonitoreoService(DispositivoService dispositivoService, LecturaTelemetriaRepository lecturaRepository,
                            ActivoRepository activoRepository, AlertaRepository alertaRepository) {
        this.dispositivoService = dispositivoService;
        this.lecturaRepository = lecturaRepository;
        this.activoRepository = activoRepository;
        this.alertaRepository = alertaRepository;
    }

    public List<UbicacionResponse> ubicacionesActuales(SesionUsuario sesion) {
        return dispositivoService.visibles(sesion).stream()
                .map(this::ubicacion)
                .sorted(Comparator.comparing(UbicacionResponse::activoEtiqueta, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public UbicacionResponse ubicacionActual(SesionUsuario sesion, String dispositivoId) {
        return ubicacion(dispositivoService.buscarConAcceso(sesion, dispositivoId));
    }

    private UbicacionResponse ubicacion(Dispositivo d) {
        Instant ahora = Instant.now();
        Activo activo = d.getActivoId() == null ? null : activoRepository.findById(d.getActivoId()).orElse(null);
        Optional<LecturaTelemetria> ultima = lecturaRepository.findTopByDispositivoIdOrderByFechaHoraDesc(d.getId());
        Optional<LecturaTelemetria> conPosicion = ultima.filter(LecturaTelemetria::tieneUbicacion)
                .or(() -> lecturaRepository.findTopByDispositivoIdAndLatitudNotNullOrderByFechaHoraDesc(d.getId()));
        boolean conectado = d.estaConectado(ahora);
        boolean ubicacionDisponible = conPosicion.isPresent();

        Boolean motor = null;
        Double km = null;
        if (activo instanceof Vehiculo v) {
            motor = v.getMotorEncendido();
            km = v.getKilometraje();
        }
        boolean datosMotor = activo instanceof Vehiculo && motor != null && km != null && ultima.isPresent();

        String mensaje;
        if (!ubicacionDisponible) {
            mensaje = "Ubicación no disponible: el dispositivo no ha reportado coordenadas.";
        } else if (!conectado) {
            mensaje = "Desconectado: se muestra la última ubicación conocida.";
        } else {
            mensaje = null;
        }
        LecturaTelemetria p = conPosicion.orElse(null);
        return new UbicacionResponse(d.getId(), d.getSerial(), d.getTipo(), d.getActivoId(),
                DispositivoService.tipoActivo(activo),
                activo == null ? d.getSerial() : activo.getEtiqueta(),
                activo == null ? "Sin activo asociado" : activo.getDescripcion(),
                conectado, d.getNivelBateria(), d.tieneBateriaBaja(), ubicacionDisponible,
                p == null ? null : p.getLatitud(), p == null ? null : p.getLongitud(),
                p == null || !conectado ? null : p.getVelocidad(),
                p == null ? null : p.getFechaHora(),
                datosMotor ? motor : null, datosMotor ? km : null, datosMotor,
                d.getLimiteVelocidad(), mensaje);
    }

    /** Trayectoria de un dispositivo en un rango de fechas (HU-16). */
    public Recorrido construirRecorrido(SesionUsuario sesion, String dispositivoId, Instant desde, Instant hasta) {
        validarRango(desde, hasta);
        Dispositivo d = dispositivoService.buscarConAcceso(sesion, dispositivoId);
        List<LecturaTelemetria> puntos = lecturaRepository
                .findByDispositivoIdAndFechaHoraBetweenOrderByFechaHoraAsc(d.getId(), desde, hasta).stream()
                .filter(LecturaTelemetria::tieneUbicacion)
                .toList();
        return new Recorrido(d.getId(), desde, hasta, puntos);
    }

    public List<Alerta> alertasDelRango(String dispositivoId, Instant desde, Instant hasta) {
        return alertaRepository.findByDispositivoIdAndFechaHoraBetweenOrderByFechaHoraAsc(dispositivoId, desde, hasta);
    }

    public RecorridoResponse consultarRecorrido(SesionUsuario sesion, String dispositivoId, Instant desde, Instant hasta) {
        Recorrido recorrido = construirRecorrido(sesion, dispositivoId, desde, hasta);
        Dispositivo d = dispositivoService.buscarSinAcceso(dispositivoId);
        String etiqueta = d.getActivoId() == null ? d.getSerial()
                : activoRepository.findById(d.getActivoId()).map(Activo::getEtiqueta).orElse(d.getSerial());
        if (!recorrido.tieneDatos()) {
            return new RecorridoResponse(dispositivoId, etiqueta, desde, hasta, false,
                    "No hay datos disponibles para " + etiqueta + " en el rango seleccionado.",
                    0, null, null, null, null, List.of(), List.of());
        }
        DoubleSummaryStatistics velocidades = recorrido.getPuntos().stream()
                .map(LecturaTelemetria::getVelocidad).filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue).summaryStatistics();
        List<LecturaTelemetria> puntos = recorrido.getPuntos();
        return new RecorridoResponse(dispositivoId, etiqueta, desde, hasta, true, null,
                recorrido.getDistanciaKm(),
                velocidades.getCount() == 0 ? null : velocidades.getMax(),
                velocidades.getCount() == 0 ? null : Math.round(velocidades.getAverage() * 10) / 10.0,
                puntos.get(0).getFechaHora(), puntos.get(puntos.size() - 1).getFechaHora(),
                puntos, alertasDelRango(dispositivoId, desde, hasta));
    }

    public void validarRango(Instant desde, Instant hasta) {
        if (desde == null || hasta == null) {
            throw new ValidacionException("Rango inválido: indica la fecha de inicio y la fecha final.");
        }
        if (desde.isAfter(hasta)) {
            throw new ValidacionException("Rango inválido: la fecha de inicio es posterior a la fecha final.");
        }
        if (Duration.between(desde, hasta).compareTo(RANGO_MAXIMO) > 0) {
            throw new ValidacionException("Rango inválido: el periodo máximo de consulta es de 31 días.");
        }
    }

    /** Valida la capa del mapa seleccionada (HU-09). */
    public String validarCapa(String capa) {
        String valor = capa == null ? "" : capa.trim().toUpperCase();
        if (!CAPAS.contains(valor)) {
            throw new ValidacionException("Opción no disponible: la vista de mapa \"" + capa + "\" no existe. Usa estándar o satelital.");
        }
        return valor;
    }
}
