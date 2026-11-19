package com.rastreoiot.presentacion;

import com.rastreoiot.modelo.TipoAlerta;
import com.rastreoiot.negocio.AlertaService;
import com.rastreoiot.negocio.dto.AlertaResponse;
import com.rastreoiot.seguridad.SesionActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alertas")
@Tag(name = "Alertas", description = "HU-12 Alertas de geocerca, HU-13 SOS y HU-15 Exceso de velocidad")
public class AlertaController {

    private final AlertaService alertaService;

    public AlertaController(AlertaService alertaService) {
        this.alertaService = alertaService;
    }

    @GetMapping
    @Operation(summary = "Listar alertas visibles, de la más reciente a la más antigua")
    public List<AlertaResponse> listar(@RequestParam(required = false) TipoAlerta tipo,
                                       @RequestParam(required = false) String dispositivoId,
                                       @RequestParam(required = false) Boolean soloNoLeidas) {
        return alertaService.listar(SesionActual.obtener(), tipo, dispositivoId, soloNoLeidas);
    }

    @GetMapping("/no-leidas")
    @Operation(summary = "Cantidad de alertas sin leer (campana)")
    public Map<String, Long> noLeidas() {
        return Map.of("cantidad", alertaService.contarNoLeidas(SesionActual.obtener()));
    }

    @PutMapping("/{id}/leida")
    @Operation(summary = "Marcar una alerta como leída")
    public AlertaResponse marcarLeida(@PathVariable String id) {
        return alertaService.marcarLeida(SesionActual.obtener(), id);
    }

    @PutMapping("/leidas")
    @Operation(summary = "Marcar todas las alertas como leídas")
    public Map<String, Integer> marcarTodas() {
        return Map.of("actualizadas", alertaService.marcarTodasLeidas(SesionActual.obtener()));
    }
}
