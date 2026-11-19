package com.rastreoiot.negocio;

import com.rastreoiot.datos.ActivoRepository;
import com.rastreoiot.datos.GeocercaRepository;
import com.rastreoiot.modelo.Activo;
import com.rastreoiot.modelo.Alerta;
import com.rastreoiot.modelo.Dispositivo;
import com.rastreoiot.modelo.EstadoGeocerca;
import com.rastreoiot.modelo.Geo;
import com.rastreoiot.modelo.Geocerca;
import com.rastreoiot.modelo.LecturaTelemetria;
import com.rastreoiot.modelo.TipoAlerta;
import com.rastreoiot.negocio.dto.GeocercaRequest;
import com.rastreoiot.negocio.dto.GeocercaResponse;
import com.rastreoiot.negocio.excepciones.PermisosInsuficientesException;
import com.rastreoiot.negocio.excepciones.RecursoNoEncontradoException;
import com.rastreoiot.negocio.excepciones.ValidacionException;
import com.rastreoiot.seguridad.SesionUsuario;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Zonas de seguridad circulares: creación, estados y evaluación de entradas y salidas (HU-10, HU-11, HU-12). */
@Service
public class GeocercaService {

    public static final double RADIO_MINIMO_M = 20;
    public static final double RADIO_MAXIMO_M = 50_000;
    public static final int SILENCIO_MAXIMO_MIN = 24 * 60;

    private final GeocercaRepository geocercaRepository;
    private final DispositivoService dispositivoService;
    private final ActivoRepository activoRepository;
    private final AlertaService alertaService;

    public GeocercaService(GeocercaRepository geocercaRepository, DispositivoService dispositivoService,
                           ActivoRepository activoRepository, AlertaService alertaService) {
        this.geocercaRepository = geocercaRepository;
        this.dispositivoService = dispositivoService;
        this.activoRepository = activoRepository;
        this.alertaService = alertaService;
    }

    public List<GeocercaResponse> listar(SesionUsuario sesion, String dispositivoId) {
        List<Dispositivo> dispositivos = dispositivoId == null || dispositivoId.isBlank()
                ? dispositivoService.visibles(sesion)
                : List.of(dispositivoService.buscarConAcceso(sesion, dispositivoId));
        Map<String, Dispositivo> porId = dispositivos.stream().collect(Collectors.toMap(Dispositivo::getId, Function.identity()));
        List<Geocerca> geocercas = geocercaRepository.findByDispositivoIdIn(porId.keySet());
        Instant ahora = Instant.now();
        geocercas.stream().filter(g -> g.reanudarSiTerminoSilencio(ahora)).forEach(geocercaRepository::save);
        return geocercas.stream().map(g -> respuesta(g, porId.get(g.getDispositivoId()))).toList();
    }

    public GeocercaResponse crear(SesionUsuario sesion, GeocercaRequest req) {
        List<String> errores = new ArrayList<>();
        if (req.dispositivoId() == null || req.dispositivoId().isBlank()) errores.add("Selecciona el dispositivo");
        if (req.nombre() == null || req.nombre().isBlank()) errores.add("El nombre de la zona es obligatorio");
        if (!Geo.coordenadasValidas(req.latitudCentro(), req.longitudCentro())) {
            errores.add("El centro debe tener latitud entre -90 y 90 y longitud entre -180 y 180");
        }
        if (req.radioMetros() == null || req.radioMetros() < RADIO_MINIMO_M || req.radioMetros() > RADIO_MAXIMO_M) {
            errores.add("El radio debe estar entre " + (int) RADIO_MINIMO_M + " m y " + (int) (RADIO_MAXIMO_M / 1000) + " km");
        }
        if (!errores.isEmpty()) {
            throw new ValidacionException("Parámetros inválidos: revisa el centro y el radio de la geocerca.", errores);
        }
        Dispositivo dispositivo = dispositivoService.buscarConAcceso(sesion, req.dispositivoId());
        Geocerca geocerca = new Geocerca(dispositivo.getId(), req.nombre().trim(), req.latitudCentro(), req.longitudCentro(), req.radioMetros());
        return respuesta(geocercaRepository.save(geocerca), dispositivo);
    }

    public GeocercaResponse activar(SesionUsuario sesion, String id) {
        Geocerca g = buscarConAcceso(sesion, id);
        g.activar();
        return guardar(g);
    }

    public GeocercaResponse desactivar(SesionUsuario sesion, String id) {
        Geocerca g = buscarConAcceso(sesion, id);
        g.desactivar();
        return guardar(g);
    }

    public GeocercaResponse silenciar(SesionUsuario sesion, String id, Integer minutos) {
        if (minutos == null || minutos < 1 || minutos > SILENCIO_MAXIMO_MIN) {
            throw new ValidacionException("La duración del silencio debe estar entre 1 minuto y 24 horas.");
        }
        Geocerca g = buscarConAcceso(sesion, id);
        if (g.getEstado() == EstadoGeocerca.INACTIVA) {
            throw new ValidacionException("Solo se puede silenciar una geocerca activa.");
        }
        g.silenciar(minutos);
        return guardar(g);
    }

    public void eliminar(SesionUsuario sesion, String id) {
        geocercaRepository.delete(buscarConAcceso(sesion, id));
    }

    /**
     * Evalúa una nueva lectura contra las geocercas del dispositivo. Solo genera alertas de entrada o salida
     * cuando la geocerca está activa; inactivas o silenciadas solo actualizan la posición conocida.
     */
    public List<Alerta> evaluar(Dispositivo dispositivo, LecturaTelemetria lectura) {
        List<Alerta> alertas = new ArrayList<>();
        if (!lectura.tieneUbicacion()) {
            return alertas;
        }
        Instant ahora = Instant.now();
        String sujeto = etiqueta(dispositivo);
        for (Geocerca g : geocercaRepository.findByDispositivoId(dispositivo.getId())) {
            g.reanudarSiTerminoSilencio(ahora);
            boolean dentro = g.contiene(lectura.getLatitud(), lectura.getLongitud());
            Boolean antes = g.getDispositivoDentro();
            if (antes != null && antes != dentro && g.getEstado() == EstadoGeocerca.ACTIVA) {
                TipoAlerta tipo = dentro ? TipoAlerta.ENTRADA_GEOCERCA : TipoAlerta.SALIDA_GEOCERCA;
                String mensaje = sujeto + (dentro ? " entró a la geocerca " : " salió de la geocerca ") + g.getNombre();
                alertas.add(alertaService.generar(dispositivo, tipo, mensaje, lectura.getLatitud(), lectura.getLongitud()));
            }
            g.setDispositivoDentro(dentro);
            geocercaRepository.save(g);
        }
        return alertas;
    }

    private String etiqueta(Dispositivo d) {
        return d.getActivoId() == null ? d.getSerial()
                : activoRepository.findById(d.getActivoId()).map(Activo::getEtiqueta).orElse(d.getSerial());
    }

    private Geocerca buscarConAcceso(SesionUsuario sesion, String id) {
        Geocerca g = geocercaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La geocerca no existe."));
        try {
            dispositivoService.buscarConAcceso(sesion, g.getDispositivoId());
        } catch (RecursoNoEncontradoException e) {
            throw new PermisosInsuficientesException();
        }
        return g;
    }

    private GeocercaResponse guardar(Geocerca g) {
        Geocerca guardada = geocercaRepository.save(g);
        return respuesta(guardada, null);
    }

    private GeocercaResponse respuesta(Geocerca g, Dispositivo d) {
        Dispositivo dispositivo = d;
        if (dispositivo == null) {
            dispositivo = dispositivoService.buscarSinAcceso(g.getDispositivoId());
        }
        Activo activo = dispositivo == null || dispositivo.getActivoId() == null ? null
                : activoRepository.findById(dispositivo.getActivoId()).orElse(null);
        return GeocercaResponse.de(g, dispositivo == null ? null : dispositivo.getSerial(),
                activo == null ? null : activo.getEtiqueta(), DispositivoService.tipoActivo(activo));
    }
}
