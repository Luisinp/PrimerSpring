package com.std.ec.PrimerSpring.controller;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.std.ec.PrimerSpring.model.Administrador;
import com.std.ec.PrimerSpring.service.AdministradorService;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;

/** Bean de admin/administradores.xhtml: lista, panel de alta y edición, estado, clave y eliminación. */
@Component("administradorBean")
@ViewScoped
public class AdministradorBean {

    private static final Logger log = LoggerFactory.getLogger(AdministradorBean.class);

    private final AdministradorService servicio;

    // Lista y filtros
    private String filtroTexto = "";
    private String filtroEstado = "TODOS";
    private List<Administrador> administradores;

    // Formulario (sirve para crear y para editar)
    private boolean formularioAbierto;
    private Integer idEdicion;          // null = administrador nuevo
    private String nombre;
    private String documento;
    private String correo;
    private String telefono;
    private String sexo;
    private String nacionalidad;
    private LocalDate fechaNacimiento;
    private LocalDate fechaIngreso = LocalDate.now();

    // Contraseña temporal (se muestra una sola vez)
    private String claveTemporal;
    private String correoClave;

    public AdministradorBean(AdministradorService servicio) {
        this.servicio = servicio;
    }

    // ================= Lista =================

    public List<Administrador> getAdministradores() {
        if (administradores == null) {
            administradores = servicio.buscar(filtroTexto, filtroEstado);
        }
        return administradores;
    }

    public void filtrar() {
        administradores = null;
    }

    public void limpiar() {
        filtroTexto = "";
        filtroEstado = "TODOS";
        administradores = null;
    }

    public boolean esYo(String correoFila) {
        String actual = usuarioActual();
        return actual != null && actual.equalsIgnoreCase(correoFila);
    }

    // ================= Acciones por fila =================

    public void cambiarEstado(Integer id, String nombreFila) {
        try {
            boolean activo = servicio.cambiarEstado(id, usuarioActual());
            info(nombreFila + (activo ? " fue activado." : " fue desactivado."));
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error al cambiar el estado del administrador", e);
            error("No se pudo cambiar el estado. Intenta nuevamente.");
        }
        administradores = null;
    }

    public void restablecer(Integer id, String correoFila) {
        try {
            claveTemporal = servicio.restablecerContrasena(id);
            correoClave = correoFila;
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error al restablecer la contraseña", e);
            error("No se pudo restablecer la contraseña. Intenta nuevamente.");
        }
    }

    public void eliminar(Integer id, String nombreFila) {
        try {
            servicio.eliminar(id, usuarioActual());
            info(nombreFila + " fue eliminado.");
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error al eliminar el administrador", e);
            error("No se pudo eliminar. Intenta nuevamente.");
        }
        administradores = null;
    }

    public void cerrarClave() {
        claveTemporal = null;
        correoClave = null;
    }

    // ================= Formulario =================

    public void nuevo() {
        cerrarClave();
        idEdicion = null;
        limpiarFormulario();
        formularioAbierto = true;
    }

    public void editar(Administrador a) {
        cerrarClave();
        idEdicion = a.getId();
        nombre = a.getNombreCompleto();
        documento = a.getDocumentoIdentidad();
        correo = a.getUsuario().getCorreo();
        telefono = a.getTelefono();
        sexo = a.getSexo();
        nacionalidad = a.getNacionalidad();
        fechaNacimiento = a.getFechaNacimiento();
        fechaIngreso = a.getFechaIngreso();
        formularioAbierto = true;
    }

    public void cancelar() {
        formularioAbierto = false;
        idEdicion = null;
        limpiarFormulario();
    }

    public void guardar() {
        try {
            if (idEdicion != null) {
                servicio.actualizar(idEdicion, usuarioActual(), nombre, documento, correo, telefono,
                        sexo, nacionalidad, fechaNacimiento, fechaIngreso);
                info("Los datos de " + nombre.trim() + " fueron actualizados.");
            } else {
                claveTemporal = servicio.crear(nombre, documento, correo, telefono,
                        sexo, nacionalidad, fechaNacimiento, fechaIngreso);
                correoClave = correo.trim().toLowerCase();
                info("Administrador registrado. Entrega la contraseña temporal que aparece abajo.");
            }
            cancelar();
            administradores = null;
        } catch (IllegalArgumentException e) {
            error(e.getMessage());          // el formulario queda abierto con lo escrito
        } catch (RuntimeException e) {
            log.error("Error al guardar el administrador", e);
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

    // ================= Auxiliares =================

    private String usuarioActual() {
        return FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
    }

    private void info(String mensaje) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, mensaje, null));
    }

    private void error(String mensaje) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, mensaje, null));
    }

    // ================= Getters y setters =================

    public boolean isFormularioAbierto() { return formularioAbierto; }
    public boolean isEditando() { return idEdicion != null; }

    public String getFiltroTexto() { return filtroTexto; }
    public void setFiltroTexto(String filtroTexto) { this.filtroTexto = filtroTexto == null ? "" : filtroTexto; }

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

    public String getClaveTemporal() { return claveTemporal; }
    public String getCorreoClave() { return correoClave; }
}