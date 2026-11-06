package com.std.ec.PrimerSpring.controller;
 
import java.time.LocalDate;
import java.util.List;
 
import org.primefaces.PrimeFaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
 
import com.std.ec.PrimerSpring.model.Agente;
import com.std.ec.PrimerSpring.service.AgenteService;
 
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
 
/**
 * Bean de admin/agentes.xhtml (lista + ventana modal para crear y editar).
 * ViewScoped: vive mientras el usuario está en la misma página, así la lista
 * y los botones de cada fila siempre coinciden.
 */
@Component("agenteBean")
@ViewScoped
public class AgenteBean {
 
    private static final Logger log = LoggerFactory.getLogger(AgenteBean.class);
 
    private final AgenteService agenteService;
 
    // ---- Lista y filtros ----
    private String filtroTexto = "";
    private String filtroEstado = "TODOS";
    private List<Agente> agentes;
 
    // ---- Formulario (ventana modal): sirve para crear y para editar ----
    private Integer idEdicion;          // null = agente nuevo
    private String nombre;
    private String documento;
    private String correo;
    private String telefono;
    private String sexo;
    private String nacionalidad;
    private LocalDate fechaNacimiento;
    private LocalDate fechaIngreso = LocalDate.now();
 
    // ---- Resultado del alta (se muestra una sola vez dentro de la ventana) ----
    private boolean creado;
    private String claveNueva;
    private String correoNuevo;
 
    // ---- Resultado de "Restablecer clave" (se muestra arriba de la tabla) ----
    private String claveTemporal;
    private String correoClave;
 
    public AgenteBean(AgenteService agenteService) {
        this.agenteService = agenteService;
    }
 
    // ================= Lista =================
 
    public List<Agente> getAgentes() {
        if (agentes == null) {
            agentes = agenteService.buscar(filtroTexto, filtroEstado);
        }
        return agentes;
    }
 
    public void filtrar() {
        agentes = null;
    }
 
    public void limpiar() {
        filtroTexto = "";
        filtroEstado = "TODOS";
        agentes = null;
    }
 
    public void cambiarEstado(Agente a) {
        try {
            boolean activo = agenteService.cambiarEstado(a.getId());
            info("El agente " + a.getNombreCompleto() + (activo ? " fue activado." : " fue desactivado."));
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error al cambiar el estado del agente", e);
            error("No se pudo cambiar el estado. Intenta nuevamente.");
        }
        agentes = null;
    }
 
    public void restablecer(Agente a) {
        try {
            claveTemporal = agenteService.restablecerContrasena(a.getId());
            correoClave = a.getUsuario().getCorreo();
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error al restablecer la contraseña", e);
            error("No se pudo restablecer la contraseña. Intenta nuevamente.");
        }
    }
 
    public void eliminar(Agente a) {
        try {
            agenteService.eliminar(a.getId());
            info("El agente " + a.getNombreCompleto() + " fue eliminado.");
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error al eliminar el agente", e);
            error("No se pudo eliminar. Intenta nuevamente.");
        }
        agentes = null;
    }
 
    public void cerrarClave() {
        claveTemporal = null;
        correoClave = null;
    }
 
    // ================= Ventana crear / editar =================
 
    /** Se llama al pulsar "Nuevo agente" (y "Registrar otro"): formulario limpio y en modo alta. */
    public void nuevo() {
        idEdicion = null;
        creado = false;
        claveNueva = null;
        correoNuevo = null;
        limpiarFormulario();
    }
 
    /** Se llama al pulsar "Editar" en una fila: carga los datos del agente en el formulario. */
    public void editar(Agente a) {
        idEdicion = a.getId();
        creado = false;
        claveNueva = null;
        correoNuevo = null;
        nombre = a.getNombreCompleto();
        documento = a.getDocumentoIdentidad();
        correo = a.getUsuario().getCorreo();
        telefono = a.getTelefono();
        sexo = a.getSexo();
        nacionalidad = a.getNacionalidad();
        fechaNacimiento = a.getFechaNacimiento();
        fechaIngreso = a.getFechaIngreso();
    }
 
    public void guardar() {
        try {
            if (idEdicion != null) {
                agenteService.actualizar(idEdicion, nombre, documento, correo, telefono,
                        sexo, nacionalidad, fechaNacimiento, fechaIngreso);
                info("Los datos de " + nombre.trim() + " fueron actualizados.");
                agentes = null;
                PrimeFaces.current().executeScript("PF('dlgNuevo').hide()");
                PrimeFaces.current().ajax().update("frm:msgs", "frm:tabla");
            } else {
                claveNueva = agenteService.crear(nombre, documento, correo, telefono,
                        sexo, nacionalidad, fechaNacimiento, fechaIngreso);
                correoNuevo = correo.trim().toLowerCase();
                creado = true;
                limpiarFormulario();
                agentes = null;   // la tabla se vuelve a consultar
            }
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error al guardar el agente", e);
            error("No se pudo guardar. Intenta nuevamente.");
        }
    }
 
    private void limpiarFormulario() {
        nombre = null;
        documento = null;
        correo = null;
        telefono = null;
        sexo = null;
        nacionalidad = null;
        fechaNacimiento = null;
        fechaIngreso = LocalDate.now();
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
 
    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }
 
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
 
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
 
    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }
 
    public String getNacionalidad() { return nacionalidad; }
    public void setNacionalidad(String nacionalidad) { this.nacionalidad = nacionalidad; }
 
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
 
    public LocalDate getFechaIngreso() { return fechaIngreso; }
    public void setFechaIngreso(LocalDate fechaIngreso) { this.fechaIngreso = fechaIngreso; }
 
    public boolean isCreado() { return creado; }
 
    public String getClaveNueva() { return claveNueva; }
 
    public String getCorreoNuevo() { return correoNuevo; }
 
    public String getClaveTemporal() { return claveTemporal; }
 
    public String getCorreoClave() { return correoClave; }
}