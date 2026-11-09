package com.std.ec.PrimerSpring.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/** Convierte un ReporteTabla en un archivo Excel .xlsx (Apache POI). */
@Component
public class ReporteExcel {

    public byte[] generar(ReporteTabla t, String generadoPor) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Reporte");

            Font negrita = libro.createFont();
            negrita.setBold(true);
            Font titulo = libro.createFont();
            titulo.setBold(true);
            titulo.setFontHeightInPoints((short) 14);
            Font blanca = libro.createFont();
            blanca.setBold(true);
            blanca.setColor(IndexedColors.WHITE.getIndex());

            CellStyle sTitulo = libro.createCellStyle();
            sTitulo.setFont(titulo);
            CellStyle sNegrita = libro.createCellStyle();
            sNegrita.setFont(negrita);
            CellStyle sCab = libro.createCellStyle();
            sCab.setFont(blanca);
            sCab.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            sCab.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle sMonto = libro.createCellStyle();
            sMonto.setDataFormat(libro.createDataFormat().getFormat("#,##0.00"));

            int r = 0;
            texto(hoja.createRow(r++), 0, "TravelZen · " + t.getTitulo(), sTitulo);
            texto(hoja.createRow(r++), 0, t.getPeriodo(), null);
            texto(hoja.createRow(r++), 0, "Generado el "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    + " por " + generadoPor, null);
            r++;

            int n = t.getColumnas().size();
            int[] ancho = new int[n];

            Row cab = hoja.createRow(r++);
            for (int i = 0; i < n; i++) {
                texto(cab, i, t.getColumnas().get(i), sCab);
                ancho[i] = t.getColumnas().get(i).length();
            }

            List<List<Object>> filas = t.getFilas();
            for (List<Object> fila : filas) {
                Row row = hoja.createRow(r++);
                for (int i = 0; i < n; i++) {
                    Object v = fila.get(i);
                    Cell c = row.createCell(i);
                    if (v instanceof Number num) {
                        c.setCellValue(num.doubleValue());
                        if (t.esMonto(i)) {
                            c.setCellStyle(sMonto);
                        }
                    } else {
                        c.setCellValue(String.valueOf(v));
                    }
                    ancho[i] = Math.max(ancho[i], t.texto(v, i).length());
                }
            }

            r++;
            for (Map.Entry<String, String> e : t.getResumen().entrySet()) {
                Row row = hoja.createRow(r++);
                texto(row, 0, e.getKey(), sNegrita);
                texto(row, 1, e.getValue(), null);
            }

            for (int i = 0; i < n; i++) {
                hoja.setColumnWidth(i, Math.min(60, ancho[i] + 3) * 256);
            }

            libro.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear el Excel", e);
        }
    }

    private static void texto(Row fila, int col, String valor, CellStyle estilo) {
        Cell c = fila.createCell(col);
        c.setCellValue(valor);
        if (estilo != null) {
            c.setCellStyle(estilo);
        }
    }
}