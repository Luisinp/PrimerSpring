package com.std.ec.PrimerSpring.controller;
 
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
 
import com.std.ec.PrimerSpring.service.RegistroService;
 
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
 
@Component("registroBean")
@Scope("request")
public class RegistroBean {
 
    private final RegistroService registroService;
 
    private String nombre;
    private String documento;
    private String correo;
    private String telefono;
    private String nacionalidad;
    private String contrasena;
    private String confirmacion;
    private boolean registrado;
 
    public RegistroBean(RegistroService registroService) {
        this.registroService = registroService;
    }
 
    public String registrar() {
        if (!contrasena.equals(confirmacion)) {
            error("Las contraseñas no coinciden.");
            return null;
        }
        if (contrasena.length() < 8) {
            error("La contraseña debe tener al menos 8 caracteres.");
            return null;
        }
        try {
            registroService.registrarCliente(nombre, documento, correo, telefono, nacionalidad, contrasena);
            registrado = true;
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
        } catch (RuntimeException e) {
            error("No se pudo completar el registro. Intenta nuevamente.");
        }
        return null;
    }
 
    private void error(String mensaje) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, mensaje, null));
    }
 
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
 
    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }
 
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
 
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
 
    public String getNacionalidad() { return nacionalidad; }
    public void setNacionalidad(String nacionalidad) { this.nacionalidad = nacionalidad; }
 
    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
 
    public String getConfirmacion() { return confirmacion; }
    public void setConfirmacion(String confirmacion) { this.confirmacion = confirmacion; }
 
    public boolean isRegistrado() { return registrado; }
}