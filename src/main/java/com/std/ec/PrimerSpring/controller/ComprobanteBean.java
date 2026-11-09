package com.std.ec.PrimerSpring.controller;
 
import org.springframework.stereotype.Component;
 
import com.std.ec.PrimerSpring.model.Comprobante;
import com.std.ec.PrimerSpring.service.PagoService;
 
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
 
/** Bean de operario/comprobante.xhtml: muestra un comprobante (?id=...) listo para imprimir. */
@Component("comprobanteBean")
@ViewScoped
public class ComprobanteBean {
 
    private final PagoService pagoService;
 
    private Comprobante comprobante;
    private boolean cargado;
    private String atendidoPor;
 
    public ComprobanteBean(PagoService pagoService) {
        this.pagoService = pagoService;
    }
 
    /** null si no existe o no se indicó el id. */
    public Comprobante getComprobante() {
        if (!cargado) {
            String valor = FacesContext.getCurrentInstance().getExternalContext()
                    .getRequestParameterMap().get("id");
            if (valor != null && valor.matches("\\d{1,9}")) {
                comprobante = pagoService.comprobante(Integer.valueOf(valor)).orElse(null);
                if (comprobante != null) {
                    atendidoPor = pagoService.atendidoPor(comprobante.getPago());
                }
            }
            cargado = true;
        }
        return comprobante;
    }
 
    /** Número con ceros a la izquierda, por ejemplo 000001. */
    public String getNumero() {
        Comprobante c = getComprobante();
        return c == null ? "" : String.format("%06d", c.getNumeroCorrelativo());
    }
 
    public Integer getIdReserva() {
        Comprobante c = getComprobante();
        return c == null ? null : c.getPago().getReserva().getId();
    }
 
    public String getAtendidoPor() {
        getComprobante();
        return atendidoPor == null ? "—" : atendidoPor;
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
}
 