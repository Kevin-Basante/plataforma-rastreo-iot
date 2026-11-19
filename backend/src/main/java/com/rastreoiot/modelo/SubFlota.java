package com.rastreoiot.modelo;

import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Agrupación de vehículos de una empresa: sub-flota o sucursal (HU-18). */
@Document(collection = "subflotas")
public class SubFlota {

    @Id
    private String id;
    private String empresaId;
    private String nombre;
    private String descripcion;
    private Set<String> vehiculoIds = new LinkedHashSet<>();

    public SubFlota() {
    }

    public SubFlota(String empresaId, String nombre, String descripcion) {
        this.empresaId = empresaId;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmpresaId() { return empresaId; }
    public void setEmpresaId(String empresaId) { this.empresaId = empresaId; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Set<String> getVehiculoIds() { return vehiculoIds; }
    public void setVehiculoIds(Set<String> vehiculoIds) { this.vehiculoIds = vehiculoIds; }
}
