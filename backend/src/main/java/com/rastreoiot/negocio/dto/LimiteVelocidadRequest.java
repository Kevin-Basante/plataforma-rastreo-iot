package com.rastreoiot.negocio.dto;

/** Límite en km/h; null deja la configuración pendiente (HU-15). */
public record LimiteVelocidadRequest(Double limiteVelocidad) {
}
