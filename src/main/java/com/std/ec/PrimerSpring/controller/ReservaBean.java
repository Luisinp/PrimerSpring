package com.std.ec.PrimerSpring.controller;
 
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
 
import com.std.ec.PrimerSpring.model.PaqueteTuristico;
import com.std.ec.PrimerSpring.model.Reserva;
import com.std.ec.PrimerSpring.service.CatalogoService;
import com.std.ec.PrimerSpring.service.PasajeroDatos;
import com.std.ec.PrimerSpring.service.ReservaService;
 
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
 
/** Bean de reservar.xhtml: datos del viaje, lista de pasajeros y confirmación. */
@Component("reservaBean")
@ViewScoped
public class ReservaBean {
 
    private static final Logger log = LoggerFactory.getLogger(ReservaBean.class);
    private static final int MAX_PASAJEROS = 10;
 
    private final ReservaService reservaService;
    private final CatalogoService catalogoService;
 
    private Integer idPaquete;              // viene de la dirección (?id=...)
    private PaqueteTuristico paquete;
    private boolean paqueteCargado;
 
    private LocalDate fechaInicio;
    private String origen;
    private List<PasajeroDatos> pasajeros;
 
    public ReservaBean(ReservaService reservaService, CatalogoService catalogoService) {
        this.reservaService = reservaService;
        this.catalogoService = catalogoService;
    }
 
    // ================= Paquete =================
 
    /** null si no existe o ya no está publicado. */
    public PaqueteTuristico getPaquete() {
        if (!paqueteCargado) {
            paquete = catalogoService.detalle(idEfectivo()).orElse(null);
            paqueteCargado = true;
        }
        return paquete;
    }
 
    // ================= Pasajeros =================
 
    /** El primer pasajero se rellena con los datos del cliente (suele viajar él mismo). */
    public List<PasajeroDatos> getPasajeros() {
        if (pasajeros == null) {
            pasajeros = new ArrayList<>();
            pasajeros.add(reservaService.datosDelCliente(usuarioActual()));
        }
        return pasajeros;
    }
 
    public void agregar() {
        if (getPasajeros().size() < maximoPasajeros()) {
            pasajeros.add(new PasajeroDatos());
        } else {
            error("No puedes agregar más pasajeros: máximo " + maximoPasajeros() + " por los cupos disponibles.");
        }
    }
 
    public void quitar(PasajeroDatos p) {
        if (getPasajeros().size() > 1) {
            pasajeros.remove(p);
        }
    }
 
    public boolean isPuedeAgregar() {
        return getPasajeros().size() < maximoPasajeros();
    }
 
    private int maximoPasajeros() {
        PaqueteTuristico p = getPaquete();
        if (p == null || p.getCuposDisponibles() == null) {
            return 1;
        }
        return Math.max(1, Math.min(MAX_PASAJEROS, p.getCuposDisponibles()));
    }
 
    // ================= Cálculos para el resumen =================
 
    public BigDecimal getTotal() {
        PaqueteTuristico p = getPaquete();
        return p == null ? BigDecimal.ZERO : p.getPrecio().multiply(BigDecimal.valueOf(getPasajeros().size()));
    }
 
    public LocalDate getFechaMinima() {
        return LocalDate.now().plusDays(1);
    }
 
    public LocalDate getFechaFin() {
        PaqueteTuristico p = getPaquete();
        return (fechaInicio == null || p == null) ? null : fechaInicio.plusDays(p.getDuracionDias() - 1L);
    }
 
    // ================= Confirmar =================
 
    public String reservar() {
        try {
            Reserva r = reservaService.reservar(usuarioActual(), idEfectivo(), fechaInicio, origen, getPasajeros());
            return "/cliente/mis-reservas?faces-redirect=true&nueva=" + r.getCodigoUnico();
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error al registrar la reserva", e);
            error("No se pudo registrar la reserva. Intenta nuevamente.");
        }
        paqueteCargado = false;   // vuelve a leer los cupos actuales
        return null;
    }
 
    // ================= Auxiliares =================
 
    private String usuarioActual() {
        return FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
    }
 
    // Usa lo que dejó f:viewParam; si aún no llegó, lo lee de la dirección (?id=...)
    private Integer idEfectivo() {
        if (idPaquete != null) {
            return idPaquete;
        }
        String valor = FacesContext.getCurrentInstance().getExternalContext()
                .getRequestParameterMap().get("id");
        return valor != null && valor.matches("\\d{1,9}") ? Integer.valueOf(valor) : null;
    }
 
    private void error(String mensaje) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, mensaje, null));
    }
 
    // ================= Getters y setters =================
 
    public Integer getIdPaquete() { return idPaquete; }
    public void setIdPaquete(Integer idPaquete) { this.idPaquete = idPaquete; }
 
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
 
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
}
 