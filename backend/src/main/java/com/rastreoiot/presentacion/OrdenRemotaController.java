package com.rastreoiot.presentacion;

import com.rastreoiot.modelo.OrdenRemota;
import com.rastreoiot.negocio.OrdenRemotaService;
import com.rastreoiot.negocio.dto.OrdenRequest;
import com.rastreoiot.seguridad.SesionActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ordenes")
@Tag(name = "Órdenes remotas", description = "HU-14 Apagado o bloqueo remoto del motor")
public class OrdenRemotaController {

    private final OrdenRemotaService ordenService;

    public OrdenRemotaController(OrdenRemotaService ordenService) {
        this.ordenService = ordenService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Enviar una orden de apagado o bloqueo al vehículo")
    public OrdenRemota enviar(@Valid @RequestBody OrdenRequest req) {
        return ordenService.enviarOrden(SesionActual.obtener(), req);
    }

    @GetMapping("/vehiculos/{vehiculoId}")
    @Operation(summary = "Historial de órdenes de un vehículo")
    public List<OrdenRemota> historial(@PathVariable String vehiculoId) {
        return ordenService.historial(SesionActual.obtener(), vehiculoId);
    }
}
