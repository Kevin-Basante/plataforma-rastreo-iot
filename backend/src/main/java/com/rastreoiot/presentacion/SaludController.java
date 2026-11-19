package com.rastreoiot.presentacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint público para verificar que la API está en línea (lo usa Render como health check). */
@RestController
@Tag(name = "Salud")
public class SaludController {

    @GetMapping("/api/salud")
    @Operation(summary = "Estado de la API")
    public Map<String, Object> salud() {
        return Map.of("estado", "en línea", "fecha", Instant.now().toString());
    }
}
