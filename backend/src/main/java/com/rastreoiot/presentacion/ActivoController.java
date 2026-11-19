package com.rastreoiot.presentacion;

import com.rastreoiot.modelo.Activo;
import com.rastreoiot.negocio.ActivoService;
import com.rastreoiot.negocio.dto.ActivoRequest;
import com.rastreoiot.seguridad.SesionActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activos")
@Tag(name = "Activos", description = "HU-05 Registro de vehículos, familiares y mascotas")
public class ActivoController {

    private final ActivoService activoService;

    public ActivoController(ActivoService activoService) {
        this.activoService = activoService;
    }

    @GetMapping
    @Operation(summary = "Listar los activos monitoreados visibles")
    public List<Activo> listar() {
        return activoService.listar(SesionActual.obtener());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un activo")
    public Activo obtener(@PathVariable String id) {
        return activoService.obtener(SesionActual.obtener(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar un activo: tipo VEHICULO (placa, modelo), FAMILIAR (nombre, parentesco) o MASCOTA (nombre, especie)")
    public Activo registrar(@RequestBody ActivoRequest req) {
        return activoService.registrar(SesionActual.obtener(), req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar un activo")
    public void eliminar(@PathVariable String id) {
        activoService.eliminar(SesionActual.obtener(), id);
    }
}
