package com.std.ec.PrimerSpring.controller;
 
import java.math.BigDecimal;
import java.util.List;
 
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
 
import com.std.ec.PrimerSpring.model.Destino;
import com.std.ec.PrimerSpring.model.PaqueteTuristico;
import com.std.ec.PrimerSpring.service.DestinoService;
import com.std.ec.PrimerSpring.service.PaqueteService;
 
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
 
/** Bean de operario/paquetes.xhtml (lista + ventana modal para crear y editar, con foto). */
@Component("paqueteBean")
@ViewScoped
public class PaqueteBean {
 
    private static final Logger log = LoggerFactory.getLogger(PaqueteBean.class);
    private static final long MAX_IMAGEN = 5L * 1024 * 1024;
 
    private final PaqueteService paqueteService;
    private final DestinoService destinoService;
 
    // ---- Lista y filtros ----
    private String filtroTexto = "";
    private String filtroEstado = "TODOS";
    private String filtroPublicacion = "TODOS";
    private List<PaqueteTuristico> paquetes;
    private List<Destino> destinos;      // opciones del selector de destino
 
    // ---- Formulario (ventana modal) ----
    private Integer idEdicion;           // null = paquete nuevo
    private String nombre;
    private Integer idDestino;
    private Integer duracionDias;
    private String descripcion;
    private String serviciosIncluidos;
    private BigDecimal precio;
    private Integer cuposDisponibles;
 
    // ---- Foto ----
    private String imagenActual;         // la que ya tiene guardada el paquete
    private byte[] imagenBytes;          // la recién elegida (se guarda al pulsar el botón)
    private String imagenNombre;
 
    public PaqueteBean(PaqueteService paqueteService, DestinoService destinoService) {
        this.paqueteService = paqueteService;
        this.destinoService = destinoService;
    }
 
    // ================= Lista =================
 
    public List<PaqueteTuristico> getPaquetes() {
        if (paquetes == null) {
            paquetes = paqueteService.buscar(filtroTexto, filtroEstado, filtroPublicacion);
        }
        return paquetes;
    }
 
    public List<Destino> getDestinos() {
        if (destinos == null) {
            destinos = destinoService.listarActivos();
        }
        return destinos;
    }
 
    public void filtrar() {
        paquetes = null;
    }
 
    public void limpiar() {
        filtroTexto = "";
        filtroEstado = "TODOS";
        filtroPublicacion = "TODOS";
        paquetes = null;
    }
 
    public void cambiarPublicacion(PaqueteTuristico p) {
        try {
            boolean publicado = paqueteService.cambiarPublicacion(p.getId());
            info("El paquete " + p.getNombre() + (publicado
                    ? " ahora aparece en el catálogo." : " ya no aparece en el catálogo."));
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error al publicar el paquete", e);
            error("No se pudo cambiar la publicación. Intenta nuevamente.");
        }
        paquetes = null;
    }
 
    public void cambiarEstado(PaqueteTuristico p) {
        try {
            boolean activo = paqueteService.cambiarEstado(p.getId());
            info("El paquete " + p.getNombre() + (activo ? " fue activado." : " fue desactivado y ocultado del catálogo."));
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error al cambiar el estado del paquete", e);
            error("No se pudo cambiar el estado. Intenta nuevamente.");
        }
        paquetes = null;
    }
 
    public void eliminar(PaqueteTuristico p) {
        try {
            paqueteService.eliminar(p.getId());
            info("El paquete " + p.getNombre() + " fue eliminado.");
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error al eliminar el paquete", e);
            error("No se pudo eliminar. Intenta nuevamente.");
        }
        paquetes = null;
    }
 
    // ================= Ventana crear / editar =================
 
    public void nuevo() {
        idEdicion = null;
        destinos = null;   // vuelve a leer los destinos activos
        nombre = null;
        idDestino = null;
        duracionDias = null;
        descripcion = null;
        serviciosIncluidos = null;
        precio = null;
        cuposDisponibles = 0;
        imagenActual = null;
        limpiarImagenElegida();
    }
 
    public void editar(PaqueteTuristico p) {
        idEdicion = p.getId();
        destinos = null;
        nombre = p.getNombre();
        idDestino = p.getDestino().getId();
        duracionDias = p.getDuracionDias();
        descripcion = p.getDescripcion();
        serviciosIncluidos = p.getServiciosIncluidos();
        precio = p.getPrecio();
        cuposDisponibles = p.getCuposDisponibles();
        imagenActual = p.getImagenPortada();
        limpiarImagenElegida();
    }
 
    /** Se ejecuta cuando el usuario elige una foto (auto-subida de PrimeFaces). */
    public void subirImagen(FileUploadEvent event) {
        UploadedFile archivo = event.getFile();
        if (archivo.getSize() > MAX_IMAGEN) {
            error("La imagen supera los 5 MB.");
            return;
        }
        imagenBytes = archivo.getContent();
        imagenNombre = archivo.getFileName();
    }
 
    public void guardar() {
        try {
            if (idEdicion == null) {
                paqueteService.crear(nombre, idDestino, duracionDias, descripcion, serviciosIncluidos,
                        precio, cuposDisponibles, imagenBytes, imagenNombre);
                info("El paquete " + nombre.trim() + " fue creado como borrador. "
                        + "Pulsa Publicar para que aparezca en el catálogo.");
            } else {
                paqueteService.actualizar(idEdicion, nombre, idDestino, duracionDias, descripcion,
                        serviciosIncluidos, precio, cuposDisponibles, imagenBytes, imagenNombre);
                info("El paquete " + nombre.trim() + " fue actualizado.");
            }
            paquetes = null;
            limpiarImagenElegida();
            PrimeFaces.current().executeScript("PF('dlgPaquete').hide()");
            PrimeFaces.current().ajax().update("frm:msgs", "frm:tabla");
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error al guardar el paquete", e);
            error("No se pudo guardar. Intenta nuevamente.");
        }
    }
 
    private void limpiarImagenElegida() {
        imagenBytes = null;
        imagenNombre = null;
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
 
    public String getFiltroTexto() { return filtroTexto; }
    public void setFiltroTexto(String filtroTexto) { this.filtroTexto = filtroTexto; }
 
    public String getFiltroEstado() { return filtroEstado; }
    public void setFiltroEstado(String filtroEstado) { this.filtroEstado = filtroEstado; }
 
    public String getFiltroPublicacion() { return filtroPublicacion; }
    public void setFiltroPublicacion(String filtroPublicacion) { this.filtroPublicacion = filtroPublicacion; }
 
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
 
    public Integer getIdDestino() { return idDestino; }
    public void setIdDestino(Integer idDestino) { this.idDestino = idDestino; }
 
    public Integer getDuracionDias() { return duracionDias; }
    public void setDuracionDias(Integer duracionDias) { this.duracionDias = duracionDias; }
 
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
 
    public String getServiciosIncluidos() { return serviciosIncluidos; }
    public void setServiciosIncluidos(String serviciosIncluidos) { this.serviciosIncluidos = serviciosIncluidos; }
 
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
 
    public Integer getCuposDisponibles() { return cuposDisponibles; }
    public void setCuposDisponibles(Integer cuposDisponibles) { this.cuposDisponibles = cuposDisponibles; }
 
    public String getImagenActual() { return imagenActual; }
 
    public String getImagenNombre() { return imagenNombre; }
}
 