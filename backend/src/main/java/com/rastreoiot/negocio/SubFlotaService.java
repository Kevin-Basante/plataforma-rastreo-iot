package com.rastreoiot.negocio;

import com.rastreoiot.datos.ActivoRepository;
import com.rastreoiot.datos.DispositivoRepository;
import com.rastreoiot.datos.EmpresaRepository;
import com.rastreoiot.datos.SubFlotaRepository;
import com.rastreoiot.datos.UsuarioRepository;
import com.rastreoiot.modelo.Activo;
import com.rastreoiot.modelo.Dispositivo;
import com.rastreoiot.modelo.SubFlota;
import com.rastreoiot.modelo.Usuario;
import com.rastreoiot.modelo.Vehiculo;
import com.rastreoiot.negocio.dto.SubFlotaRequest;
import com.rastreoiot.negocio.dto.SubFlotaResponse;
import com.rastreoiot.negocio.dto.VehiculoResponse;
import com.rastreoiot.negocio.excepciones.PermisosInsuficientesException;
import com.rastreoiot.negocio.excepciones.RecursoNoEncontradoException;
import com.rastreoiot.negocio.excepciones.ValidacionException;
import com.rastreoiot.seguridad.SesionUsuario;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Agrupación de vehículos corporativos en sub-flotas o sucursales (HU-18). */
@Service
public class SubFlotaService {

    private final SubFlotaRepository subFlotaRepository;
    private final ActivoRepository activoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final DispositivoRepository dispositivoRepository;

    public SubFlotaService(SubFlotaRepository subFlotaRepository, ActivoRepository activoRepository,
                           UsuarioRepository usuarioRepository, EmpresaRepository empresaRepository,
                           DispositivoRepository dispositivoRepository) {
        this.subFlotaRepository = subFlotaRepository;
        this.activoRepository = activoRepository;
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.dispositivoRepository = dispositivoRepository;
    }

    public List<SubFlotaResponse> listar(SesionUsuario sesion, String empresaId) {
        String empresa = empresaDeTrabajo(sesion, empresaId);
        List<SubFlota> subflotas = empresa == null ? subFlotaRepository.findAll() : subFlotaRepository.findByEmpresaId(empresa);
        Map<String, Vehiculo> vehiculos = vehiculosPorId(empresa);
        return subflotas.stream().sorted(Comparator.comparing(SubFlota::getNombre)).map(sf -> respuesta(sf, vehiculos)).toList();
    }

    /** Vehículos de la empresa con las agrupaciones a las que pertenecen. */
    public List<VehiculoResponse> vehiculos(SesionUsuario sesion, String empresaId) {
        String empresa = empresaDeTrabajo(sesion, empresaId);
        List<SubFlota> subflotas = empresa == null ? subFlotaRepository.findAll() : subFlotaRepository.findByEmpresaId(empresa);
        return vehiculosPorId(empresa).values().stream()
                .sorted(Comparator.comparing(Vehiculo::getPlaca))
                .map(v -> vehiculo(v, subflotas))
                .toList();
    }

    public SubFlotaResponse crear(SesionUsuario sesion, SubFlotaRequest req) {
        List<String> errores = new ArrayList<>();
        if (req.nombre() == null || req.nombre().isBlank()) {
            errores.add("el nombre de la sub-flota es obligatorio");
        }
        String empresa = sesion.esSuperAdmin() ? req.empresaId() : sesion.empresaId();
        if (sesion.esUsuarioHogar()) {
            throw new PermisosInsuficientesException("Permisos insuficientes: las sub-flotas son exclusivas de clientes corporativos.");
        }
        if (empresa == null || !empresaRepository.existsById(empresa)) {
            errores.add("la sub-flota debe pertenecer a una empresa válida (alcance)");
        }
        if (!errores.isEmpty()) {
            throw new ValidacionException("No se pudo crear la agrupación: " + String.join("; ", errores) + ".", errores);
        }
        String nombre = req.nombre().trim();
        boolean repetida = subFlotaRepository.findByEmpresaId(empresa).stream()
                .anyMatch(sf -> sf.getNombre().equalsIgnoreCase(nombre));
        if (repetida) {
            throw new ValidacionException("Ya existe una sub-flota llamada \"" + nombre + "\" en la empresa.");
        }
        SubFlota subFlota = new SubFlota(empresa, nombre, req.descripcion() == null ? null : req.descripcion().trim());
        Map<String, Vehiculo> vehiculos = vehiculosPorId(empresa);
        if (req.vehiculoIds() != null) {
            for (String id : req.vehiculoIds()) {
                if (!vehiculos.containsKey(id)) {
                    throw new ValidacionException("Uno de los vehículos seleccionados no pertenece a la empresa.");
                }
                subFlota.getVehiculoIds().add(id);
            }
        }
        return respuesta(subFlotaRepository.save(subFlota), vehiculos);
    }

    public SubFlotaResponse agregarVehiculo(SesionUsuario sesion, String subFlotaId, String vehiculoId) {
        SubFlota sf = buscar(sesion, subFlotaId);
        Map<String, Vehiculo> vehiculos = vehiculosPorId(sf.getEmpresaId());
        if (!vehiculos.containsKey(vehiculoId)) {
            throw new ValidacionException("El vehículo no pertenece a la empresa de esta sub-flota.");
        }
        sf.getVehiculoIds().add(vehiculoId);
        return respuesta(subFlotaRepository.save(sf), vehiculos);
    }

    public SubFlotaResponse quitarVehiculo(SesionUsuario sesion, String subFlotaId, String vehiculoId) {
        SubFlota sf = buscar(sesion, subFlotaId);
        sf.getVehiculoIds().remove(vehiculoId);
        return respuesta(subFlotaRepository.save(sf), vehiculosPorId(sf.getEmpresaId()));
    }

    public void eliminar(SesionUsuario sesion, String subFlotaId) {
        subFlotaRepository.delete(buscar(sesion, subFlotaId));
    }

    private SubFlota buscar(SesionUsuario sesion, String id) {
        SubFlota sf = subFlotaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La sub-flota no existe."));
        if (!sesion.esSuperAdmin() && !Objects.equals(sf.getEmpresaId(), sesion.empresaId())) {
            throw new PermisosInsuficientesException("Permisos insuficientes: la sub-flota pertenece a otra empresa.");
        }
        return sf;
    }

    /** Empresa sobre la que trabaja la sesión; null significa todas (SuperAdmin sin filtro). */
    private String empresaDeTrabajo(SesionUsuario sesion, String empresaId) {
        if (sesion.esSuperAdmin()) {
            return empresaId == null || empresaId.isBlank() ? null : empresaId;
        }
        if (sesion.esAdminFlota() && sesion.empresaId() != null) {
            return sesion.empresaId();
        }
        throw new PermisosInsuficientesException("Permisos insuficientes: las sub-flotas son exclusivas de clientes corporativos.");
    }

    private Map<String, Vehiculo> vehiculosPorId(String empresaId) {
        List<String> usuarios = (empresaId == null
                ? usuarioRepository.findAll().stream().filter(u -> u.getEmpresaId() != null)
                : usuarioRepository.findByEmpresaId(empresaId).stream())
                .map(Usuario::getId).toList();
        return activoRepository.findByUsuarioIdIn(usuarios).stream()
                .filter(Vehiculo.class::isInstance).map(Vehiculo.class::cast)
                .collect(Collectors.toMap(Activo::getId, v -> v));
    }

    private SubFlotaResponse respuesta(SubFlota sf, Map<String, Vehiculo> vehiculos) {
        List<SubFlota> todas = List.of(sf);
        List<VehiculoResponse> lista = sf.getVehiculoIds().stream()
                .map(vehiculos::get).filter(Objects::nonNull)
                .map(v -> vehiculo(v, todas))
                .toList();
        return new SubFlotaResponse(sf.getId(), sf.getEmpresaId(), sf.getNombre(), sf.getDescripcion(), lista);
    }

    private VehiculoResponse vehiculo(Vehiculo v, List<SubFlota> subflotas) {
        Dispositivo d = v.getDispositivoId() == null ? null : dispositivoRepository.findById(v.getDispositivoId()).orElse(null);
        List<String> grupos = subflotas.stream().filter(sf -> sf.getVehiculoIds().contains(v.getId())).map(SubFlota::getNombre).toList();
        return new VehiculoResponse(v.getId(), v.getPlaca(), v.getModelo(), v.getMotorEncendido(), v.getKilometraje(),
                v.getDispositivoId(), d == null ? null : d.getSerial(), d != null && d.estaConectado(Instant.now()),
                v.getUsuarioId(), grupos);
    }
}
