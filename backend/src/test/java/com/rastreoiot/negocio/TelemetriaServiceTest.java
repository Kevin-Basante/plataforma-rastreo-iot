package com.rastreoiot.negocio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rastreoiot.datos.ActivoRepository;
import com.rastreoiot.datos.DispositivoRepository;
import com.rastreoiot.datos.LecturaTelemetriaRepository;
import com.rastreoiot.modelo.Alerta;
import com.rastreoiot.modelo.Dispositivo;
import com.rastreoiot.modelo.LecturaTelemetria;
import com.rastreoiot.modelo.TipoAlerta;
import com.rastreoiot.modelo.TipoDispositivo;
import com.rastreoiot.negocio.dto.LecturaRequest;
import com.rastreoiot.negocio.dto.ResultadoLectura;
import com.rastreoiot.negocio.dto.SosRequest;
import com.rastreoiot.negocio.excepciones.NegocioException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Reglas de HU-13 (SOS) y HU-15 (exceso de velocidad). */
class TelemetriaServiceTest {

    private DispositivoRepository dispositivos;
    private LecturaTelemetriaRepository lecturas;
    private AlertaService alertaService;
    private TelemetriaService servicio;
    private Dispositivo gps;

    @BeforeEach
    void preparar() {
        dispositivos = mock(DispositivoRepository.class);
        lecturas = mock(LecturaTelemetriaRepository.class);
        ActivoRepository activos = mock(ActivoRepository.class);
        GeocercaService geocercas = mock(GeocercaService.class);
        alertaService = mock(AlertaService.class);
        servicio = new TelemetriaService(dispositivos, lecturas, activos, geocercas, alertaService, mock(ControlAcceso.class));

        gps = new Dispositivo("GPS-1", "RIOT-GPS-1", TipoDispositivo.GPS);
        gps.setId("d1");
        gps.setUsuarioId("u1");
        when(dispositivos.findBySerial("GPS-1")).thenReturn(Optional.of(gps));
        when(lecturas.save(any())).thenAnswer(i -> i.getArgument(0, LecturaTelemetria.class));
        when(geocercas.evaluar(any(), any())).thenReturn(List.of());
        when(alertaService.generar(any(), any(), anyString(), any(), any()))
                .thenAnswer(i -> new Alerta("d1", i.getArgument(1), i.getArgument(2), null, null));
    }

    @Test
    void velocidadDentroDelLimiteNoGeneraAlerta() {
        gps.setLimiteVelocidad(80.0);
        ResultadoLectura r = servicio.procesarLectura(lectura(79), Optional.empty());
        assertTrue(r.alertasGeneradas().isEmpty());
    }

    @Test
    void velocidadSobreElLimiteGeneraAlerta() {
        gps.setLimiteVelocidad(80.0);
        ResultadoLectura r = servicio.procesarLectura(lectura(94), Optional.empty());
        assertEquals(1, r.alertasGeneradas().size());
        assertEquals(TipoAlerta.EXCESO_VELOCIDAD, r.alertasGeneradas().get(0).getTipo());
    }

    @Test
    void sinLimiteConfiguradoIndicaConfiguracionPendiente() {
        ResultadoLectura r = servicio.procesarLectura(lectura(120), Optional.empty());
        assertTrue(r.alertasGeneradas().isEmpty());
        assertTrue(r.configuracionPendiente());
    }

    @Test
    void sosDeDispositivoInexistenteSeMarcaInconsistente() {
        when(dispositivos.findBySerial("NO-EXISTE")).thenReturn(Optional.empty());
        assertThrows(NegocioException.class,
                () -> servicio.registrarSos(new SosRequest("NO-EXISTE", null, 4.6, -74.0), Optional.empty()));
        verify(alertaService).registrarInconsistente(eq(null), eq("NO-EXISTE"), anyString(), eq(4.6), eq(-74.0));
        verify(alertaService, never()).generar(any(), any(), anyString(), any(), any());
    }

    private static LecturaRequest lectura(double velocidad) {
        return new LecturaRequest("GPS-1", null, 4.65, -74.08, velocidad, 80, true, 1000.0, null);
    }
}
