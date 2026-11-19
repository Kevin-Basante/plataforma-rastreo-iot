package com.rastreoiot.modelo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "empresas")
public class Empresa {

    @Id
    private String id;
    private String nombre;
    @Indexed(unique = true)
    private String nit;

    public Empresa() {
    }

    public Empresa(String nombre, String nit) {
        this.nombre = nombre;
        this.nit = nit;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getNit() { return nit; }
    public void setNit(String nit) { this.nit = nit; }
}
