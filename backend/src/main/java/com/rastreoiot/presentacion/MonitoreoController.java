package com.rastreoiot.presentacion;

import com.rastreoiot.negocio.MonitoreoService;
import com.rastreoiot.negocio.dto.RecorridoResponse;
import com.rastreoiot.negocio.dto.UbicacionResponse;
import com.rastreoiot.seguridad.SesionActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitoreo")
@Tag(name = "Monitoreo", description = "HU-07 Posición, HU-08 Motor y kilometraje, HU-09 Vista del mapa, HU-16 Recorrido")
public class MonitoreoController {

    private final MonitoreoService monitoreoService;

    public MonitoreoController(MonitoreoService monitoreoService) {
        this.monitoreoService = monitoreoService;
    }

    @GetMapping("/ubicaciones")
    @Operation(summary = "Ubicación y estado actual de todos los dispositivos visibles")
    public List<UbicacionResponse> ubicaciones() {
        return monitoreoService.ubicacionesActuales(SesionActual.obtener());
    }

    @GetMapping("/dispositivos/{id}")
    @Operation(summary = "Ubicación y estado actual de un dispositivo")
    public UbicacionResponse ubicacion(@PathVariable String id) {
        return monitoreoService.ubicacionActual(SesionActual.obtener(), id);
    }

    @GetMapping("/dispositivos/{id}/recorrido")
    @Operation(summary = "Recorrido de un dispositivo en un rango de fechas (ISO-8601)")
    public RecorridoResponse recorrido(@PathVariable String id, @RequestParam Instant desde, @RequestParam Instant hasta) {
        return monitoreoService.consultarRecorrido(SesionActual.obtener(), id, desde, hasta);
    }

    @GetMapping("/capas")
    @Operation(summary = "Capas de mapa disponibles")
    public List<String> capas() {
        return MonitoreoService.CAPAS;
    }

    @GetMapping("/capas/{capa}")
    @Operation(summary = "Validar una capa de mapa; responde 400 si la opción no está disponible")
    public Map<String, String> validarCapa(@PathVariable String capa) {
        return Map.of("capa", monitoreoService.validarCapa(capa));
    }
}
