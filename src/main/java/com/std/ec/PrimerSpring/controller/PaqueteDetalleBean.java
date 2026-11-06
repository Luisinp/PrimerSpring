package com.std.ec.PrimerSpring.controller;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
 
import com.std.ec.PrimerSpring.model.PaqueteTuristico;
import com.std.ec.PrimerSpring.service.CatalogoService;
 
import jakarta.faces.context.FacesContext;
 
/** Bean de paquete.xhtml y reservar.xhtml: carga el paquete indicado en ?id=... */
@Component("detalleBean")
@Scope("request")
public class PaqueteDetalleBean {
 
    private static final Logger log = LoggerFactory.getLogger(PaqueteDetalleBean.class);
 
    private final CatalogoService catalogoService;
 
    private Integer id;
    private PaqueteTuristico paquete;
    private boolean cargado;
 
    public PaqueteDetalleBean(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }
 
    /** null si no se indicó el paquete, si no existe o si ya no está publicado. */
    public PaqueteTuristico getPaquete() {
        if (!cargado) {
            Integer idEfectivo = idEfectivo();
            paquete = catalogoService.detalle(idEfectivo).orElse(null);
            if (paquete == null) {
                log.warn("Paquete no disponible para mostrar. id={}", idEfectivo);
            }
            cargado = true;
        }
        return paquete;
    }
 
    /** true si la dirección no trae un id válido (distinto de "paquete no disponible"). */
    public boolean isSinId() {
        return idEfectivo() == null;
    }
 
    public boolean isDisponible() {
        PaqueteTuristico p = getPaquete();
        return p != null && p.getCuposDisponibles() != null && p.getCuposDisponibles() > 0;
    }
 
    // Usa el valor que dejó f:viewParam; si por el orden del ciclo de vida de JSF
    // todavía no llegó, lo lee directamente de la dirección (?id=...).
    private Integer idEfectivo() {
        if (id != null) {
            return id;
        }
        FacesContext contexto = FacesContext.getCurrentInstance();
        if (contexto == null) {
            return null;
        }
        String valor = contexto.getExternalContext().getRequestParameterMap().get("id");
        if (valor != null && valor.matches("\\d{1,9}")) {
            return Integer.valueOf(valor);
        }
        return null;
    }
 
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
}
 