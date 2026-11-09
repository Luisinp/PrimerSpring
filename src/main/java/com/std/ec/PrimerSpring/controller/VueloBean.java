package com.std.ec.PrimerSpring.controller;
 
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
 
import com.std.ec.PrimerSpring.model.Vuelo;
import com.std.ec.PrimerSpring.service.VueloService;
 
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
 
/** Bean de operario/vuelos.xhtml: lista con filtros, alta, edición, activar/desactivar y eliminar. */
@Component("vueloBean")
@ViewScoped
public class VueloBean {
 
    private static final Logger log = LoggerFactory.getLogger(VueloBean.class);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
 
    private final VueloService servicio;
 
    // Filtros
    private String texto = "";
    private String estado = "TODOS";
 
    private List<Vuelo> vuelos;
    private Long activos;
    private Long inactivos;
 
    // Formulario (alta y edición)
    private boolean formularioAbierto;
    private Integer idEditando;
    private String aerolinea = "";
    private String origen = "";
    private String destino = "";
    private LocalDate fecha;
    private String hora = "";
    private BigDecimal tarifa;
    private Integer asientos;
 
    public VueloBean(VueloService servicio) {
        this.servicio = servicio;
    }
 
    // ================= Datos =================
 
    public List<Vuelo> getVuelos() {
        if (vuelos == null) {
            vuelos = servicio.buscar(texto, estado);
        }
        return vuelos;
    }
 
    public long getActivos() {
        if (activos == null) {
            activos = servicio.contarActivos();
        }
        return activos;
    }
 
    public long getInactivos() {
        if (inactivos == null) {
            inactivos = servicio.contarInactivos();
        }
        return inactivos;
    }
 
    public LocalDate getHoy() {
        return LocalDate.now();
    }
 
    // ================= Filtros =================
 
    public void buscar() {
        recargar();
    }
 
    public void limpiar() {
        texto = "";
        estado = "TODOS";
        recargar();
    }
 
    // ================= Alta y edición =================
 
    public void nuevo() {
        limpiarFormulario();
        formularioAbierto = true;
    }
 
    public void editar(Integer id) {
        Optional<Vuelo> encontrado = servicio.obtener(id);
        if (encontrado.isEmpty()) {
            error("El vuelo ya no existe.");
            recargar();
            return;
        }
        Vuelo v = encontrado.get();
        limpiarFormulario();
        idEditando = v.getId();
        aerolinea = v.getAerolinea();
        origen = v.getOrigen();
        destino = v.getDestino();
        fecha = v.getFecha();
        hora = v.getHora().format(HORA);
        tarifa = v.getTarifa();
        asientos = v.getAsientosDisponibles();
        formularioAbierto = true;
    }
 
    public void guardar() {
        try {
            Vuelo v = servicio.guardar(idEditando, aerolinea, origen, destino, fecha, hora, tarifa, asientos);
            mensaje(FacesMessage.SEVERITY_INFO, "Vuelo " + v.getOrigen() + " a " + v.getDestino() + " guardado.");
            limpiarFormulario();
            formularioAbierto = false;
            recargar();
        } catch (IllegalArgumentException e) {
            log.warn("Vuelo rechazado: {}", e.getMessage());
            error(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error al guardar el vuelo", e);
            error("No se pudo guardar el vuelo. Intenta nuevamente.");
        }
    }
 
    // ================= Desactivar, activar y eliminar =================
 
    public void desactivar(Integer id) {
        cambiarActivo(id, false, "desactivado. Ya no recibe reservas nuevas.");
    }
 
    public void activar(Integer id) {
        cambiarActivo(id, true, "activado.");
    }
 
    private void cambiarActivo(Integer id, boolean activo, String resultado) {
        try {
            Vuelo v = servicio.cambiarActivo(id, activo);
            mensaje(FacesMessage.SEVERITY_INFO, "Vuelo " + v.getOrigen() + " a " + v.getDestino() + " " + resultado);
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error al cambiar el estado del vuelo {}", id, e);
            error("No se pudo completar la operación. Intenta nuevamente.");
        }
        recargar();
    }
 
    public void eliminar(Integer id) {
        try {
            servicio.eliminar(id);
            mensaje(FacesMessage.SEVERITY_INFO, "Vuelo eliminado.");
        } catch (IllegalArgumentException e) {
            log.warn("Eliminación rechazada: {}", e.getMessage());
            error(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error al eliminar el vuelo {}", id, e);
            error("No se pudo eliminar el vuelo. Intenta nuevamente.");
        }
        recargar();
    }
 
    // ================= Auxiliares =================
 
    private void recargar() {
        vuelos = null;
        activos = null;
        inactivos = null;
    }
 
    private void limpiarFormulario() {
        idEditando = null;
        aerolinea = "";
        origen = "";
        destino = "";
        fecha = null;
        hora = "";
        tarifa = null;
        asientos = null;
    }
 
    private void error(String texto) {
        mensaje(FacesMessage.SEVERITY_ERROR, texto);
    }
 
    private void mensaje(FacesMessage.Severity severidad, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidad, texto, null));
    }
 
    // ================= Getters y setters =================
 
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto == null ? "" : texto; }
 
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
 
    public boolean isFormularioAbierto() { return formularioAbierto; }
    public Integer getIdEditando() { return idEditando; }
 
    public String getAerolinea() { return aerolinea; }
    public void setAerolinea(String aerolinea) { this.aerolinea = aerolinea == null ? "" : aerolinea; }
 
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen == null ? "" : origen; }
 
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino == null ? "" : destino; }
 
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
 
    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora == null ? "" : hora; }
 
    public BigDecimal getTarifa() { return tarifa; }
    public void setTarifa(BigDecimal tarifa) { this.tarifa = tarifa; }
 
    public Integer getAsientos() { return asientos; }
    public void setAsientos(Integer asientos) { this.asientos = asientos; }
}
 