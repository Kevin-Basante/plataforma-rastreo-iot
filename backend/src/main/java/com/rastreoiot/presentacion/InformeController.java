package com.rastreoiot.presentacion;

import com.rastreoiot.modelo.InformePDF;
import com.rastreoiot.negocio.InformeService;
import com.rastreoiot.seguridad.SesionActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/informes")
@Tag(name = "Informes", description = "HU-17 Exportación de informes PDF")
public class InformeController {

    private final InformeService informeService;

    public InformeController(InformeService informeService) {
        this.informeService = informeService;
    }

    @GetMapping(value = "/dispositivos/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Descargar el informe PDF de recorrido y alertas de un dispositivo")
    public ResponseEntity<byte[]> pdf(@PathVariable String id, @RequestParam Instant desde, @RequestParam Instant hasta) {
        InformePDF informe = informeService.generarPdf(SesionActual.obtener(), id, desde, hasta);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(informe.getNombreArchivo()).build().toString())
                .body(informe.getContenido());
    }
}
