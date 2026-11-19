package com.rastreoiot.presentacion;

import com.rastreoiot.negocio.SubFlotaService;
import com.rastreoiot.negocio.dto.SubFlotaRequest;
import com.rastreoiot.negocio.dto.SubFlotaResponse;
import com.rastreoiot.negocio.dto.VehiculoResponse;
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
@RequestMapping("/api/subflotas")
@Tag(name = "Sub-flotas", description = "HU-18 Agrupación de vehículos en sub-flotas o sucursales")
public class SubFlotaController {

    private final SubFlotaService subFlotaService;

    public SubFlotaController(SubFlotaService subFlotaService) {
        this.subFlotaService = subFlotaService;
    }

    @GetMapping
    @Operation(summary = "Listar las sub-flotas de la empresa")
    public List<SubFlotaResponse> listar(@RequestParam(required = false) String empresaId) {
        return subFlotaService.listar(SesionActual.obtener(), empresaId);
    }

    @GetMapping("/vehiculos")
    @Operation(summary = "Vehículos de la empresa con sus agrupaciones")
    public List<VehiculoResponse> vehiculos(@RequestParam(required = false) String empresaId) {
        return subFlotaService.vehiculos(SesionActual.obtener(), empresaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear una sub-flota o sucursal")
    public SubFlotaResponse crear(@RequestBody SubFlotaRequest req) {
        return subFlotaService.crear(SesionActual.obtener(), req);
    }

    @PutMapping("/{id}/vehiculos/{vehiculoId}")
    @Operation(summary = "Agregar un vehículo a la sub-flota")
    public SubFlotaResponse agregar(@PathVariable String id, @PathVariable String vehiculoId) {
        return subFlotaService.agregarVehiculo(SesionActual.obtener(), id, vehiculoId);
    }

    @DeleteMapping("/{id}/vehiculos/{vehiculoId}")
    @Operation(summary = "Quitar un vehículo de la sub-flota")
    public SubFlotaResponse quitar(@PathVariable String id, @PathVariable String vehiculoId) {
        return subFlotaService.quitarVehiculo(SesionActual.obtener(), id, vehiculoId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar la sub-flota")
    public void eliminar(@PathVariable String id) {
        subFlotaService.eliminar(SesionActual.obtener(), id);
    }
}
