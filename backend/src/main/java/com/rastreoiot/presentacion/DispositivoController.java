package com.rastreoiot.presentacion;

import com.rastreoiot.negocio.DispositivoService;
import com.rastreoiot.negocio.dto.AsociarActivoRequest;
import com.rastreoiot.negocio.dto.DispositivoNuevoRequest;
import com.rastreoiot.negocio.dto.DispositivoResponse;
import com.rastreoiot.negocio.dto.EnlaceRequest;
import com.rastreoiot.negocio.dto.LimiteVelocidadRequest;
import com.rastreoiot.seguridad.SesionActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dispositivos")
@Tag(name = "Dispositivos", description = "HU-04 Enlace, HU-06 Estado y batería, HU-15 Límite de velocidad")
public class DispositivoController {

    private final DispositivoService dispositivoService;

    public DispositivoController(DispositivoService dispositivoService) {
        this.dispositivoService = dispositivoService;
    }

    @GetMapping
    @Operation(summary = "Listar los dispositivos visibles con su estado de conexión y batería")
    public List<DispositivoResponse> listar() {
        return dispositivoService.listar(SesionActual.obtener());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar el estado de un dispositivo")
    public DispositivoResponse estado(@PathVariable String id) {
        return dispositivoService.estado(SesionActual.obtener(), id);
    }

    @PostMapping("/enlazar")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Enlazar un dispositivo al perfil por código QR o serial")
    public DispositivoResponse enlazar(@RequestBody EnlaceRequest req) {
        return dispositivoService.enlazar(SesionActual.obtener(), req);
    }

    @PutMapping("/{id}/activo")
    @Operation(summary = "Asociar o quitar el activo monitoreado del dispositivo")
    public DispositivoResponse asociarActivo(@PathVariable String id, @RequestBody AsociarActivoRequest req) {
        return dispositivoService.asociarActivo(SesionActual.obtener(), id, req.activoId());
    }

    @PutMapping("/{id}/limite-velocidad")
    @Operation(summary = "Configurar el límite de velocidad (km/h); null lo deja pendiente")
    public DispositivoResponse configurarLimite(@PathVariable String id, @RequestBody LimiteVelocidadRequest req) {
        return dispositivoService.configurarLimite(SesionActual.obtener(), id, req.limiteVelocidad());
    }

    @DeleteMapping("/{id}/enlace")
    @Operation(summary = "Desenlazar el dispositivo del perfil")
    public DispositivoResponse desenlazar(@PathVariable String id) {
        return dispositivoService.desenlazar(SesionActual.obtener(), id);
    }

    @PostMapping("/catalogo")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar un equipo en el catálogo (solo SuperAdmin)")
    public DispositivoResponse registrar(@Valid @RequestBody DispositivoNuevoRequest req) {
        return dispositivoService.registrarEnCatalogo(SesionActual.obtener(), req);
    }
}
