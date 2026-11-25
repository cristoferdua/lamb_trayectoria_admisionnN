package pe.edu.upeu.lamb.admision.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.edu.upeu.lamb.admision.dto.PostulanteResponse;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PdfCarnetService {

    private static final Color NAVY = new Color(26, 36, 86);
    private static final Color GOLD = new Color(245, 184, 0);
    private static final Color MUTED = new Color(107, 114, 128);
    private static final Color LIGHT = new Color(246, 247, 251);

    private final QrService qrService;

    public byte[] generar(PostulanteResponse p) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 48, 48, 48, 48);
            PdfWriter.getInstance(doc, out);
            doc.open();

            // Header navy
            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[]{1, 4});

            PdfPCell logo = new PdfPCell(new Phrase("A",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 28, Color.WHITE)));
            logo.setBackgroundColor(GOLD);
            logo.setHorizontalAlignment(Element.ALIGN_CENTER);
            logo.setVerticalAlignment(Element.ALIGN_MIDDLE);
            logo.setFixedHeight(60);
            logo.setBorder(Rectangle.NO_BORDER);
            header.addCell(logo);

            PdfPCell titulo = new PdfPCell();
            titulo.setBackgroundColor(NAVY);
            titulo.setPadding(14);
            titulo.setBorder(Rectangle.NO_BORDER);
            titulo.addElement(new Paragraph("UNIVERSIDAD PERUANA UNIÓN",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.WHITE)));
            titulo.addElement(new Paragraph("Carnet del Postulante · Admisión " + periodoDesdeCodigo(p.getCodigoPostulante()),
                    FontFactory.getFont(FontFactory.HELVETICA, 10, GOLD)));
            header.addCell(titulo);
            doc.add(header);

            doc.add(Chunk.NEWLINE);

            // Código grande
            PdfPTable codigoBox = new PdfPTable(1);
            codigoBox.setWidthPercentage(100);
            PdfPCell codigoCell = new PdfPCell();
            codigoCell.setBackgroundColor(LIGHT);
            codigoCell.setBorderColorLeft(GOLD);
            codigoCell.setBorderWidthLeft(4);
            codigoCell.setBorderWidthTop(0);
            codigoCell.setBorderWidthRight(0);
            codigoCell.setBorderWidthBottom(0);
            codigoCell.setPadding(16);
            codigoCell.addElement(new Paragraph("CÓDIGO DE POSTULANTE",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, MUTED)));
            codigoCell.addElement(new Paragraph(safe(p.getCodigoPostulante()),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, NAVY)));
            codigoBox.addCell(codigoCell);
            doc.add(codigoBox);

            doc.add(Chunk.NEWLINE);

            // Datos + QR
            PdfPTable mid = new PdfPTable(2);
            mid.setWidthPercentage(100);
            mid.setWidths(new float[]{3, 2});

            PdfPCell datos = new PdfPCell();
            datos.setBorder(Rectangle.NO_BORDER);
            datos.setPadding(4);
            datos.addElement(sectionTitle("DATOS DEL POSTULANTE"));
            datos.addElement(kv("ID Persona",       str(p.getIdPersona())));
            datos.addElement(kv("Estado",           safe(p.getEstado())));
            datos.addElement(kv("Fecha de registro",
                    p.getFechaRegistro() == null ? "—" :
                    DateTimeFormatter.ofPattern("dd 'de' MMMM yyyy · HH:mm", new Locale("es", "PE"))
                            .format(p.getFechaRegistro().atZone(ZoneId.systemDefault()))));
            mid.addCell(datos);

            // QR con URL o código
            PdfPCell qrCell = new PdfPCell();
            qrCell.setBorder(Rectangle.NO_BORDER);
            qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            byte[] qrPng = qrService.generar(safe(p.getCodigoPostulante()), 220);
            Image qrImg = Image.getInstance(qrPng);
            qrImg.scaleToFit(160, 160);
            qrImg.setAlignment(Image.ALIGN_CENTER);
            qrCell.addElement(qrImg);
            Paragraph qrLabel = new Paragraph("Código QR · escanear",
                    FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED));
            qrLabel.setAlignment(Element.ALIGN_CENTER);
            qrCell.addElement(qrLabel);
            mid.addCell(qrCell);

            doc.add(mid);
            doc.add(Chunk.NEWLINE);

            // Académico
            doc.add(sectionTitle("INFORMACIÓN ACADÉMICA"));
            PdfPTable acad = new PdfPTable(2);
            acad.setWidthPercentage(100);
            acad.setWidths(new float[]{1, 1});
            acad.setSpacingBefore(6);
            acad.addCell(kvCell("Convocatoria",  safe(p.getCodigoConvocatoria()) + "  (#" + str(p.getIdConvocatoria()) + ")"));
            acad.addCell(kvCell("Modalidad",     safe(p.getCodigoModalidad()) + "  (#" + str(p.getIdModalidad()) + ")"));
            acad.addCell(kvCell("Programa",      "#" + str(p.getIdPrograma())));
            acad.addCell(kvCell("Periodo",       periodoDesdeCodigo(p.getCodigoPostulante())));
            doc.add(acad);

            doc.add(Chunk.NEWLINE);

            // Colegio
            doc.add(sectionTitle("COLEGIO DE PROCEDENCIA"));
            PdfPTable col = new PdfPTable(2);
            col.setWidthPercentage(100);
            col.setWidths(new float[]{1, 1});
            col.setSpacingBefore(6);
            col.addCell(kvCell("Institución",    safe(p.getColegioProcedencia())));
            col.addCell(kvCell("Año de egreso",  str(p.getAnioEgresoColegio())));
            col.addCell(kvCell("Promedio",       p.getPromedioColegio() == null ? "—" : p.getPromedioColegio().toString()));
            col.addCell(kvCell("", ""));
            doc.add(col);

            doc.add(Chunk.NEWLINE);

            // Contacto
            doc.add(sectionTitle("CONTACTO"));
            PdfPTable cont = new PdfPTable(2);
            cont.setWidthPercentage(100);
            cont.setWidths(new float[]{1, 1});
            cont.setSpacingBefore(6);
            cont.addCell(kvCell("Teléfono",          safe(p.getTelefonoContacto())));
            cont.addCell(kvCell("Correo",            safe(p.getCorreoContacto())));
            cont.addCell(kvCell("Apoderado",         safe(p.getNombreApoderado())));
            cont.addCell(kvCell("Teléfono apoderado", safe(p.getTelefonoApoderado())));
            doc.add(cont);

            doc.add(Chunk.NEWLINE);
            doc.add(Chunk.NEWLINE);

            // Footer
            Paragraph footer = new Paragraph(
                    "Universidad Peruana Unión · Oficina de Admisión\n"
                  + "Este documento es el carnet oficial del postulante. Presentarlo el día del examen de admisión junto al DNI.",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, MUTED));
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF: " + e.getMessage(), e);
        }
    }

    private static Paragraph sectionTitle(String txt) {
        Paragraph p = new Paragraph(txt,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, NAVY));
        p.setSpacingAfter(4);
        return p;
    }

    private static Paragraph kv(String label, String value) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + ": ",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, MUTED)));
        p.add(new Chunk(value,
                FontFactory.getFont(FontFactory.HELVETICA, 10, NAVY)));
        p.setSpacingAfter(3);
        return p;
    }

    private static PdfPCell kvCell(String label, String value) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(new Color(229, 231, 235));
        c.setPadding(6);
        c.addElement(new Paragraph(label.toUpperCase(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, MUTED)));
        c.addElement(new Paragraph(value == null || value.isBlank() ? "—" : value,
                FontFactory.getFont(FontFactory.HELVETICA, 10, NAVY)));
        return c;
    }

    private static String safe(String s)  { return (s == null || s.isBlank()) ? "—" : s; }
    private static String str(Object o)   { return o == null ? "—" : o.toString(); }

    /** Extrae el periodo de un código tipo POS-{periodo}-{correlativo}. */
    private static String periodoDesdeCodigo(String codigo) {
        if (codigo == null) return "—";
        String[] parts = codigo.split("-");
        if (parts.length >= 3) {
            return parts[1] + "-" + parts[2];
        }
        return "—";
    }
}
