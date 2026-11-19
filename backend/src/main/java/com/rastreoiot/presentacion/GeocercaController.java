package com.rastreoiot.presentacion;

import com.rastreoiot.negocio.GeocercaService;
import com.rastreoiot.negocio.dto.GeocercaRequest;
import com.rastreoiot.negocio.dto.GeocercaResponse;
import com.rastreoiot.seguridad.SesionActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/geocercas")
@Tag(name = "Geocercas", description = "HU-10 Zonas de seguridad y HU-11 Activación, desactivación y silencio")
public class GeocercaController {

    private final GeocercaService geocercaService;

    public GeocercaController(GeocercaService geocercaService) {
        this.geocercaService = geocercaService;
    }

    @GetMapping
    @Operation(summary = "Listar geocercas (opcionalmente de un dispositivo)")
    public List<GeocercaResponse> listar(@RequestParam(required = false) String dispositivoId) {
        return geocercaService.listar(SesionActual.obtener(), dispositivoId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear una geocerca circular")
    public GeocercaResponse crear(@RequestBody GeocercaRequest req) {
        return geocercaService.crear(SesionActual.obtener(), req);
    }

    @PutMapping("/{id}/activar")
    @Operation(summary = "Activar la geocerca")
    public GeocercaResponse activar(@PathVariable String id) {
        return geocercaService.activar(SesionActual.obtener(), id);
    }

    @PutMapping("/{id}/desactivar")
    @Operation(summary = "Desactivar la geocerca")
    public GeocercaResponse desactivar(@PathVariable String id) {
        return geocercaService.desactivar(SesionActual.obtener(), id);
    }

    @PutMapping("/{id}/silenciar")
    @Operation(summary = "Silenciar la geocerca durante los minutos indicados")
    public GeocercaResponse silenciar(@PathVariable String id, @RequestParam Integer minutos) {
        return geocercaService.silenciar(SesionActual.obtener(), id, minutos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar la geocerca")
    public void eliminar(@PathVariable String id) {
        geocercaService.eliminar(SesionActual.obtener(), id);
    }
}
