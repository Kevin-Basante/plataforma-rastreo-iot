package com.rastreoiot.negocio;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.rastreoiot.datos.ActivoRepository;
import com.rastreoiot.modelo.Activo;
import com.rastreoiot.modelo.Alerta;
import com.rastreoiot.modelo.Dispositivo;
import com.rastreoiot.modelo.InformePDF;
import com.rastreoiot.modelo.LecturaTelemetria;
import com.rastreoiot.modelo.Recorrido;
import com.rastreoiot.modelo.TipoAlerta;
import com.rastreoiot.seguridad.SesionUsuario;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Exportación del informe de recorrido y alertas en PDF (HU-17). */
@Service
public class InformeService {

    private static final Color PRIMARIO = new Color(0x0E, 0x7C, 0x66);
    private static final Color OSCURO = new Color(0x0B, 0x1F, 0x2A);
    private static final Color GRIS = new Color(0x5B, 0x6B, 0x73);
    private static final Color FONDO = new Color(0xF4, 0xF6, 0xF5);

    private static final Font TITULO = new Font(Font.HELVETICA, 18, Font.BOLD, OSCURO);
    private static final Font SUBTITULO = new Font(Font.HELVETICA, 12, Font.BOLD, OSCURO);
    private static final Font NORMAL = new Font(Font.HELVETICA, 10, Font.NORMAL, OSCURO);
    private static final Font NEGRITA = new Font(Font.HELVETICA, 10, Font.BOLD, OSCURO);
    private static final Font PEQUENA = new Font(Font.HELVETICA, 9, Font.NORMAL, GRIS);
    private static final Font CIFRA = new Font(Font.HELVETICA, 16, Font.BOLD, PRIMARIO);
    private static final Font CABECERA = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);

    private final MonitoreoService monitoreoService;
    private final DispositivoService dispositivoService;
    private final ActivoRepository activoRepository;
    private final ZoneId zona;
    private final DateTimeFormatter formatoFecha;

    public InformeService(MonitoreoService monitoreoService, DispositivoService dispositivoService,
                          ActivoRepository activoRepository, @Value("${rastreo.zona-horaria:America/Bogota}") String zona) {
        this.monitoreoService = monitoreoService;
        this.dispositivoService = dispositivoService;
        this.activoRepository = activoRepository;
        this.zona = ZoneId.of(zona);
        this.formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", new Locale("es", "CO")).withZone(this.zona);
    }

    public InformePDF generarPdf(SesionUsuario sesion, String dispositivoId, Instant desde, Instant hasta) {
        Recorrido recorrido = monitoreoService.construirRecorrido(sesion, dispositivoId, desde, hasta);
        Dispositivo dispositivo = dispositivoService.buscarSinAcceso(dispositivoId);
        Activo activo = dispositivo.getActivoId() == null ? null : activoRepository.findById(dispositivo.getActivoId()).orElse(null);
        List<Alerta> alertas = monitoreoService.alertasDelRango(dispositivoId, desde, hasta);
        String etiqueta = activo == null ? dispositivo.getSerial() : activo.getEtiqueta();

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.LETTER, 48, 48, 48, 48);
        try {
            PdfWriter.getInstance(doc, salida);
            doc.addTitle("Informe de recorrido y alertas - " + etiqueta);
            doc.addCreator("Plataforma de Rastreo IoT");
            doc.open();

            PdfPTable cabecera = new PdfPTable(new float[]{3, 2});
            cabecera.setWidthPercentage(100);
            cabecera.addCell(celdaSinBorde(new Phrase("Plataforma de Rastreo IoT", new Font(Font.HELVETICA, 11, Font.BOLD, PRIMARIO)), Element.ALIGN_LEFT));
            cabecera.addCell(celdaSinBorde(new Phrase("Generado: " + formatoFecha.format(Instant.now()) + "\nPor: " + sesion.nombre(), PEQUENA), Element.ALIGN_RIGHT));
            doc.add(cabecera);
            LineSeparator linea = new LineSeparator(1.5f, 100, OSCURO, Element.ALIGN_CENTER, -4);
            doc.add(new Chunk(linea));

            Paragraph titulo = new Paragraph("Informe de recorrido y alertas", TITULO);
            titulo.setSpacingBefore(14);
            doc.add(titulo);
            doc.add(new Paragraph(etiqueta + (activo == null ? "" : " · " + activo.getDescripcion()), PEQUENA));

            PdfPTable datos = new PdfPTable(new float[]{1, 2, 1, 2});
            datos.setWidthPercentage(100);
            datos.setSpacingBefore(12);
            agregarDato(datos, "Dispositivo", dispositivo.getTipo() + " " + dispositivo.getSerial());
            agregarDato(datos, "Periodo", formatoFecha.format(desde) + " a " + formatoFecha.format(hasta));
            agregarDato(datos, "Límite de velocidad", dispositivo.getLimiteVelocidad() == null ? "Configuración pendiente"
                    : String.format(Locale.ROOT, "%.0f km/h", dispositivo.getLimiteVelocidad()));
            agregarDato(datos, "Puntos registrados", String.valueOf(recorrido.getPuntos().size()));
            doc.add(datos);

            Paragraph resumen = new Paragraph("Resumen", SUBTITULO);
            resumen.setSpacingBefore(16);
            resumen.setSpacingAfter(6);
            doc.add(resumen);

            if (!recorrido.tieneDatos() && alertas.isEmpty()) {
                PdfPCell vacio = new PdfPCell(new Phrase(
                        "No se encontraron registros de recorrido ni eventos de alerta para este dispositivo en el periodo seleccionado.", NORMAL));
                vacio.setBackgroundColor(FONDO);
                vacio.setBorderColor(FONDO);
                vacio.setPadding(14);
                PdfPTable caja = new PdfPTable(1);
                caja.setWidthPercentage(100);
                caja.addCell(vacio);
                doc.add(caja);
            } else {
                Double velMax = recorrido.getPuntos().stream().map(LecturaTelemetria::getVelocidad).filter(Objects::nonNull)
                        .max(Double::compare).orElse(null);
                PdfPTable kpi = new PdfPTable(3);
                kpi.setWidthPercentage(100);
                kpi.addCell(kpi(String.format(Locale.ROOT, "%.1f km", recorrido.getDistanciaKm()), "Distancia recorrida"));
                kpi.addCell(kpi(String.valueOf(alertas.size()), "Eventos de alerta"));
                kpi.addCell(kpi(velMax == null ? "—" : String.format(Locale.ROOT, "%.0f km/h", velMax), "Velocidad máxima"));
                doc.add(kpi);

                Map<TipoAlerta, Long> conteo = new EnumMap<>(TipoAlerta.class);
                alertas.forEach(a -> conteo.merge(a.getTipo(), 1L, Long::sum));
                PdfPTable porTipo = new PdfPTable(4);
                porTipo.setWidthPercentage(100);
                porTipo.setSpacingBefore(8);
                for (TipoAlerta t : TipoAlerta.values()) {
                    porTipo.addCell(kpiPequeno(String.valueOf(conteo.getOrDefault(t, 0L)), nombreTipo(t)));
                }
                doc.add(porTipo);

                Paragraph tituloEventos = new Paragraph("Eventos de alerta", SUBTITULO);
                tituloEventos.setSpacingBefore(16);
                tituloEventos.setSpacingAfter(6);
                doc.add(tituloEventos);
                if (alertas.isEmpty()) {
                    doc.add(new Paragraph("No se registraron alertas en el periodo.", NORMAL));
                } else {
                    PdfPTable tabla = new PdfPTable(new float[]{1.3f, 1.4f, 3.2f, 1.6f});
                    tabla.setWidthPercentage(100);
                    tabla.setHeaderRows(1);
                    for (String c : List.of("Fecha y hora", "Tipo", "Detalle", "Ubicación")) {
                        PdfPCell celda = new PdfPCell(new Phrase(c, CABECERA));
                        celda.setBackgroundColor(OSCURO);
                        celda.setPadding(6);
                        celda.setBorderColor(OSCURO);
                        tabla.addCell(celda);
                    }
                    for (Alerta a : alertas) {
                        tabla.addCell(celda(formatoFecha.format(a.getFechaHora())));
                        tabla.addCell(celda(nombreTipo(a.getTipo())));
                        tabla.addCell(celda(a.getMensaje()));
                        tabla.addCell(celda(a.getLatitud() == null ? "—"
                                : String.format(Locale.ROOT, "%.5f, %.5f", a.getLatitud(), a.getLongitud())));
                    }
                    doc.add(tabla);
                }
            }
            Paragraph pie = new Paragraph("La distancia se calcula sumando los tramos entre posiciones GPS consecutivas.", PEQUENA);
            pie.setSpacingBefore(20);
            doc.add(pie);
            doc.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el informe PDF", e);
        }
        String nombre = "informe-" + etiqueta.replaceAll("[^A-Za-z0-9-]", "_") + "-"
                + DateTimeFormatter.ofPattern("yyyyMMdd").withZone(zona).format(desde) + ".pdf";
        return new InformePDF(nombre, salida.toByteArray());
    }

    public static String nombreTipo(TipoAlerta tipo) {
        return switch (tipo) {
            case ENTRADA_GEOCERCA -> "Entrada a geocerca";
            case SALIDA_GEOCERCA -> "Salida de geocerca";
            case SOS -> "SOS";
            case EXCESO_VELOCIDAD -> "Exceso de velocidad";
        };
    }

    private static PdfPCell celdaSinBorde(Phrase frase, int alineacion) {
        PdfPCell c = new PdfPCell(frase);
        c.setBorder(PdfPCell.NO_BORDER);
        c.setHorizontalAlignment(alineacion);
        c.setPaddingBottom(6);
        return c;
    }

    private static void agregarDato(PdfPTable tabla, String etiqueta, String valor) {
        PdfPCell e = new PdfPCell(new Phrase(etiqueta, PEQUENA));
        PdfPCell v = new PdfPCell(new Phrase(valor, NEGRITA));
        for (PdfPCell c : List.of(e, v)) {
            c.setBackgroundColor(FONDO);
            c.setBorderColor(FONDO);
            c.setPadding(6);
            tabla.addCell(c);
        }
    }

    private static PdfPCell kpi(String valor, String etiqueta) {
        Paragraph p = new Paragraph();
        p.add(new Phrase(valor + "\n", CIFRA));
        p.add(new Phrase(etiqueta, PEQUENA));
        PdfPCell c = new PdfPCell(p);
        c.setPadding(10);
        c.setBorderColor(new Color(0xDC, 0xE3, 0xE0));
        return c;
    }

    private static PdfPCell kpiPequeno(String valor, String etiqueta) {
        Paragraph p = new Paragraph();
        p.add(new Phrase(valor + "  ", NEGRITA));
        p.add(new Phrase(etiqueta, PEQUENA));
        PdfPCell c = new PdfPCell(p);
        c.setPadding(6);
        c.setBorderColor(new Color(0xDC, 0xE3, 0xE0));
        return c;
    }

    private static PdfPCell celda(String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, NORMAL));
        c.setPadding(5);
        c.setBorderColor(new Color(0xDC, 0xE3, 0xE0));
        return c;
    }
}
