package com.rastreoiot.modelo;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "alertas")
public class Alerta {

    @Id
    private String id;
    @Indexed
    private String dispositivoId;
    /** Serial reportado; se conserva aunque el dispositivo no exista (SOS inconsistente, HU-13). */
    private String serialDispositivo;
    private String usuarioId;
    private TipoAlerta tipo;
    private String mensaje;
    private Double latitud;
    private Double longitud;
    private Instant fechaHora;
    private boolean leida;
    /** Evento recibido de un dispositivo inexistente o sin asociar; lo revisa el equipo de soporte. */
    private boolean inconsistente;
    /** Si el dueño quiere verla en la campana según sus preferencias (HU-03). */
    private boolean notificar = true;

    public Alerta() {
    }

    public Alerta(String dispositivoId, TipoAlerta tipo, String mensaje, Double latitud, Double longitud) {
        this.dispositivoId = dispositivoId;
        this.tipo = tipo;
        this.mensaje = mensaje;
        this.latitud = latitud;
        this.longitud = longitud;
        this.fechaHora = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDispositivoId() { return dispositivoId; }
    public void setDispositivoId(String dispositivoId) { this.dispositivoId = dispositivoId; }
    public String getSerialDispositivo() { return serialDispositivo; }
    public void setSerialDispositivo(String serialDispositivo) { this.serialDispositivo = serialDispositivo; }
    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
    public TipoAlerta getTipo() { return tipo; }
    public void setTipo(TipoAlerta tipo) { this.tipo = tipo; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }
    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }
    public Instant getFechaHora() { return fechaHora; }
    public void setFechaHora(Instant fechaHora) { this.fechaHora = fechaHora; }
    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }
    public boolean isInconsistente() { return inconsistente; }
    public void setInconsistente(boolean inconsistente) { this.inconsistente = inconsistente; }
    public boolean isNotificar() { return notificar; }
    public void setNotificar(boolean notificar) { this.notificar = notificar; }
}
