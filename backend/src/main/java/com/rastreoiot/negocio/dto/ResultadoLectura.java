package com.rastreoiot.negocio.dto;

import com.rastreoiot.modelo.Alerta;
import java.util.List;

public record ResultadoLectura(String lecturaId, List<Alerta> alertasGeneradas, boolean configuracionPendiente, String mensaje) {
}
