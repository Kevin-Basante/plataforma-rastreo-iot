package com.rastreoiot.modelo;

public class Vehiculo extends Activo {

    private String placa;
    private String modelo;
    private Boolean motorEncendido;
    private Double kilometraje;

    public Vehiculo() {
    }

    public Vehiculo(String placa, String modelo) {
        this.placa = placa;
        this.modelo = modelo;
    }

    @Override
    public String getEtiqueta() { return placa; }

    @Override
    public String getDescripcion() { return "Vehículo · " + modelo; }

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public Boolean getMotorEncendido() { return motorEncendido; }
    public void setMotorEncendido(Boolean motorEncendido) { this.motorEncendido = motorEncendido; }
    public Double getKilometraje() { return kilometraje; }
    public void setKilometraje(Double kilometraje) { this.kilometraje = kilometraje; }
}
