package com.std.ec.PrimerSpring.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.std.ec.PrimerSpring.model.Reporte;
import com.std.ec.PrimerSpring.service.ReporteExcel;
import com.std.ec.PrimerSpring.service.ReportePdf;
import com.std.ec.PrimerSpring.service.ReporteService;
import com.std.ec.PrimerSpring.service.ReporteTabla;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;

/** Bean de admin/reportes.xhtml: elegir el reporte, verlo en pantalla y descargarlo en PDF o Excel. */
@Component("reporteBean")
@ViewScoped
public class ReporteBean {

    private static final Logger log = LoggerFactory.getLogger(ReporteBean.class);

    private final ReporteService servicio;
    private final ReportePdf pdf;
    private final ReporteExcel excel;

    private String tipo = "INGRESOS";
    private String estado = "TODOS";
    private LocalDate fechaInicio = LocalDate.now().withDayOfMonth(1);
    private LocalDate fechaFin = LocalDate.now();

    private ReporteTabla tabla;
    private List<Reporte> recientes;

    public ReporteBean(ReporteService servicio, ReportePdf pdf, ReporteExcel excel) {
        this.servicio = servicio;
        this.pdf = pdf;
        this.excel = excel;
    }

    // ================= Acciones =================

    public void ver() {
        ReporteTabla t = generar("PANTALLA");
        if (t != null) {
            tabla = t;
        }
    }

    public void descargarPdf() {
        descargar("PDF");
    }

    public void descargarExcel() {
        descargar("EXCEL");
    }

    private void descargar(String formato) {
        ReporteTabla t = generar(formato);
        if (t == null) {
            return;
        }
        tabla = t;
        try {
            boolean esPdf = "PDF".equals(formato);
            byte[] datos = esPdf ? pdf.generar(t, usuarioActual()) : excel.generar(t, usuarioActual());
            String mime = esPdf ? "application/pdf"
                    : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            String nombre = "reporte-" + tipo.toLowerCase() + "-" + fechaInicio + "_" + fechaFin
                    + (esPdf ? ".pdf" : ".xlsx");
            enviar(datos, mime, nombre);
        } catch (IOException | RuntimeException e) {
            log.error("Error al crear el archivo del reporte", e);
            error("No se pudo crear el archivo. Intenta nuevamente.");
        }
    }

    private ReporteTabla generar(String formato) {
        try {
            ReporteTabla t = servicio.generar(usuarioActual(), tipo, fechaInicio, fechaFin, estado, formato);
            recientes = null;
            log.info("Reporte {} ({}) generado por {}", tipo, formato, usuarioActual());
            return t;
        } catch (IllegalArgumentException e) {
            log.warn("Reporte rechazado: {}", e.getMessage());
            error(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error al generar el reporte", e);
            error("No se pudo generar el reporte. Intenta nuevamente.");
        }
        return null;
    }

    private void enviar(byte[] datos, String mime, String nombre) throws IOException {
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();
        ec.responseReset();
        ec.setResponseContentType(mime);
        ec.setResponseHeader("Content-Disposition", "attachment; filename=\"" + nombre + "\"");
        ec.setResponseContentLength(datos.length);
        ec.getResponseOutputStream().write(datos);
        fc.responseComplete();
    }

    // ================= Datos para la página =================

    public ReporteTabla getTabla() { return tabla; }

    public List<Map.Entry<String, String>> getResumen() {
        return tabla == null ? List.of() : new ArrayList<>(tabla.getResumen().entrySet());
    }

    public List<Reporte> getRecientes() {
        if (recientes == null) {
            recientes = servicio.recientes();
        }
        return recientes;
    }

    public String etiquetaTipo(String t) {
        if (t == null) {
            return "—";
        }
        return switch (t) {
            case "RESERVAS" -> "Reservas por estado y periodo";
            case "INGRESOS" -> "Ingresos y pagos";
            case "CLIENTES" -> "Clientes";
            case "PAQUETES" -> "Paquetes comercializados";
            default -> t;
        };
    }

    public String etiquetaFormato(String f) {
        if (f == null) {
            return "—";
        }
        return switch (f) {
            case "PANTALLA" -> "Pantalla";
            case "PDF" -> "PDF";
            case "EXCEL" -> "Excel";
            default -> f;
        };
    }

    // ================= Auxiliares =================

    private String usuarioActual() {
        return FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
    }

    private void error(String mensaje) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, mensaje, null));
    }

    // ================= Getters y setters =================

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
}