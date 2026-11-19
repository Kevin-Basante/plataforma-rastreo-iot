package com.rastreoiot.modelo;

public class Mascota extends Activo {

    private String nombre;
    private String especie;
    private String raza;

    public Mascota() {
    }

    public Mascota(String nombre, String especie, String raza) {
        this.nombre = nombre;
        this.especie = especie;
        this.raza = raza;
    }

    @Override
    public String getEtiqueta() { return nombre; }

    @Override
    public String getDescripcion() {
        return "Mascota · " + especie + (raza != null && !raza.isBlank() ? ", " + raza : "");
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEspecie() { return especie; }
    public void setEspecie(String especie) { this.especie = especie; }
    public String getRaza() { return raza; }
    public void setRaza(String raza) { this.raza = raza; }
}
