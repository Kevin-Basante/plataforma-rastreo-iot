package com.rastreoiot.modelo;

import java.time.Duration;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "dispositivos")
public class Dispositivo {

    /** Tiempo sin datos tras el cual el dispositivo se considera desconectado (HU-06). */
    public static final Duration TIEMPO_SIN_SENAL = Duration.ofMinutes(5);
    /** Porcentaje desde el cual la batería se considera crítica (HU-06). */
    public static final int BATERIA_CRITICA = 20;

    @Id
    private String id;
    @Indexed(unique = true)
    private String serial;
    @Indexed(unique = true)
    private String codigoQR;
    private TipoDispositivo tipo;
    private boolean conectado;
    private Integer nivelBateria;
    private Instant ultimaConexion;
    /** Velocidad máxima permitida en km/h; null significa configuración pendiente (HU-15). */
    private Double limiteVelocidad;
    private String usuarioId;
    private String activoId;

    public Dispositivo() {
    }

    public Dispositivo(String serial, String codigoQR, TipoDispositivo tipo) {
        this.serial = serial;
        this.codigoQR = codigoQR;
        this.tipo = tipo;
    }

    /** El dispositivo está conectado si reportó datos dentro del periodo relevante. */
    public boolean estaConectado(Instant ahora) {
        return conectado && ultimaConexion != null
                && Duration.between(ultimaConexion, ahora).compareTo(TIEMPO_SIN_SENAL) <= 0;
    }

    public boolean tieneBateriaBaja() {
        return nivelBateria != null && nivelBateria <= BATERIA_CRITICA;
    }

    public boolean estaEnlazado() {
        return usuarioId != null;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSerial() { return serial; }
    public void setSerial(String serial) { this.serial = serial; }
    public String getCodigoQR() { return codigoQR; }
    public void setCodigoQR(String codigoQR) { this.codigoQR = codigoQR; }
    public TipoDispositivo getTipo() { return tipo; }
    public void setTipo(TipoDispositivo tipo) { this.tipo = tipo; }
    public boolean isConectado() { return conectado; }
    public void setConectado(boolean conectado) { this.conectado = conectado; }
    public Integer getNivelBateria() { return nivelBateria; }
    public void setNivelBateria(Integer nivelBateria) { this.nivelBateria = nivelBateria; }
    public Instant getUltimaConexion() { return ultimaConexion; }
    public void setUltimaConexion(Instant ultimaConexion) { this.ultimaConexion = ultimaConexion; }
    public Double getLimiteVelocidad() { return limiteVelocidad; }
    public void setLimiteVelocidad(Double limiteVelocidad) { this.limiteVelocidad = limiteVelocidad; }
    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
    public String getActivoId() { return activoId; }
    public void setActivoId(String activoId) { this.activoId = activoId; }
}
