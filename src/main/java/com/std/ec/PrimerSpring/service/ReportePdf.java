package com.std.ec.PrimerSpring.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

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

/** Convierte un ReporteTabla en un PDF (OpenPDF). */

@Component
public class ReportePdf {

    public byte[] generar(ReporteTabla t, String generadoPor) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Color azul = new Color(11, 59, 96);
            Color gris = new Color(71, 85, 105);
            Font fMarca = new Font(Font.HELVETICA, 11, Font.BOLD, azul);
            Font fTitulo = new Font(Font.HELVETICA, 18, Font.BOLD, azul);
            Font fTexto = new Font(Font.HELVETICA, 9, Font.NORMAL, gris);
            Font fResumen = new Font(Font.HELVETICA, 10, Font.BOLD, azul);
            Font fCab = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
            Font fCelda = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(15, 42, 67));

            doc.add(new Paragraph("TravelZen · Agencia de viajes internacional", fMarca));
            doc.add(new Paragraph(t.getTitulo(), fTitulo));
            doc.add(new Paragraph(t.getPeriodo(), fTexto));
            doc.add(new Paragraph("Generado el "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    + " por " + generadoPor, fTexto));

            StringBuilder resumen = new StringBuilder();
            for (Map.Entry<String, String> e : t.getResumen().entrySet()) {
                if (resumen.length() > 0) {
                    resumen.append("     ");
                }
                resumen.append(e.getKey()).append(": ").append(e.getValue());
            }
            Paragraph pResumen = new Paragraph(resumen.toString(), fResumen);
            pResumen.setSpacingBefore(10);
            pResumen.setSpacingAfter(10);
            doc.add(pResumen);

            if (t.isVacia()) {
                doc.add(new Paragraph("No hay datos en el periodo elegido.", fTexto));
            } else {
                int n = t.getColumnas().size();
                PdfPTable tabla = new PdfPTable(n);
                tabla.setWidthPercentage(100);
                tabla.setHeaderRows(1);

                for (String c : t.getColumnas()) {
                    PdfPCell cel = new PdfPCell(new Phrase(c, fCab));
                    cel.setBackgroundColor(azul);
                    cel.setPadding(5);
                    tabla.addCell(cel);
                }
                List<List<String>> filas = t.getFilasTexto();
                for (int i = 0; i < filas.size(); i++) {
                    List<String> fila = filas.get(i);
                    for (int j = 0; j < n; j++) {
                        PdfPCell cel = new PdfPCell(new Phrase(fila.get(j), fCelda));
                        cel.setPadding(4);
                        if (i % 2 == 1) {
                            cel.setBackgroundColor(new Color(244, 247, 250));
                        }
                        if (t.esMonto(j)) {
                            cel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        }
                        tabla.addCell(cel);
                    }
                }
                doc.add(tabla);
            }
            doc.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo crear el PDF", e);
        }
        return out.toByteArray();
    }
}