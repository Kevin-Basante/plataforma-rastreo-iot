package com.rastreoiot.negocio.dto;

/** Enlace por código QR o por serial (HU-04). Se debe enviar uno de los dos. */
public record EnlaceRequest(String codigoQR, String serial, String activoId) {
}
