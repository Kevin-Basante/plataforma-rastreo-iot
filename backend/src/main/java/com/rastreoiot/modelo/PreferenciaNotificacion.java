package com.rastreoiot.modelo;

import jakarta.validation.constraints.NotNull;

public class PreferenciaNotificacion {

    @NotNull(message = "El tipo de alerta es obligatorio")
    private TipoAlerta tipoAlerta;
    private boolean activa;

    public PreferenciaNotificacion() {
    }

    public PreferenciaNotificacion(TipoAlerta tipoAlerta, boolean activa) {
        this.tipoAlerta = tipoAlerta;
        this.activa = activa;
    }

    public TipoAlerta getTipoAlerta() { return tipoAlerta; }
    public void setTipoAlerta(TipoAlerta tipoAlerta) { this.tipoAlerta = tipoAlerta; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
}
