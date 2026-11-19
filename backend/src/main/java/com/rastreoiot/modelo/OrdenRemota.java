package com.rastreoiot.modelo;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Orden de apagado o bloqueo remoto del motor (HU-14). */
@Document(collection = "ordenes")
public class OrdenRemota {

    @Id
    private String id;
    private String vehiculoId;
    private String usuarioId;
    private String nombreUsuario;
    private TipoOrden tipo;
    private EstadoOrden estado;
    private String detalle;
    private Instant fechaHora;

    public OrdenRemota() {
    }

    public OrdenRemota(String vehiculoId, String usuarioId, String nombreUsuario, TipoOrden tipo) {
        this.vehiculoId = vehiculoId;
        this.usuarioId = usuarioId;
        this.nombreUsuario = nombreUsuario;
        this.tipo = tipo;
        this.fechaHora = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getVehiculoId() { return vehiculoId; }
    public void setVehiculoId(String vehiculoId) { this.vehiculoId = vehiculoId; }
    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
    public TipoOrden getTipo() { return tipo; }
    public void setTipo(TipoOrden tipo) { this.tipo = tipo; }
    public EstadoOrden getEstado() { return estado; }
    public void setEstado(EstadoOrden estado) { this.estado = estado; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
    public Instant getFechaHora() { return fechaHora; }
    public void setFechaHora(Instant fechaHora) { this.fechaHora = fechaHora; }
}
