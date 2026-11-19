package com.rastreoiot.modelo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Zona de seguridad circular asociada a un dispositivo (HU-10, HU-11, HU-12). */
@Document(collection = "geocercas")
public class Geocerca {

    @Id
    private String id;
    private String dispositivoId;
    private String nombre;
    private double latitudCentro;
    private double longitudCentro;
    private double radioMetros;
    private EstadoGeocerca estado = EstadoGeocerca.ACTIVA;
    private Instant silenciadaHasta;
    /** Última posición conocida del dispositivo respecto a la zona; null si aún no se ha evaluado. */
    private Boolean dispositivoDentro;

    public Geocerca() {
    }

    public Geocerca(String dispositivoId, String nombre, double latitudCentro, double longitudCentro, double radioMetros) {
        this.dispositivoId = dispositivoId;
        this.nombre = nombre;
        this.latitudCentro = latitudCentro;
        this.longitudCentro = longitudCentro;
        this.radioMetros = radioMetros;
    }

    public void activar() {
        estado = EstadoGeocerca.ACTIVA;
        silenciadaHasta = null;
    }

    public void desactivar() {
        estado = EstadoGeocerca.INACTIVA;
        silenciadaHasta = null;
    }

    public void silenciar(int minutos) {
        estado = EstadoGeocerca.SILENCIADA;
        silenciadaHasta = Instant.now().plus(minutos, ChronoUnit.MINUTES);
    }

    /** Si el silencio terminó, la geocerca vuelve a estar activa. Devuelve true si cambió. */
    public boolean reanudarSiTerminoSilencio(Instant ahora) {
        if (estado == EstadoGeocerca.SILENCIADA && silenciadaHasta != null && !ahora.isBefore(silenciadaHasta)) {
            activar();
            return true;
        }
        return false;
    }

    public boolean contiene(double lat, double lon) {
        return Geo.distanciaMetros(latitudCentro, longitudCentro, lat, lon) <= radioMetros;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDispositivoId() { return dispositivoId; }
    public void setDispositivoId(String dispositivoId) { this.dispositivoId = dispositivoId; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public double getLatitudCentro() { return latitudCentro; }
    public void setLatitudCentro(double latitudCentro) { this.latitudCentro = latitudCentro; }
    public double getLongitudCentro() { return longitudCentro; }
    public void setLongitudCentro(double longitudCentro) { this.longitudCentro = longitudCentro; }
    public double getRadioMetros() { return radioMetros; }
    public void setRadioMetros(double radioMetros) { this.radioMetros = radioMetros; }
    public EstadoGeocerca getEstado() { return estado; }
    public void setEstado(EstadoGeocerca estado) { this.estado = estado; }
    public Instant getSilenciadaHasta() { return silenciadaHasta; }
    public void setSilenciadaHasta(Instant silenciadaHasta) { this.silenciadaHasta = silenciadaHasta; }
    public Boolean getDispositivoDentro() { return dispositivoDentro; }
    public void setDispositivoDentro(Boolean dispositivoDentro) { this.dispositivoDentro = dispositivoDentro; }
}
