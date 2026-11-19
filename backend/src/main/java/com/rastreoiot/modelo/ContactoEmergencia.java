package com.rastreoiot.modelo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ContactoEmergencia {

    @NotBlank(message = "El nombre del contacto es obligatorio")
    private String nombre;
    @NotBlank(message = "El teléfono del contacto es obligatorio")
    @Pattern(regexp = "^[0-9 +()-]{7,20}$", message = "El teléfono del contacto no es válido")
    private String telefono;
    @NotBlank(message = "El parentesco del contacto es obligatorio")
    private String parentesco;

    public ContactoEmergencia() {
    }

    public ContactoEmergencia(String nombre, String telefono, String parentesco) {
        this.nombre = nombre;
        this.telefono = telefono;
        this.parentesco = parentesco;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getParentesco() { return parentesco; }
    public void setParentesco(String parentesco) { this.parentesco = parentesco; }
}
