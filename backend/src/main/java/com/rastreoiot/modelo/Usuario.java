package com.rastreoiot.modelo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "usuarios")
public class Usuario {

    @Id
    private String id;
    private String nombre;
    @Indexed(unique = true)
    private String correo;
    @JsonIgnore
    private String contrasenaHash;
    private String telefono;
    private String ciudad;
    /** Imagen de perfil guardada como data URL (base64). */
    private String fotoPerfil;
    private Rol rol;
    private String empresaId;
    private Instant fechaRegistro;
    private List<ContactoEmergencia> contactosEmergencia = new ArrayList<>();
    private List<PreferenciaNotificacion> preferencias = new ArrayList<>();

    public Usuario() {
    }

    public Usuario(String nombre, String correo, String contrasenaHash, String telefono, Rol rol) {
        this.nombre = nombre;
        this.correo = correo;
        this.contrasenaHash = contrasenaHash;
        this.telefono = telefono;
        this.rol = rol;
        this.fechaRegistro = Instant.now();
        for (TipoAlerta tipo : TipoAlerta.values()) {
            preferencias.add(new PreferenciaNotificacion(tipo, true));
        }
    }

    /** Indica si el usuario quiere ver notificaciones de este tipo de alerta. */
    public boolean quiereNotificacion(TipoAlerta tipo) {
        return preferencias.stream()
                .filter(p -> p.getTipoAlerta() == tipo)
                .findFirst()
                .map(PreferenciaNotificacion::isActiva)
                .orElse(true);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getContrasenaHash() { return contrasenaHash; }
    public void setContrasenaHash(String contrasenaHash) { this.contrasenaHash = contrasenaHash; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    public String getFotoPerfil() { return fotoPerfil; }
    public void setFotoPerfil(String fotoPerfil) { this.fotoPerfil = fotoPerfil; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public String getEmpresaId() { return empresaId; }
    public void setEmpresaId(String empresaId) { this.empresaId = empresaId; }
    public Instant getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(Instant fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public List<ContactoEmergencia> getContactosEmergencia() { return contactosEmergencia; }
    public void setContactosEmergencia(List<ContactoEmergencia> contactosEmergencia) { this.contactosEmergencia = contactosEmergencia; }
    public List<PreferenciaNotificacion> getPreferencias() { return preferencias; }
    public void setPreferencias(List<PreferenciaNotificacion> preferencias) { this.preferencias = preferencias; }
}
