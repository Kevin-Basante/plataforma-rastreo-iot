package com.rastreoiot.modelo;

import java.time.LocalDate;
import java.time.Period;

public class Familiar extends Activo {

    private String nombre;
    private String parentesco;
    private LocalDate fechaNacimiento;

    public Familiar() {
    }

    public Familiar(String nombre, String parentesco, LocalDate fechaNacimiento) {
        this.nombre = nombre;
        this.parentesco = parentesco;
        this.fechaNacimiento = fechaNacimiento;
    }

    @Override
    public String getEtiqueta() { return nombre; }

    @Override
    public String getDescripcion() {
        String texto = "Familiar · " + parentesco;
        if (fechaNacimiento != null) {
            texto += ", " + Period.between(fechaNacimiento, LocalDate.now()).getYears() + " años";
        }
        return texto;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getParentesco() { return parentesco; }
    public void setParentesco(String parentesco) { this.parentesco = parentesco; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
}
