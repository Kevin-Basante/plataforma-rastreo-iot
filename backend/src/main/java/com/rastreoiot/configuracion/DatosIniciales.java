package com.rastreoiot.configuracion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rastreoiot.datos.ActivoRepository;
import com.rastreoiot.datos.AlertaRepository;
import com.rastreoiot.datos.DispositivoRepository;
import com.rastreoiot.datos.EmpresaRepository;
import com.rastreoiot.datos.GeocercaRepository;
import com.rastreoiot.datos.LecturaTelemetriaRepository;
import com.rastreoiot.datos.OrdenRemotaRepository;
import com.rastreoiot.datos.SubFlotaRepository;
import com.rastreoiot.datos.UsuarioRepository;
import com.rastreoiot.modelo.Activo;
import com.rastreoiot.modelo.Alerta;
import com.rastreoiot.modelo.ContactoEmergencia;
import com.rastreoiot.modelo.Dispositivo;
import com.rastreoiot.modelo.Empresa;
import com.rastreoiot.modelo.EstadoOrden;
import com.rastreoiot.modelo.Familiar;
import com.rastreoiot.modelo.Geo;
import com.rastreoiot.modelo.Geocerca;
import com.rastreoiot.modelo.LecturaTelemetria;
import com.rastreoiot.modelo.Mascota;
import com.rastreoiot.modelo.OrdenRemota;
import com.rastreoiot.modelo.Rol;
import com.rastreoiot.modelo.SubFlota;
import com.rastreoiot.modelo.TipoAlerta;
import com.rastreoiot.modelo.TipoDispositivo;
import com.rastreoiot.modelo.TipoOrden;
import com.rastreoiot.modelo.Usuario;
import com.rastreoiot.modelo.Vehiculo;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Carga datos de demostración (Bogotá) la primera vez que arranca la aplicación con la base vacía.
 * Se desactiva con la variable DATOS_DEMO=false.
 */
@Component
@Order(1)
public class DatosIniciales implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DatosIniciales.class);
    public static final String SERIAL_RUTA_DEMO = "GPS-4C1E-77A2";

    private final UsuarioRepository usuarios;
    private final EmpresaRepository empresas;
    private final DispositivoRepository dispositivos;
    private final ActivoRepository activos;
    private final GeocercaRepository geocercas;
    private final AlertaRepository alertas;
    private final SubFlotaRepository subflotas;
    private final OrdenRemotaRepository ordenes;
    private final LecturaTelemetriaRepository lecturas;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final boolean habilitados;
    private final String contrasena;
    private final ZoneId zona;

    public DatosIniciales(UsuarioRepository usuarios, EmpresaRepository empresas, DispositivoRepository dispositivos,
                          ActivoRepository activos, GeocercaRepository geocercas, AlertaRepository alertas,
                          SubFlotaRepository subflotas, OrdenRemotaRepository ordenes, LecturaTelemetriaRepository lecturas,
                          PasswordEncoder passwordEncoder, ObjectMapper objectMapper,
                          @Value("${rastreo.datos-demo.habilitados}") boolean habilitados,
                          @Value("${rastreo.datos-demo.contrasena}") String contrasena,
                          @Value("${rastreo.zona-horaria}") String zona) {
        this.usuarios = usuarios;
        this.empresas = empresas;
        this.dispositivos = dispositivos;
        this.activos = activos;
        this.geocercas = geocercas;
        this.alertas = alertas;
        this.subflotas = subflotas;
        this.ordenes = ordenes;
        this.lecturas = lecturas;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.habilitados = habilitados;
        this.contrasena = contrasena;
        this.zona = ZoneId.of(zona);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!habilitados) {
            return;
        }
        if (usuarios.count() == 0) {
            LOG.info("Base de datos vacía: cargando datos de demostración");
            cargar();
        }
        asegurarRecorridoDemo();
    }

    private void cargar() {
        Instant ahora = Instant.now();

        // Empresas y usuarios (HU-01, HU-02)
        Empresa andinos = empresas.save(new Empresa("Transportes Andinos S.A.S.", "900482113-5"));
        Empresa sabana = empresas.save(new Empresa("Logística Sabana Ltda.", "901227640-1"));

        usuario("Equipo de Soporte", "soporte@rastreoiot.co", "601 744 2010", Rol.SUPERADMIN, null);
        Usuario andres = usuario("Andrés Gómez", "andres.gomez@transportesandinos.co", "310 455 8821", Rol.ADMIN_FLOTA_B2B, andinos.getId());
        Usuario sabanaAdmin = usuario("Paola Rincón", "operaciones@logisticasabana.co", "312 600 1934", Rol.ADMIN_FLOTA_B2B, sabana.getId());
        Usuario laura = usuario("Laura Martínez", "laura.martinez@correo.co", "300 218 6645", Rol.USUARIO_HOGAR_B2C, null);
        laura.setCiudad("Bogotá");
        laura.getContactosEmergencia().add(new ContactoEmergencia("Javier Martínez", "315 882 4410", "Hermano"));
        laura.getContactosEmergencia().add(new ContactoEmergencia("Ana Ruiz", "301 552 9087", "Vecina"));
        usuarios.save(laura);

        // Activos y dispositivos de Laura (HU-04, HU-05)
        Vehiculo klm = vehiculo(laura, "KLM-482", "Chevrolet Spark GT 2021", true, 48215.0);
        Familiar carlos = new Familiar("Don Carlos Martínez", "Padre", LocalDate.of(1948, 3, 12));
        carlos.setUsuarioId(laura.getId());
        carlos = activos.save(carlos);
        Mascota luna = new Mascota("Luna", "Perro", "Golden Retriever");
        luna.setUsuarioId(laura.getId());
        luna = activos.save(luna);

        Dispositivo gpsKlm = dispositivo(SERIAL_RUTA_DEMO, TipoDispositivo.GPS, laura, klm, 86, 80.0, ahora);
        Dispositivo pulsera = dispositivo("PUL-7F3A-21C9", TipoDispositivo.PULSERA, laura, carlos, 14, null, ahora);
        Dispositivo collar = dispositivo("COL-2B8D-90E4", TipoDispositivo.COLLAR, laura, luna, 62, null,
                ahora.minus(Duration.ofMinutes(134)));
        lectura(collar, 4.67402, -74.05661, 0.0, 62, null, null, ahora.minus(Duration.ofMinutes(134)));
        lectura(pulsera, 4.64874, -74.06421, 0.0, 14, null, null, ahora.minus(Duration.ofMinutes(1)));
        lectura(gpsKlm, 4.65123, -74.07906, 32.0, 86, true, 48215.0, ahora.minus(Duration.ofMinutes(1)));

        // Geocercas (HU-10, HU-11)
        Geocerca casa = new Geocerca(pulsera.getId(), "Casa", 4.64874, -74.06421, 150);
        casa.setDispositivoDentro(true);
        geocercas.save(casa);
        geocercas.save(new Geocerca(pulsera.getId(), "Clínica del Country", 4.66788, -74.05693, 120));
        Geocerca virrey = new Geocerca(collar.getId(), "Parque El Virrey", 4.67310, -74.05530, 250);
        virrey.silenciar(90);
        geocercas.save(virrey);
        Geocerca oficina = new Geocerca(gpsKlm.getId(), "Oficina", 4.65976, -74.06910, 200);
        oficina.desactivar();
        geocercas.save(oficina);

        // Flota de Transportes Andinos (14 vehículos) y Logística Sabana
        String[][] flota = {
                {"TRX-915", "Renault Duster 2022", "GPS-9A3F-12B7", "61480", "80"},
                {"HDT-154", "Renault Kangoo 2021", "GPS-2B19-8D44", "39117", "80"},
                {"KZP-201", "Mazda 2 2022", "GPS-3C55-71E0", "27904", "80"},
                {"MXL-608", "Hino 300 2022", "GPS-4D81-09B3", "88412", "60"},
                {"JNV-628", "Chevrolet NHR 2019", "GPS-1F77-C3A0", "134902", "60"},
                {"FRD-774", "Ford Ranger 2021", "GPS-5A62-E4C8", "52330", "80"},
                {"CQT-339", "Kia Picanto 2023", "GPS-6B09-2F17", "12876", "80"},
                {"BGP-307", "Foton Aumark 2020", "GPS-6D20-4E91", null, "60"},
                {"LSK-415", "JAC X200 2021", "GPS-7E14-8A26", "70215", "60"},
                {"NTR-892", "Isuzu NPR 2021", "GPS-8F3B-5C49", "95540", "60"},
                {"WEB-120", "Chevrolet NQR 2020", "GPS-9C70-1D82", "118306", "60"},
                {"GHU-556", "JAC X350 2022", "GPS-A1D4-6E35", "43122", "60"},
                {"RVA-043", "Chevrolet N300 2023", "GPS-B2E8-7F51", "8840", "80"},
                {"PQX-918", "Renault Logan 2020", "GPS-C3F9-0A64", "77019", "80"},
        };
        Map<String, Vehiculo> porPlaca = new LinkedHashMap<>();
        Map<String, Dispositivo> gpsPorPlaca = new LinkedHashMap<>();
        for (String[] v : flota) {
            boolean sinDatos = v[3] == null;
            Vehiculo veh = vehiculo(andres, v[0], v[1], sinDatos ? null : "JNV-628".equals(v[0]) ? false : Boolean.TRUE,
                    sinDatos ? null : Double.valueOf(v[3]));
            Dispositivo d = dispositivo(v[2], TipoDispositivo.GPS, andres, veh, sinDatos ? null : 70 + (v[0].hashCode() & 0x1f) % 30,
                    Double.valueOf(v[4]), sinDatos ? null : ahora);
            if (sinDatos) {
                d.setConectado(false);
                dispositivos.save(d);
            }
            porPlaca.put(v[0], veh);
            gpsPorPlaca.put(v[0], d);
        }
        Vehiculo pqr = vehiculo(sabanaAdmin, "PQR-733", "Toyota Hilux 2022", true, 30551.0);
        dispositivo("GPS-77E1-A90C", TipoDispositivo.GPS, sabanaAdmin, pqr, 91, 80.0, ahora);

        // Geocercas de la flota: sede y centro de distribución
        geocercas.save(new Geocerca(gpsPorPlaca.get("TRX-915").getId(), "Sede Calle 100", 4.68610, -74.05480, 300));
        geocercas.save(new Geocerca(gpsPorPlaca.get("JNV-628").getId(), "Centro de distribución Calle 13", 4.62870, -74.11630, 400));

        // Sub-flotas (HU-18)
        subflota(andinos, "Sucursal Norte", "Calle 100 y Autopista Norte", porPlaca, "TRX-915", "HDT-154", "KZP-201", "MXL-608");
        subflota(andinos, "Sucursal Centro", "Centro y La Candelaria", porPlaca, "JNV-628", "FRD-774", "CQT-339");
        subflota(andinos, "Distribución Urbana", "Entregas dentro de Bogotá", porPlaca, "BGP-307", "LSK-415", "NTR-892", "WEB-120", "GHU-556");

        // Equipos del catálogo aún sin enlazar (HU-04)
        dispositivos.save(new Dispositivo("GPS-5E21-0B7C", "RIOT-GPS-5E21-0B7C", TipoDispositivo.GPS));
        dispositivos.save(new Dispositivo("PUL-91C0-3D5E", "RIOT-PUL-91C0-3D5E", TipoDispositivo.PULSERA));
        dispositivos.save(new Dispositivo("COL-6A4F-1D93", "RIOT-COL-6A4F-1D93", TipoDispositivo.COLLAR));
        dispositivos.save(new Dispositivo("GPS-8B04-6C2D", "RIOT-GPS-8B04-6C2D", TipoDispositivo.GPS));

        // Historial de alertas y órdenes (HU-12 a HU-15)
        Instant hoy = LocalDate.now(zona).atStartOfDay(zona).toInstant();
        alerta(pulsera, TipoAlerta.ENTRADA_GEOCERCA, "Don Carlos Martínez entró a la geocerca Clínica del Country", 4.66788, -74.05693, hoy.plus(Duration.ofHours(9).plusMinutes(14)).minus(1, ChronoUnit.DAYS), true);
        alerta(gpsKlm, TipoAlerta.EXCESO_VELOCIDAD, "KLM-482 a 87 km/h con límite de 80 km/h", 4.69611, -74.10402, hoy.minus(Duration.ofHours(5).plusMinutes(20)), true);
        alerta(collar, TipoAlerta.ENTRADA_GEOCERCA, "Luna entró a la geocerca Parque El Virrey", 4.67210, -74.05402, ahora.minus(Duration.ofMinutes(190)), false);
        alerta(collar, TipoAlerta.SALIDA_GEOCERCA, "Luna salió de la geocerca Parque El Virrey", 4.67402, -74.05661, ahora.minus(Duration.ofMinutes(150)), false);
        alerta(gpsKlm, TipoAlerta.EXCESO_VELOCIDAD, "KLM-482 a 94 km/h con límite de 80 km/h", 4.66997, -74.09088, ahora.minus(Duration.ofMinutes(95)), false);
        alerta(pulsera, TipoAlerta.SOS, "Botón de pánico presionado por Don Carlos Martínez", 4.64921, -74.06518, ahora.minus(Duration.ofMinutes(40)), false);
        Dispositivo hdt = dispositivos.findBySerial("GPS-2B19-8D44").orElseThrow();
        Dispositivo jnv = dispositivos.findBySerial("GPS-1F77-C3A0").orElseThrow();
        alerta(hdt, TipoAlerta.EXCESO_VELOCIDAD, "HDT-154 a 88 km/h con límite de 80 km/h", 4.75310, -74.04602, hoy.minus(Duration.ofHours(6).plusMinutes(38)), true);
        alerta(jnv, TipoAlerta.EXCESO_VELOCIDAD, "JNV-628 a 68 km/h con límite de 60 km/h", 4.59802, -74.08321, hoy.plus(Duration.ofHours(8).plusMinutes(30)), false);
        alerta(hdt, TipoAlerta.EXCESO_VELOCIDAD, "HDT-154 a 96 km/h con límite de 80 km/h", 4.65122, -74.10630, ahora.minus(Duration.ofMinutes(70)), false);

        Alerta inconsistente = new Alerta(dispositivos.findBySerial("PUL-91C0-3D5E").map(Dispositivo::getId).orElse(null),
                TipoAlerta.SOS, "SOS recibido de un dispositivo no asociado a ningún perfil (PUL-91C0-3D5E)", 4.60412, -74.06990);
        inconsistente.setSerialDispositivo("PUL-91C0-3D5E");
        inconsistente.setInconsistente(true);
        inconsistente.setFechaHora(ahora.minus(Duration.ofMinutes(33)));
        alertas.save(inconsistente);

        orden(porPlaca.get("TRX-915"), andres, TipoOrden.APAGADO, EstadoOrden.ENVIADA, "Orden de apagado transmitida al dispositivo GPS-9A3F-12B7.", ahora.minus(35, ChronoUnit.DAYS));
        orden(porPlaca.get("TRX-915"), andres, TipoOrden.BLOQUEO, EstadoOrden.ENVIADA, "Orden de bloqueo transmitida al dispositivo GPS-9A3F-12B7.", ahora.minus(22, ChronoUnit.DAYS));
        orden(porPlaca.get("BGP-307"), andres, TipoOrden.BLOQUEO, EstadoOrden.FALLIDA, "El vehículo no está accesible o no reporta un estado válido.", ahora.minus(3, ChronoUnit.DAYS));

        LOG.info("Datos de demostración cargados: {} usuarios, {} dispositivos, {} activos",
                usuarios.count(), dispositivos.count(), activos.count());
    }

    /** Garantiza que el GPS de KLM-482 tenga un recorrido completo ayer para la demostración de HU-16 y HU-17. */
    private void asegurarRecorridoDemo() {
        dispositivos.findBySerial(SERIAL_RUTA_DEMO).ifPresent(d -> {
            Instant inicio = LocalDate.now(zona).minusDays(1).atTime(LocalTime.of(7, 10)).atZone(zona).toInstant();
            boolean existe = !lecturas.findByDispositivoIdAndFechaHoraBetweenOrderByFechaHoraAsc(
                    d.getId(), inicio.minus(Duration.ofHours(1)), inicio.plus(Duration.ofHours(2))).isEmpty();
            if (existe) {
                return;
            }
            List<double[]> ruta = rutaDemo();
            List<LecturaTelemetria> puntos = new ArrayList<>();
            Instant t = inicio;
            double km = 48150;
            double[] anterior = null;
            for (double[] p : ruta) {
                double velocidad = 0;
                if (anterior != null) {
                    double metros = Geo.distanciaMetros(anterior[0], anterior[1], p[0], p[1]);
                    velocidad = Math.min(62, 24 + (metros % 30));
                    long segundos = Math.max(8, Math.round(metros / (velocidad / 3.6)));
                    t = t.plusSeconds(segundos);
                    km += metros / 1000;
                }
                LecturaTelemetria l = new LecturaTelemetria();
                l.setDispositivoId(d.getId());
                l.setLatitud(p[0]);
                l.setLongitud(p[1]);
                l.setVelocidad(Math.round(velocidad * 10) / 10.0);
                l.setNivelBateria(91);
                l.setMotorEncendido(true);
                l.setKilometraje(Math.round(km * 10) / 10.0);
                l.setFechaHora(t);
                puntos.add(l);
                anterior = p;
            }
            lecturas.saveAll(puntos);
            LOG.info("Recorrido de demostración de KLM-482 cargado ({} puntos)", puntos.size());
        });
    }

    public List<double[]> rutaDemo() {
        try (InputStream in = new ClassPathResource("demo/ruta-klm482.json").getInputStream()) {
            return List.of(objectMapper.readValue(in, double[][].class));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer la ruta de demostración", e);
        }
    }

    private Usuario usuario(String nombre, String correo, String telefono, Rol rol, String empresaId) {
        Usuario u = new Usuario(nombre, correo, passwordEncoder.encode(contrasena), telefono, rol);
        u.setEmpresaId(empresaId);
        return usuarios.save(u);
    }

    private Vehiculo vehiculo(Usuario dueno, String placa, String modelo, Boolean motor, Double km) {
        Vehiculo v = new Vehiculo(placa, modelo);
        v.setUsuarioId(dueno.getId());
        v.setMotorEncendido(motor);
        v.setKilometraje(km);
        return activos.save(v);
    }

    private Dispositivo dispositivo(String serial, TipoDispositivo tipo, Usuario dueno, Activo activo,
                                    Integer bateria, Double limite, Instant ultimaConexion) {
        Dispositivo d = new Dispositivo(serial, "RIOT-" + serial, tipo);
        d.setUsuarioId(dueno.getId());
        d.setActivoId(activo.getId());
        d.setNivelBateria(bateria);
        d.setLimiteVelocidad(limite);
        d.setUltimaConexion(ultimaConexion);
        d.setConectado(ultimaConexion != null);
        d = dispositivos.save(d);
        activo.setDispositivoId(d.getId());
        activos.save(activo);
        return d;
    }

    private void lectura(Dispositivo d, double lat, double lon, double vel, Integer bateria, Boolean motor, Double km, Instant fecha) {
        LecturaTelemetria l = new LecturaTelemetria();
        l.setDispositivoId(d.getId());
        l.setLatitud(lat);
        l.setLongitud(lon);
        l.setVelocidad(vel);
        l.setNivelBateria(bateria);
        l.setMotorEncendido(motor);
        l.setKilometraje(km);
        l.setFechaHora(fecha);
        lecturas.save(l);
    }

    private void alerta(Dispositivo d, TipoAlerta tipo, String mensaje, double lat, double lon, Instant fecha, boolean leida) {
        Alerta a = new Alerta(d.getId(), tipo, mensaje, lat, lon);
        a.setSerialDispositivo(d.getSerial());
        a.setUsuarioId(d.getUsuarioId());
        a.setFechaHora(fecha);
        a.setLeida(leida);
        alertas.save(a);
    }

    private void subflota(Empresa empresa, String nombre, String descripcion, Map<String, Vehiculo> porPlaca, String... placas) {
        SubFlota sf = new SubFlota(empresa.getId(), nombre, descripcion);
        for (String p : placas) {
            sf.getVehiculoIds().add(porPlaca.get(p).getId());
        }
        subflotas.save(sf);
    }

    private void orden(Vehiculo v, Usuario u, TipoOrden tipo, EstadoOrden estado, String detalle, Instant fecha) {
        OrdenRemota o = new OrdenRemota(v.getId(), u.getId(), u.getNombre(), tipo);
        o.setEstado(estado);
        o.setDetalle(detalle);
        o.setFechaHora(fecha);
        ordenes.save(o);
    }
}
