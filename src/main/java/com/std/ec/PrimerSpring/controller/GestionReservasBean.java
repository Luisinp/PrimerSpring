package com.std.ec.PrimerSpring.controller;
 
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
 
import com.std.ec.PrimerSpring.model.HistorialEstadoReserva;
import com.std.ec.PrimerSpring.model.Reserva;
import com.std.ec.PrimerSpring.service.GestionReservaService;
 
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
 
/** Bean de operario/reservas.xhtml: lista con filtros, cambios de estado e historial. */
@Component("gestionReservasBean")
@ViewScoped
public class GestionReservasBean {
 
    private static final Logger log = LoggerFactory.getLogger(GestionReservasBean.class);
 
    private final GestionReservaService servicio;
 
    // Filtros
    private String estado = "TODOS";
    private String texto = "";
 
    private List<Reserva> reservas;
    private Map<String, Long> conteo;
 
    // Reserva cuyo historial está desplegado
    private Integer historialDe;
    private List<HistorialEstadoReserva> historial = new ArrayList<>();
 
    public GestionReservasBean(GestionReservaService servicio) {
        this.servicio = servicio;
    }
 
    // ================= Datos =================
 
    public List<Reserva> getReservas() {
        if (reservas == null) {
            reservas = servicio.buscar(estado, texto);
        }
        return reservas;
    }
 
    public Map<String, Long> getConteo() {
        if (conteo == null) {
            conteo = servicio.contarPorEstado();
        }
        return conteo;
    }
 
    // ================= Filtros =================
 
    public void buscar() {
        historialDe = null;
        historial = new ArrayList<>();
        recargar();
    }
 
    public void limpiar() {
        estado = "TODOS";
        texto = "";
        buscar();
    }
 
    // ================= Cambios de estado =================
 
    public void confirmar(Integer idReserva) {
        cambiar(idReserva, "CONFIRMADA", "confirmada.");
    }
 
    public void cancelar(Integer idReserva) {
        cambiar(idReserva, "CANCELADA", "cancelada. Los cupos volvieron al paquete.");
    }
 
    public void finalizar(Integer idReserva) {
        cambiar(idReserva, "FINALIZADA", "finalizada.");
    }
 
    private void cambiar(Integer idReserva, String nuevoEstado, String resultado) {
        try {
            Reserva r = servicio.cambiarEstado(idReserva, nuevoEstado, usuarioActual());
            log.info("Reserva {} pasó a {} por {}", r.getCodigoUnico(), nuevoEstado, usuarioActual());
            mensaje(FacesMessage.SEVERITY_INFO, "Reserva " + r.getCodigoUnico() + " " + resultado);
        } catch (IllegalArgumentException e) {
            log.warn("Cambio de estado rechazado: {}", e.getMessage());
            mensaje(FacesMessage.SEVERITY_ERROR, e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error al cambiar el estado de la reserva {}", idReserva, e);
            mensaje(FacesMessage.SEVERITY_ERROR, "No se pudo completar la operación. Intenta nuevamente.");
        }
        recargar();
        if (historialDe != null) {
            historial = servicio.historial(historialDe);
        }
    }
 
    // ================= Historial =================
 
    public void alternarHistorial(Integer idReserva) {
        if (idReserva != null && idReserva.equals(historialDe)) {
            historialDe = null;
            historial = new ArrayList<>();
        } else {
            historialDe = idReserva;
            historial = servicio.historial(idReserva);
        }
    }
 
    // ================= Etiquetas para la página =================
 
    public String etiquetaEstado(String e) {
        if (e == null) {
            return "—";
        }
        return switch (e) {
            case "PENDIENTE" -> "Pendiente de confirmación";
            case "CONFIRMADA" -> "Confirmada";
            case "CANCELADA" -> "Cancelada";
            case "FINALIZADA" -> "Finalizada";
            default -> e;
        };
    }
 
    public String etiquetaPago(String p) {
        if (p == null) {
            return "—";
        }
        return switch (p) {
            case "PENDIENTE" -> "Pendiente";
            case "PARCIAL" -> "Parcial";
            case "COMPLETADO" -> "Completado";
            default -> p;
        };
    }
 
    public String etiquetaRol(String rol) {
        if (rol == null) {
            return "";
        }
        return switch (rol) {
            case "ADMIN" -> "Administrador";
            case "AGENTE" -> "Agente";
            case "CLIENTE" -> "Cliente";
            default -> rol;
        };
    }
 
    public String descripcionCambio(String anterior, String nuevo) {
        if (anterior == null || anterior.isBlank()) {
            return "Reserva registrada como " + etiquetaEstadoCorta(nuevo);
        }
        return "De " + etiquetaEstadoCorta(anterior) + " a " + etiquetaEstadoCorta(nuevo);
    }
 
    private String etiquetaEstadoCorta(String e) {
        if (e == null) {
            return "—";
        }
        return switch (e) {
            case "PENDIENTE" -> "Pendiente";
            case "CONFIRMADA" -> "Confirmada";
            case "CANCELADA" -> "Cancelada";
            case "FINALIZADA" -> "Finalizada";
            default -> e;
        };
    }
 
    // ================= Auxiliares =================
 
    private void recargar() {
        reservas = null;
        conteo = null;
    }
 
    private String usuarioActual() {
        return FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
    }
 
    private void mensaje(FacesMessage.Severity severidad, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidad, texto, null));
    }
 
    // ================= Getters y setters =================
 
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
 
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto == null ? "" : texto; }
 
    public Integer getHistorialDe() { return historialDe; }
 
    public List<HistorialEstadoReserva> getHistorial() { return historial; }
}
 