package com.std.ec.PrimerSpring.controller;
 
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
 
import com.std.ec.PrimerSpring.model.Comprobante;
import com.std.ec.PrimerSpring.model.Reserva;
import com.std.ec.PrimerSpring.service.PagoService;
 
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
 
/** Bean de operario/pagos.xhtml: pagos de una reserva (?id=...) y registro de uno nuevo. */
@Component("pagoBean")
@ViewScoped
public class PagoBean {
 
    private static final Logger log = LoggerFactory.getLogger(PagoBean.class);
 
    private final PagoService pagoService;
 
    // Reserva (llega en la dirección: ?id=...)
    private Integer idReserva;
    private Reserva reserva;
    private boolean reservaCargada;
    private BigDecimal saldo;
    private List<Comprobante> pagos;
 
    // Formulario del pago
    private BigDecimal monto;
    private String metodo = "EFECTIVO";
    private LocalDate fecha = LocalDate.now();
    private String concepto = "";
 
    public PagoBean(PagoService pagoService) {
        this.pagoService = pagoService;
    }
 
    // ================= Reserva =================
 
    public Integer getIdReserva() {
        if (idReserva == null) {
            String valor = FacesContext.getCurrentInstance().getExternalContext()
                    .getRequestParameterMap().get("id");
            if (valor != null && valor.matches("\\d{1,9}")) {
                idReserva = Integer.valueOf(valor);
            }
        }
        return idReserva;
    }
 
    /** null si no existe o no se indicó el id. */
    public Reserva getReserva() {
        if (!reservaCargada) {
            Integer id = getIdReserva();
            reserva = (id == null) ? null : pagoService.reserva(id).orElse(null);
            reservaCargada = true;
        }
        return reserva;
    }
 
    public BigDecimal getSaldo() {
        if (saldo == null) {
            Reserva r = getReserva();
            saldo = (r == null) ? BigDecimal.ZERO : pagoService.saldo(r.getId());
        }
        return saldo;
    }
 
    public BigDecimal getTotalPagado() {
        Reserva r = getReserva();
        return r == null ? BigDecimal.ZERO : r.getMontoTotal().subtract(getSaldo());
    }
 
    public List<Comprobante> getPagos() {
        if (pagos == null) {
            Reserva r = getReserva();
            pagos = (r == null) ? new ArrayList<>() : pagoService.pagosDeReserva(r.getId());
        }
        return pagos;
    }
 
    /** Se puede cobrar si la reserva está pendiente o confirmada y todavía tiene saldo. */
    public boolean isPuedePagar() {
        Reserva r = getReserva();
        if (r == null) {
            return false;
        }
        boolean activa = "PENDIENTE".equals(r.getEstado()) || "CONFIRMADA".equals(r.getEstado());
        return activa && getSaldo().compareTo(BigDecimal.ZERO) > 0;
    }
 
    public String getMotivoSinPago() {
        Reserva r = getReserva();
        if (r == null) {
            return "";
        }
        if ("CANCELADA".equals(r.getEstado())) {
            return "La reserva está cancelada y no admite pagos.";
        }
        if ("FINALIZADA".equals(r.getEstado())) {
            return "La reserva está finalizada y no admite pagos.";
        }
        return "La reserva ya está pagada por completo.";
    }
 
    public LocalDate getHoy() {
        return LocalDate.now();
    }
 
    // ================= Registrar =================
 
    public String registrar() {
        Integer id = getIdReserva();
        if (id == null) {
            error("No se indicó la reserva.");
            return null;
        }
        try {
            Comprobante c = pagoService.registrarPago(usuarioActual(), id, monto, metodo, fecha, concepto);
            log.info("Pago de USD {} registrado en la reserva {} (comprobante {}) por {}",
                    monto, id, c.getNumeroCorrelativo(), usuarioActual());
            return "/operario/comprobante?faces-redirect=true&id=" + c.getId();
        } catch (IllegalArgumentException e) {
            log.warn("Pago rechazado: {}", e.getMessage());
            error(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error al registrar el pago", e);
            error("No se pudo registrar el pago. Intenta nuevamente.");
        }
        // Vuelve a leer saldo y pagos actuales
        reservaCargada = false;
        saldo = null;
        pagos = null;
        return null;
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
 
    public String etiquetaMetodo(String m) {
        if (m == null) {
            return "—";
        }
        return switch (m) {
            case "EFECTIVO" -> "Efectivo";
            case "TRANSFERENCIA" -> "Transferencia";
            default -> m;
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
 
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
 
    public String getMetodo() { return metodo; }
    public void setMetodo(String metodo) { this.metodo = metodo; }
 
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
 
    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto == null ? "" : concepto; }
}
 