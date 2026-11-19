package com.rastreoiot.negocio;

import com.rastreoiot.datos.ActivoRepository;
import com.rastreoiot.modelo.Activo;
import com.rastreoiot.modelo.Familiar;
import com.rastreoiot.modelo.Mascota;
import com.rastreoiot.modelo.Vehiculo;
import com.rastreoiot.negocio.dto.ActivoRequest;
import com.rastreoiot.negocio.excepciones.RecursoNoEncontradoException;
import com.rastreoiot.negocio.excepciones.ValidacionException;
import com.rastreoiot.seguridad.SesionUsuario;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** Registro de vehículos, familiares y mascotas monitoreados (HU-05). */
@Service
public class ActivoService {

    private final ActivoRepository activoRepository;
    private final DispositivoService dispositivoService;
    private final ControlAcceso controlAcceso;

    public ActivoService(ActivoRepository activoRepository, DispositivoService dispositivoService, ControlAcceso controlAcceso) {
        this.activoRepository = activoRepository;
        this.dispositivoService = dispositivoService;
        this.controlAcceso = controlAcceso;
    }

    public List<Activo> listar(SesionUsuario sesion) {
        List<Activo> activos = controlAcceso.usuariosVisibles(sesion)
                .map(activoRepository::findByUsuarioIdIn)
                .orElseGet(activoRepository::findAll);
        return activos.stream()
                .sorted(Comparator.comparing(Activo::getFechaRegistro, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public Activo obtener(SesionUsuario sesion, String id) {
        Activo activo = activoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El activo no existe."));
        controlAcceso.verificarActivo(sesion, activo);
        return activo;
    }

    public Activo registrar(SesionUsuario sesion, ActivoRequest req) {
        String tipo = req.tipo() == null ? "" : req.tipo().trim().toUpperCase(Locale.ROOT);
        Activo activo = switch (tipo) {
            case "VEHICULO" -> crearVehiculo(req);
            case "FAMILIAR" -> crearFamiliar(req);
            case "MASCOTA" -> crearMascota(req);
            default -> throw new ValidacionException("Faltan datos obligatorios: selecciona el tipo de activo (vehículo, familiar o mascota).",
                    List.of("tipo"));
        };
        activo.setUsuarioId(sesion.id());
        activo = activoRepository.save(activo);
        if (req.dispositivoId() != null && !req.dispositivoId().isBlank()) {
            dispositivoService.asociarActivo(sesion, req.dispositivoId(), activo.getId());
            activo = activoRepository.findById(activo.getId()).orElseThrow();
        }
        return activo;
    }

    public void eliminar(SesionUsuario sesion, String id) {
        Activo activo = obtener(sesion, id);
        if (activo.getDispositivoId() != null) {
            dispositivoService.asociarActivo(sesion, activo.getDispositivoId(), null);
        }
        activoRepository.deleteById(id);
    }

    private Vehiculo crearVehiculo(ActivoRequest req) {
        List<String> faltantes = new ArrayList<>();
        if (vacio(req.placa())) faltantes.add("placa");
        if (vacio(req.modelo())) faltantes.add("modelo");
        exigir(faltantes);
        String placa = req.placa().trim().toUpperCase(Locale.ROOT);
        if (!placa.matches("^[A-Z]{3}-?\\d{2}[A-Z0-9]$")) {
            throw new ValidacionException("La placa debe tener el formato colombiano, por ejemplo KLM-482.", List.of("placa"));
        }
        if (placa.length() == 6) {
            placa = placa.substring(0, 3) + "-" + placa.substring(3);
        }
        return new Vehiculo(placa, req.modelo().trim());
    }

    private Familiar crearFamiliar(ActivoRequest req) {
        List<String> faltantes = new ArrayList<>();
        if (vacio(req.nombre())) faltantes.add("nombre");
        if (vacio(req.parentesco())) faltantes.add("parentesco");
        exigir(faltantes);
        if (req.fechaNacimiento() != null && req.fechaNacimiento().isAfter(LocalDate.now())) {
            throw new ValidacionException("La fecha de nacimiento no puede ser futura.", List.of("fechaNacimiento"));
        }
        return new Familiar(req.nombre().trim(), req.parentesco().trim(), req.fechaNacimiento());
    }

    private Mascota crearMascota(ActivoRequest req) {
        List<String> faltantes = new ArrayList<>();
        if (vacio(req.nombre())) faltantes.add("nombre");
        if (vacio(req.especie())) faltantes.add("especie");
        exigir(faltantes);
        return new Mascota(req.nombre().trim(), req.especie().trim(), vacio(req.raza()) ? null : req.raza().trim());
    }

    private static void exigir(List<String> faltantes) {
        if (!faltantes.isEmpty()) {
            throw new ValidacionException("Faltan datos obligatorios: " + String.join(", ", faltantes) + ".", faltantes);
        }
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
