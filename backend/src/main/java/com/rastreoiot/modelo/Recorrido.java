package com.rastreoiot.modelo;

import java.time.Instant;
import java.util.List;

/** Trayectoria calculada a partir de las lecturas de un rango de fechas (HU-16). No se persiste. */
public class Recorrido {

    private String dispositivoId;
    private Instant fechaInicio;
    private Instant fechaFin;
    private double distanciaKm;
    private List<LecturaTelemetria> puntos;

    public Recorrido() {
    }

    public Recorrido(String dispositivoId, Instant fechaInicio, Instant fechaFin, List<LecturaTelemetria> puntos) {
        this.dispositivoId = dispositivoId;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.puntos = puntos;
        this.distanciaKm = calcularDistancia(puntos);
    }

    public boolean tieneDatos() {
        return puntos != null && !puntos.isEmpty();
    }

    private static double calcularDistancia(List<LecturaTelemetria> puntos) {
        double metros = 0;
        LecturaTelemetria anterior = null;
        for (LecturaTelemetria p : puntos) {
            if (!p.tieneUbicacion()) {
                continue;
            }
            if (anterior != null) {
                metros += Geo.distanciaMetros(anterior.getLatitud(), anterior.getLongitud(), p.getLatitud(), p.getLongitud());
            }
            anterior = p;
        }
        return Math.round(metros / 10.0) / 100.0;
    }

    public String getDispositivoId() { return dispositivoId; }
    public void setDispositivoId(String dispositivoId) { this.dispositivoId = dispositivoId; }
    public Instant getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(Instant fechaInicio) { this.fechaInicio = fechaInicio; }
    public Instant getFechaFin() { return fechaFin; }
    public void setFechaFin(Instant fechaFin) { this.fechaFin = fechaFin; }
    public double getDistanciaKm() { return distanciaKm; }
    public void setDistanciaKm(double distanciaKm) { this.distanciaKm = distanciaKm; }
    public List<LecturaTelemetria> getPuntos() { return puntos; }
    public void setPuntos(List<LecturaTelemetria> puntos) { this.puntos = puntos; }
}
