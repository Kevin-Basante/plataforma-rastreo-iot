package com.rastreoiot.modelo;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Activo o sujeto monitoreado: vehículo, familiar o mascota (HU-05). */
@Document(collection = "activos")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "tipo")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Vehiculo.class, name = "VEHICULO"),
        @JsonSubTypes.Type(value = Familiar.class, name = "FAMILIAR"),
        @JsonSubTypes.Type(value = Mascota.class, name = "MASCOTA")
})
public abstract class Activo {

    @Id
    private String id;
    private Instant fechaRegistro = Instant.now();
    private String usuarioId;
    private String dispositivoId;

    /** Nombre corto para mostrar en listas y mapas. */
    public abstract String getEtiqueta();

    /** Descripción secundaria (modelo, parentesco, especie...). */
    public abstract String getDescripcion();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Instant getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(Instant fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
    public String getDispositivoId() { return dispositivoId; }
    public void setDispositivoId(String dispositivoId) { this.dispositivoId = dispositivoId; }
}
