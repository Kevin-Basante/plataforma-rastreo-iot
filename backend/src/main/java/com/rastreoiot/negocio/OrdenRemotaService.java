package com.rastreoiot.negocio;

import com.rastreoiot.datos.ActivoRepository;
import com.rastreoiot.datos.DispositivoRepository;
import com.rastreoiot.datos.OrdenRemotaRepository;
import com.rastreoiot.modelo.Activo;
import com.rastreoiot.modelo.Dispositivo;
import com.rastreoiot.modelo.EstadoOrden;
import com.rastreoiot.modelo.OrdenRemota;
import com.rastreoiot.modelo.TipoOrden;
import com.rastreoiot.modelo.Vehiculo;
import com.rastreoiot.negocio.dto.OrdenRequest;
import com.rastreoiot.negocio.excepciones.NegocioException;
import com.rastreoiot.negocio.excepciones.PermisosInsuficientesException;
import com.rastreoiot.negocio.excepciones.RecursoNoEncontradoException;
import com.rastreoiot.seguridad.SesionUsuario;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Apagado o bloqueo remoto del motor (HU-14). */
@Service
public class OrdenRemotaService {

    private final OrdenRemotaRepository ordenRepository;
    private final ActivoRepository activoRepository;
    private final DispositivoRepository dispositivoRepository;
    private final ControlAcceso controlAcceso;

    public OrdenRemotaService(OrdenRemotaRepository ordenRepository, ActivoRepository activoRepository,
                              DispositivoRepository dispositivoRepository, ControlAcceso controlAcceso) {
        this.ordenRepository = ordenRepository;
        this.activoRepository = activoRepository;
        this.dispositivoRepository = dispositivoRepository;
        this.controlAcceso = controlAcceso;
    }

    /**
     * Procesa la orden y la transmite al dispositivo del vehículo. Si el vehículo no tiene un dispositivo
     * conectado con estado válido, la orden se registra como FALLIDA y se informa al usuario.
     */
    public OrdenRemota enviarOrden(SesionUsuario sesion, OrdenRequest req) {
        Vehiculo vehiculo = buscarVehiculo(req.vehiculoId());
        if (!controlAcceso.puedeVerRecursoDe(sesion, vehiculo.getUsuarioId())) {
            throw new PermisosInsuficientesException("Permisos insuficientes: no estás autorizado para enviar órdenes remotas a "
                    + vehiculo.getPlaca() + ". La orden no se envió.");
        }
        OrdenRemota orden = new OrdenRemota(vehiculo.getId(), sesion.id(), sesion.nombre(), req.tipo());
        Dispositivo dispositivo = vehiculo.getDispositivoId() == null ? null
                : dispositivoRepository.findById(vehiculo.getDispositivoId()).orElse(null);
        String accion = req.tipo() == TipoOrden.APAGADO ? "apagado" : "bloqueo";

        if (dispositivo == null || !dispositivo.estaConectado(Instant.now()) || vehiculo.getMotorEncendido() == null) {
            orden.setEstado(EstadoOrden.FALLIDA);
            orden.setDetalle(dispositivo == null
                    ? "El vehículo no tiene un dispositivo GPS asociado."
                    : "El vehículo no está accesible o no reporta un estado válido.");
            ordenRepository.save(orden);
            throw new NegocioException(HttpStatus.CONFLICT, "La orden de " + accion + " no pudo completarse: "
                    + orden.getDetalle().substring(0, 1).toLowerCase() + orden.getDetalle().substring(1)
                    + " El fallo quedó registrado en el historial.");
        }
        orden.setEstado(EstadoOrden.ENVIADA);
        orden.setDetalle("Orden de " + accion + " transmitida al dispositivo " + dispositivo.getSerial() + ".");
        vehiculo.setMotorEncendido(false);
        activoRepository.save(vehiculo);
        return ordenRepository.save(orden);
    }

    public List<OrdenRemota> historial(SesionUsuario sesion, String vehiculoId) {
        Vehiculo vehiculo = buscarVehiculo(vehiculoId);
        controlAcceso.verificarActivo(sesion, vehiculo);
        return ordenRepository.findByVehiculoIdOrderByFechaHoraDesc(vehiculoId);
    }

    private Vehiculo buscarVehiculo(String id) {
        Activo activo = activoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El vehículo no existe."));
        if (!(activo instanceof Vehiculo vehiculo)) {
            throw new RecursoNoEncontradoException("El activo seleccionado no es un vehículo.");
        }
        return vehiculo;
    }
}
