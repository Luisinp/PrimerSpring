package com.std.ec.PrimerSpring.controller;
 
import java.util.List;
 
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
 
import com.std.ec.PrimerSpring.model.Destino;
import com.std.ec.PrimerSpring.model.PaqueteTuristico;
import com.std.ec.PrimerSpring.service.CatalogoService;
 
/**
 * Bean del catálogo público (home.xhtml) y del buscador de la barra superior.
 * Los filtros viajan en la dirección (?q=...&destino=...), así se pueden compartir.
 */
@Component("catalogoBean")
@Scope("request")
public class CatalogoBean {
 
    private final CatalogoService catalogoService;
 
    private String q;           // texto de búsqueda (viene de la dirección o de la barra)
    private Integer destino;    // id del destino elegido (viene de la dirección)
 
    private List<PaqueteTuristico> paquetes;
    private List<Destino> destinos;
 
    public CatalogoBean(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }
 
    public List<PaqueteTuristico> getPaquetes() {
        if (paquetes == null) {
            paquetes = catalogoService.catalogo(destino == null ? 0 : destino, q);
        }
        return paquetes;
    }
 
    public List<Destino> getDestinos() {
        if (destinos == null) {
            destinos = catalogoService.destinosConPaquetes();
        }
        return destinos;
    }
 
    /** Lo llama el buscador de la barra: va al catálogo conservando el texto escrito. */
    public String buscar() {
        return "/home.xhtml?faces-redirect=true&includeViewParams=true";
    }
 
    public boolean isConBusqueda() {
        return q != null && !q.isBlank();
    }
 
    public String getQ() { return q; }
    public void setQ(String q) { this.q = q; }
 
    public Integer getDestino() { return destino; }
    public void setDestino(Integer destino) { this.destino = destino; }
}
 