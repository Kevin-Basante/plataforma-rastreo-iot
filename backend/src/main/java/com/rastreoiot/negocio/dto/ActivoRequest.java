package com.rastreoiot.negocio.dto;

import java.time.LocalDate;

/**
 * Registro de un activo monitoreado (HU-05). Los campos obligatorios dependen del tipo:
 * VEHICULO (placa, modelo), FAMILIAR (nombre, parentesco) y MASCOTA (nombre, especie).
 */
public record ActivoRequest(
        String tipo,
        String placa,
        String modelo,
        String nombre,
        String parentesco,
        LocalDate fechaNacimiento,
        String especie,
        String raza,
        String dispositivoId) {
}
