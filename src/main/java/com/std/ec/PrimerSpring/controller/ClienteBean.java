package com.std.ec.PrimerSpring.controller;
 
import java.time.LocalDate;
import java.util.List;
 
import org.primefaces.PrimeFaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
 
import com.std.ec.PrimerSpring.model.Cliente;
import com.std.ec.PrimerSpring.service.ClienteService;
 
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
 
/** Bean de operario/clientes.xhtml (lista + ventana modal para crear y editar). */
@Component("clienteBean")
@ViewScoped
public class ClienteBean {
 
    private static final Logger log = LoggerFactory.getLogger(ClienteBean.class);
 
    private final ClienteService clienteService;
 
    // ---- Lista y filtros ----
    private String filtroTexto = "";
    private String filtroEstado = "TODOS";
    private String filtroCuenta = "TODOS";
    private List<Cliente> clientes;
 
    // ---- Formulario (ventana modal) ----
    private Integer idEdicion;          // null = cliente nuevo
    private boolean tieneCuenta;        // true = el correo no se puede cambiar
    private String nombre;
    private String documento;
    private String correo;
    private String telefono;
    private String sexo;
    private String nacionalidad;
    private LocalDate fechaNacimiento;
 
    public ClienteBean(ClienteService clienteService) {
        this.clienteService = clienteService;
    }
 
    // ================= Lista =================
 
    public List<Cliente> getClientes() {
        if (clientes == null) {
            clientes = clienteService.buscar(filtroTexto, filtroEstado, filtroCuenta);
        }
        return clientes;
    }
 
    public void filtrar() {
        clientes = null;
    }
 
    public void limpiar() {
        filtroTexto = "";
        filtroEstado = "TODOS";
        filtroCuenta = "TODOS";
        clientes = null;
    }
 
    public void cambiarEstado(Cliente c) {
        try {
            boolean activo = clienteService.cambiarEstado(c.getId());
            info("El cliente " + c.getNombreCompleto() + (activo ? " fue activado." : " fue desactivado."));
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error al cambiar el estado del cliente", e);
            error("No se pudo cambiar el estado. Intenta nuevamente.");
        }
        clientes = null;
    }
 
    public void eliminar(Cliente c) {
        try {
            clienteService.eliminar(c.getId());
            info("El cliente " + c.getNombreCompleto() + " fue eliminado.");
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error al eliminar el cliente", e);
            error("No se pudo eliminar. Intenta nuevamente.");
        }
        clientes = null;
    }
 
    // ================= Ventana crear / editar =================
 
    public void nuevo() {
        idEdicion = null;
        tieneCuenta = false;
        limpiarFormulario();
    }
 
    public void editar(Cliente c) {
        idEdicion = c.getId();
        tieneCuenta = c.getUsuario() != null;
        nombre = c.getNombreCompleto();
        documento = c.getDocumentoIdentidad();
        correo = c.getCorreo();
        telefono = c.getTelefono();
        sexo = c.getSexo();
        nacionalidad = c.getNacionalidad();
        fechaNacimiento = c.getFechaNacimiento();
    }
 
    public void guardar() {
        try {
            if (idEdicion == null) {
                clienteService.crear(nombre, documento, correo, telefono, sexo, nacionalidad, fechaNacimiento);
                info("El cliente " + nombre.trim() + " fue registrado.");
            } else {
                clienteService.actualizar(idEdicion, nombre, documento, correo, telefono,
                        sexo, nacionalidad, fechaNacimiento);
                info("Los datos de " + nombre.trim() + " fueron actualizados.");
            }
            clientes = null;
            PrimeFaces.current().executeScript("PF('dlgCliente').hide()");
            PrimeFaces.current().ajax().update("frm:msgs", "frm:tabla");
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error al guardar el cliente", e);
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
    public boolean isTieneCuenta() { return tieneCuenta; }
 
    public String getFiltroTexto() { return filtroTexto; }
    public void setFiltroTexto(String filtroTexto) { this.filtroTexto = filtroTexto; }
 
    public String getFiltroEstado() { return filtroEstado; }
    public void setFiltroEstado(String filtroEstado) { this.filtroEstado = filtroEstado; }
 
    public String getFiltroCuenta() { return filtroCuenta; }
    public void setFiltroCuenta(String filtroCuenta) { this.filtroCuenta = filtroCuenta; }
 
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
}
 