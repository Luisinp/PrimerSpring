package com.std.ec.PrimerSpring.controller;
 
import java.util.List;
 
import org.primefaces.PrimeFaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
 
import com.std.ec.PrimerSpring.model.Destino;
import com.std.ec.PrimerSpring.service.DestinoService;
 
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
 
/** Bean de operario/destinos.xhtml (lista + ventana modal para crear y editar). */
@Component("destinoBean")
@ViewScoped
public class DestinoBean {
 
    private static final Logger log = LoggerFactory.getLogger(DestinoBean.class);
 
    private final DestinoService destinoService;
 
    private String filtroTexto = "";
    private String filtroEstado = "TODOS";
    private List<Destino> destinos;
 
    private Integer idEdicion;   // null = destino nuevo
    private String nombre;
    private String pais;
    private String descripcion;
 
    public DestinoBean(DestinoService destinoService) {
        this.destinoService = destinoService;
    }
 
    // ================= Lista =================
 
    public List<Destino> getDestinos() {
        if (destinos == null) {
            destinos = destinoService.buscar(filtroTexto, filtroEstado);
        }
        return destinos;
    }
 
    public void filtrar() {
        destinos = null;
    }
 
    public void limpiar() {
        filtroTexto = "";
        filtroEstado = "TODOS";
        destinos = null;
    }
 
    public void cambiarEstado(Destino d) {
        try {
            boolean activo = destinoService.cambiarEstado(d.getId());
            info("El destino " + d.getNombre() + (activo ? " fue activado." : " fue desactivado."));
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error al cambiar el estado del destino", e);
            error("No se pudo cambiar el estado. Intenta nuevamente.");
        }
        destinos = null;
    }
 
    public void eliminar(Destino d) {
        try {
            destinoService.eliminar(d.getId());
            info("El destino " + d.getNombre() + " fue eliminado.");
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error al eliminar el destino", e);
            error("No se pudo eliminar. Intenta nuevamente.");
        }
        destinos = null;
    }
 
    // ================= Ventana crear / editar =================
 
    public void nuevo() {
        idEdicion = null;
        nombre = null;
        pais = null;
        descripcion = null;
    }
 
    public void editar(Destino d) {
        idEdicion = d.getId();
        nombre = d.getNombre();
        pais = d.getPais();
        descripcion = d.getDescripcion();
    }
 
    public void guardar() {
        try {
            if (idEdicion == null) {
                destinoService.crear(nombre, pais, descripcion);
                info("El destino " + nombre.trim() + " fue registrado.");
            } else {
                destinoService.actualizar(idEdicion, nombre, pais, descripcion);
                info("El destino " + nombre.trim() + " fue actualizado.");
            }
            destinos = null;
            PrimeFaces.current().executeScript("PF('dlgDestino').hide()");
            PrimeFaces.current().ajax().update("frm:msgs", "frm:tabla");
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error al guardar el destino", e);
            error("No se pudo guardar. Intenta nuevamente.");
        }
    }
 
    // ================= Mensajes =================
 
    private void info(String mensaje) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, mensaje, null));
    }
 
    private void error(String mensaje) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, mensaje, null));
    }
 
    // ================= Getters y setters =================
 
    public boolean isEditando() { return idEdicion != null; }
 
    public String getFiltroTexto() { return filtroTexto; }
    public void setFiltroTexto(String filtroTexto) { this.filtroTexto = filtroTexto; }
 
    public String getFiltroEstado() { return filtroEstado; }
    public void setFiltroEstado(String filtroEstado) { this.filtroEstado = filtroEstado; }
 
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
 
    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }
 
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
 