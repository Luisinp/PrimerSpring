package com.std.ec.PrimerSpring.service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Un reporte ya calculado: título, columnas, filas y resumen. */
public class ReporteTabla {

    private final String titulo;
    private final String periodo;
    private final List<String> columnas;
    private final List<List<Object>> filas;
    private final Set<Integer> montos;                  // columnas que son dinero
    private final Map<String, String> resumen = new LinkedHashMap<>();
    private List<List<String>> filasTexto;

    public ReporteTabla(String titulo, String periodo, List<String> columnas,
                        List<List<Object>> filas, Set<Integer> montos) {
        this.titulo = titulo;
        this.periodo = periodo;
        this.columnas = columnas;
        this.filas = filas;
        this.montos = montos;
    }

    public void agregarResumen(String etiqueta, String valor) {
        resumen.put(etiqueta, valor);
    }

    public static String dinero(BigDecimal valor) {
        DecimalFormat f = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        return f.format(valor);
    }

    public String texto(Object valor, int columna) {
        if (valor instanceof Number n && montos.contains(columna)) {
            return dinero(new BigDecimal(n.toString()));
        }
        return valor == null ? "" : String.valueOf(valor);
    }

    public boolean esMonto(int columna) {
        return montos.contains(columna);
    }

    public List<List<String>> getFilasTexto() {
        if (filasTexto == null) {
            filasTexto = new ArrayList<>();
            for (List<Object> fila : filas) {
                List<String> f = new ArrayList<>();
                for (int i = 0; i < fila.size(); i++) {
                    f.add(texto(fila.get(i), i));
                }
                filasTexto.add(f);
            }
        }
        return filasTexto;
    }

    public boolean isVacia() { return filas.isEmpty(); }

    public String getTitulo() { return titulo; }
    public String getPeriodo() { return periodo; }
    public List<String> getColumnas() { return columnas; }
    public List<List<Object>> getFilas() { return filas; }
    public Map<String, String> getResumen() { return resumen; }
}