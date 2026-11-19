package com.rastreoiot.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModeloTest {

    @Test
    void geocercaContienePuntosDentroDelRadio() {
        Geocerca casa = new Geocerca("d1", "Casa", 4.64874, -74.06421, 150);
        assertTrue(casa.contiene(4.64874, -74.06421));
        assertTrue(casa.contiene(4.64950, -74.06421));   // ~85 m al norte
        assertFalse(casa.contiene(4.65100, -74.06421));  // ~250 m al norte
    }

    @Test
    void silencioTemporalSeReanudaAlTerminar() {
        Geocerca g = new Geocerca("d1", "Parque", 4.67, -74.05, 200);
        g.silenciar(10);
        assertEquals(EstadoGeocerca.SILENCIADA, g.getEstado());
        assertFalse(g.reanudarSiTerminoSilencio(Instant.now()));
        assertTrue(g.reanudarSiTerminoSilencio(Instant.now().plus(Duration.ofMinutes(11))));
        assertEquals(EstadoGeocerca.ACTIVA, g.getEstado());
    }

    @Test
    void dispositivoSinDatosRecientesQuedaDesconectado() {
        Dispositivo d = new Dispositivo("GPS-1", "QR-1", TipoDispositivo.GPS);
        d.setConectado(true);
        Instant ahora = Instant.now();
        d.setUltimaConexion(ahora.minus(Duration.ofMinutes(2)));
        assertTrue(d.estaConectado(ahora));
        d.setUltimaConexion(ahora.minus(Duration.ofMinutes(6)));
        assertFalse(d.estaConectado(ahora));
    }

    @Test
    void bateriaCriticaSeMarcaComoBaja() {
        Dispositivo d = new Dispositivo("PUL-1", "QR-2", TipoDispositivo.PULSERA);
        d.setNivelBateria(14);
        assertTrue(d.tieneBateriaBaja());
        d.setNivelBateria(60);
        assertFalse(d.tieneBateriaBaja());
    }

    @Test
    void recorridoSumaLaDistanciaEntrePuntos() {
        LecturaTelemetria a = lectura(4.6000, -74.0800);
        LecturaTelemetria b = lectura(4.6090, -74.0800); // ~1 km al norte
        Recorrido r = new Recorrido("d1", Instant.now(), Instant.now(), List.of(a, b));
        assertEquals(1.0, r.getDistanciaKm(), 0.02);
        assertTrue(r.tieneDatos());
    }

    @Test
    void usuarioNuevoTieneTodasLasPreferenciasActivas() {
        Usuario u = new Usuario("Laura", "laura@correo.co", "hash", "3000000000", Rol.USUARIO_HOGAR_B2C);
        assertEquals(TipoAlerta.values().length, u.getPreferencias().size());
        assertTrue(u.quiereNotificacion(TipoAlerta.SOS));
    }

    private static LecturaTelemetria lectura(double lat, double lon) {
        LecturaTelemetria l = new LecturaTelemetria();
        l.setLatitud(lat);
        l.setLongitud(lon);
        return l;
    }
}
