package com.rastreoiot.negocio;

import com.rastreoiot.datos.ActivoRepository;
import com.rastreoiot.datos.DispositivoRepository;
import com.rastreoiot.datos.LecturaTelemetriaRepository;
import com.rastreoiot.modelo.Activo;
import com.rastreoiot.modelo.Dispositivo;
import com.rastreoiot.modelo.Geo;
import com.rastreoiot.modelo.TipoDispositivo;
import com.rastreoiot.modelo.Vehiculo;
import com.rastreoiot.negocio.dto.LecturaRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Simula el envío de telemetría de los dispositivos de demostración, ya que el proyecto no cuenta con hardware.
 * Cada lectura pasa por {@link TelemetriaService#procesarLectura}, igual que una lectura real, por lo que
 * también dispara las alertas de geocerca y de exceso de velocidad. Se desactiva con SIMULADOR=false.
 */
@Service
public class SimuladorTelemetria {

    private static final Logger LOG = LoggerFactory.getLogger(SimuladorTelemetria.class);

    /** Dispositivos que no se simulan: el collar de Luna (desconectado) y el GPS de BGP-307 (sin estado válido). */
    private static final Set<String> SIN_SENAL = Set.of("COL-2B8D-90E4", "GPS-6D20-4E91");
    /** Vehículos estacionados: reportan posición fija con el motor apagado. */
    private static final Set<String> ESTACIONADOS = Set.of("GPS-1F77-C3A0", "GPS-4D81-09B3", "GPS-9C70-1D82",
            "GPS-A1D4-6E35", "GPS-B2E8-7F51", "GPS-C3F9-0A64");
    /** Vehículos que de vez en cuando superan su límite de velocidad. */
    private static final Set<String> CON_EXCESOS = Set.of("GPS-4C1E-77A2", "GPS-2B19-8D44");

    private final DispositivoRepository dispositivoRepository;
    private final ActivoRepository activoRepository;
    private final LecturaTelemetriaRepository lecturaRepository;
    private final TelemetriaService telemetriaService;
    private final ObjectMapper objectMapper;
    private final boolean habilitado;
    private final long intervaloMs;
    private final int diasRetencion;
    private final Map<String, Estado> estados = new HashMap<>();
    private long ciclo;

    public SimuladorTelemetria(DispositivoRepository dispositivoRepository, ActivoRepository activoRepository,
                               LecturaTelemetriaRepository lecturaRepository, TelemetriaService telemetriaService,
                               ObjectMapper objectMapper,
                               @Value("${rastreo.simulador.habilitado}") boolean habilitado,
                               @Value("${rastreo.simulador.intervalo-ms}") long intervaloMs,
                               @Value("${rastreo.simulador.dias-retencion}") int diasRetencion) {
        this.dispositivoRepository = dispositivoRepository;
        this.activoRepository = activoRepository;
        this.lecturaRepository = lecturaRepository;
        this.telemetriaService = telemetriaService;
        this.objectMapper = objectMapper;
        this.habilitado = habilitado;
        this.intervaloMs = intervaloMs;
        this.diasRetencion = diasRetencion;
    }

    @Scheduled(fixedDelayString = "${rastreo.simulador.intervalo-ms}", initialDelay = 10_000)
    public void simular() {
        if (!habilitado) {
            return;
        }
        ciclo++;
        for (Dispositivo d : dispositivoRepository.findAll()) {
            if (!d.estaEnlazado() || d.getActivoId() == null || SIN_SENAL.contains(d.getSerial())) {
                continue;
            }
            try {
                simularDispositivo(d);
            } catch (RuntimeException e) {
                LOG.warn("No se pudo simular la lectura de {}: {}", d.getSerial(), e.getMessage());
            }
        }
    }

    /** Elimina lecturas antiguas para no llenar la base de datos gratuita. */
    @Scheduled(cron = "0 15 3 * * *", zone = "America/Bogota")
    public void limpiar() {
        lecturaRepository.deleteByFechaHoraBefore(Instant.now().minus(Duration.ofDays(diasRetencion)));
    }

    private void simularDispositivo(Dispositivo d) {
        Estado e = estados.computeIfAbsent(d.getId(), id -> crearEstado(d));
        if (e == null) {
            return;
        }
        Optional<Activo> activo = activoRepository.findById(d.getActivoId());
        Vehiculo vehiculo = activo.filter(Vehiculo.class::isInstance).map(Vehiculo.class::cast).orElse(null);

        boolean apagadoRemotamente = !e.estacionado && vehiculo != null && Boolean.FALSE.equals(vehiculo.getMotorEncendido());
        if (apagadoRemotamente && ++e.ciclosDetenido > 30) {
            // Tras ~10 minutos detenido por una orden remota, el conductor vuelve a encender el vehículo.
            e.ciclosDetenido = 0;
            apagadoRemotamente = false;
        }
        if (e.estacionado || apagadoRemotamente) {
            // Reporta cada ~2 minutos para seguir apareciendo como conectado.
            if (ciclo % 6 == 1) {
                double[] p = e.posicion();
                enviar(d, p[0], p[1], 0.0, e.bateria, vehiculo == null ? null : false,
                        vehiculo == null ? null : vehiculo.getKilometraje());
            }
            return;
        }
        double velocidad = e.velocidadBase + Math.sin(ciclo / 3.0 + e.fase) * e.variacion;
        if (CON_EXCESOS.contains(d.getSerial()) && (ciclo + (long) e.fase) % 45 == 0 && d.getLimiteVelocidad() != null) {
            velocidad = d.getLimiteVelocidad() + 7 + (ciclo % 9);
        }
        velocidad = Math.max(0, velocidad);
        double metros = velocidad / 3.6 * (intervaloMs / 1000.0);
        double[] p = e.avanzar(metros);
        Double km = null;
        if (vehiculo != null) {
            km = Math.round(((vehiculo.getKilometraje() == null ? 0 : vehiculo.getKilometraje()) + metros / 1000) * 10) / 10.0;
        }
        enviar(d, p[0], p[1], Math.round(velocidad * 10) / 10.0, e.bateria, vehiculo == null ? null : true, km);
    }

    private void enviar(Dispositivo d, double lat, double lon, double vel, int bateria, Boolean motor, Double km) {
        telemetriaService.procesarLectura(new LecturaRequest(d.getSerial(), null, lat, lon, vel, bateria, motor, km, null),
                Optional.empty());
    }

    private Estado crearEstado(Dispositivo d) {
        long semilla = Math.abs((long) d.getSerial().hashCode());
        int bateria = d.getNivelBateria() == null ? 80 : d.getNivelBateria();
        double fase = semilla % 17;
        if ("GPS-4C1E-77A2".equals(d.getSerial())) {
            List<double[]> ruta = rutaDemo();
            List<double[]> idaYVuelta = new ArrayList<>(ruta);
            for (int i = ruta.size() - 2; i > 0; i--) {
                idaYVuelta.add(ruta.get(i));
            }
            return new Estado(idaYVuelta, 38, 14, fase, bateria, false);
        }
        if (d.getTipo() == TipoDispositivo.PULSERA) {
            // Camina alrededor de la geocerca "Casa", entrando y saliendo de la zona.
            return new Estado(circulo(4.64874 + 0.0012, -74.06421, 230, 24), 4, 0.6, fase, bateria, false);
        }
        if (d.getTipo() == TipoDispositivo.COLLAR) {
            return new Estado(circulo(4.67310, -74.05530, 320, 20), 5, 1, fase, bateria, false);
        }
        // Vehículos de flota: recorridos circulares alrededor de distintos puntos de Bogotá.
        double[][] centros = {
                {4.6950, -74.0450}, {4.7110, -74.0720}, {4.6100, -74.0800}, {4.6480, -74.1000},
                {4.6290, -74.0650}, {4.6770, -74.0950}, {4.6600, -74.1200}, {4.7300, -74.0500}};
        double[] c = centros[(int) (semilla % centros.length)];
        boolean estacionado = ESTACIONADOS.contains(d.getSerial());
        double radio = 900 + semilla % 1400;
        return new Estado(circulo(c[0], c[1], radio, 28), 34 + semilla % 14, 10, fase, bateria, estacionado);
    }

    private List<double[]> rutaDemo() {
        try (var in = new ClassPathResource("demo/ruta-klm482.json").getInputStream()) {
            return List.of(objectMapper.readValue(in, double[][].class));
        } catch (java.io.IOException e) {
            return circulo(4.6487, -74.0642, 1500, 24);
        }
    }

    private static List<double[]> circulo(double lat, double lon, double radioM, int puntos) {
        List<double[]> lista = new ArrayList<>();
        double dLat = radioM / 111_320.0;
        double dLon = radioM / (111_320.0 * Math.cos(Math.toRadians(lat)));
        for (int i = 0; i < puntos; i++) {
            double a = 2 * Math.PI * i / puntos;
            // Ligera deformación para que no sea un círculo perfecto.
            double f = 1 + 0.12 * Math.sin(3 * a);
            lista.add(new double[]{lat + dLat * f * Math.sin(a), lon + dLon * f * Math.cos(a)});
        }
        return lista;
    }

    /** Posición de un dispositivo sobre su ruta cerrada. */
    private static final class Estado {
        private final List<double[]> ruta;
        private final double velocidadBase;
        private final double variacion;
        private final double fase;
        private final int bateria;
        private final boolean estacionado;
        private int tramo;
        private double avanceEnTramo;
        private int ciclosDetenido;

        Estado(List<double[]> ruta, double velocidadBase, double variacion, double fase, int bateria, boolean estacionado) {
            this.ruta = ruta;
            this.velocidadBase = velocidadBase;
            this.variacion = variacion;
            this.fase = fase;
            this.bateria = bateria;
            this.estacionado = estacionado;
            this.tramo = (int) (fase % ruta.size());
        }

        double[] posicion() {
            double[] a = ruta.get(tramo);
            double[] b = ruta.get((tramo + 1) % ruta.size());
            double largo = Math.max(1, Geo.distanciaMetros(a[0], a[1], b[0], b[1]));
            double t = Math.min(1, avanceEnTramo / largo);
            return new double[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t};
        }

        double[] avanzar(double metros) {
            double restante = metros;
            while (restante > 0) {
                double[] a = ruta.get(tramo);
                double[] b = ruta.get((tramo + 1) % ruta.size());
                double largo = Geo.distanciaMetros(a[0], a[1], b[0], b[1]);
                if (avanceEnTramo + restante < largo) {
                    avanceEnTramo += restante;
                    restante = 0;
                } else {
                    restante -= Math.max(0, largo - avanceEnTramo);
                    avanceEnTramo = 0;
                    tramo = (tramo + 1) % ruta.size();
                }
            }
            return posicion();
        }
    }
}
