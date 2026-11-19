package com.rastreoiot.modelo;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "lecturas")
@CompoundIndex(name = "dispositivo_fecha", def = "{'dispositivoId': 1, 'fechaHora': -1}")
public class LecturaTelemetria {

    @Id
    private String id;
    private String dispositivoId;
    private Double latitud;
    private Double longitud;
    private Double velocidad;
    private Integer nivelBateria;
    private Boolean motorEncendido;
    private Double kilometraje;
    private Instant fechaHora;

    public LecturaTelemetria() {
    }

    public boolean tieneUbicacion() {
        return latitud != null && longitud != null;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDispositivoId() { return dispositivoId; }
    public void setDispositivoId(String dispositivoId) { this.dispositivoId = dispositivoId; }
    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }
    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }
    public Double getVelocidad() { return velocidad; }
    public void setVelocidad(Double velocidad) { this.velocidad = velocidad; }
    public Integer getNivelBateria() { return nivelBateria; }
    public void setNivelBateria(Integer nivelBateria) { this.nivelBateria = nivelBateria; }
    public Boolean getMotorEncendido() { return motorEncendido; }
    public void setMotorEncendido(Boolean motorEncendido) { this.motorEncendido = motorEncendido; }
    public Double getKilometraje() { return kilometraje; }
    public void setKilometraje(Double kilometraje) { this.kilometraje = kilometraje; }
    public Instant getFechaHora() { return fechaHora; }
    public void setFechaHora(Instant fechaHora) { this.fechaHora = fechaHora; }
}
