package com.rastreoiot.presentacion;

import com.rastreoiot.negocio.excepciones.NegocioException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Traduce las excepciones a respuestas JSON con un mensaje claro para el usuario. */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<Map<String, Object>> negocio(NegocioException e) {
        return respuesta(e.getEstado(), e.getMessage(), e.getDetalles());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException e) {
        List<String> detalles = e.getBindingResult().getAllErrors().stream()
                .map(err -> err.getDefaultMessage()).distinct().toList();
        String mensaje = detalles.size() == 1 ? detalles.get(0) : "Revisa los datos del formulario.";
        return respuesta(HttpStatus.BAD_REQUEST, mensaje, detalles);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<Map<String, Object>> formato(Exception e) {
        return respuesta(HttpStatus.BAD_REQUEST, "La solicitud tiene datos con un formato inválido.", List.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> tamano(MaxUploadSizeExceededException e) {
        return respuesta(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo supera el tamaño máximo permitido (5 MB).", List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(NoResourceFoundException e) {
        return respuesta(HttpStatus.NOT_FOUND, "El recurso solicitado no existe.", List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> inesperado(Exception e) {
        LOG.error("Error no controlado", e);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Inténtalo de nuevo.", List.of());
    }

    private ResponseEntity<Map<String, Object>> respuesta(HttpStatus estado, String mensaje, List<String> detalles) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("estado", estado.value());
        cuerpo.put("mensaje", mensaje);
        cuerpo.put("detalles", detalles);
        cuerpo.put("fecha", Instant.now().toString());
        return ResponseEntity.status(estado).body(cuerpo);
    }
}
