package com.rastreoiot.negocio.dto;

public record GeocercaRequest(String dispositivoId, String nombre, Double latitudCentro, Double longitudCentro, Double radioMetros) {
}
