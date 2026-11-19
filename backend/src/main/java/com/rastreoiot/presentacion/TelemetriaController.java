package com.rastreoiot.presentacion;

import com.rastreoiot.modelo.Alerta;
import com.rastreoiot.negocio.TelemetriaService;
import com.rastreoiot.negocio.dto.LecturaRequest;
import com.rastreoiot.negocio.dto.ResultadoLectura;
import com.rastreoiot.negocio.dto.SosRequest;
import com.rastreoiot.negocio.excepciones.NegocioException;
import com.rastreoiot.seguridad.SesionActual;
import com.rastreoiot.seguridad.SesionUsuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Entrada de datos de los dispositivos. Acepta la clave de dispositivos (cabecera X-Clave-Dispositivo)
 * o un usuario autenticado con acceso al dispositivo (simulador de la aplicación web).
 */
@RestController
@RequestMapping("/api/telemetria")
@Tag(name = "Telemetría", description = "HU-07/HU-08 lecturas, HU-12 geocercas, HU-13 SOS y HU-15 velocidad")
public class TelemetriaController {

    private final TelemetriaService telemetriaService;
    private final String claveDispositivos;

    public TelemetriaController(TelemetriaService telemetriaService,
                                @Value("${rastreo.dispositivos.clave}") String claveDispositivos) {
        this.telemetriaService = telemetriaService;
        this.claveDispositivos = claveDispositivos;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Recibir una lectura de telemetría")
    public ResultadoLectura recibir(@RequestHeader(value = "X-Clave-Dispositivo", required = false) String clave,
                                    @RequestBody LecturaRequest req) {
        return telemetriaService.procesarLectura(req, autorizar(clave));
    }

    @PostMapping("/sos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Recibir la señal del botón de pánico (SOS)")
    public Alerta sos(@RequestHeader(value = "X-Clave-Dispositivo", required = false) String clave,
                      @RequestBody SosRequest req) {
        return telemetriaService.registrarSos(req, autorizar(clave));
    }

    /** Devuelve vacío si llamó un dispositivo con clave válida, o la sesión del usuario autenticado. */
    private Optional<SesionUsuario> autorizar(String clave) {
        if (clave != null && MessageDigest.isEqual(clave.getBytes(StandardCharsets.UTF_8),
                claveDispositivos.getBytes(StandardCharsets.UTF_8))) {
            return Optional.empty();
        }
        return Optional.of(SesionActual.opcional().orElseThrow(() -> new NegocioException(HttpStatus.UNAUTHORIZED,
                "Credencial de dispositivo inválida o sesión no iniciada.")));
    }
}
