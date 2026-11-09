package com.std.ec.PrimerSpring.controller;
 
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
 
import com.std.ec.PrimerSpring.model.Cliente;
import com.std.ec.PrimerSpring.model.PaqueteTuristico;
import com.std.ec.PrimerSpring.model.Reserva;
import com.std.ec.PrimerSpring.service.PasajeroDatos;
import com.std.ec.PrimerSpring.service.ReservaService;
 
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
 
/** Bean de operario/reserva-nueva.xhtml: el personal registra una reserva a nombre de un cliente. */
@Component("reservaPersonalBean")
@ViewScoped
public class ReservaPersonalBean {
 
    private static final Logger log = LoggerFactory.getLogger(ReservaPersonalBean.class);
    private static final int MAX_PASAJEROS = 10;
 
    private final ReservaService reservaService;
 
    // 1. Cliente
    private String textoCliente = "";
    private List<Cliente> resultadosCliente = new ArrayList<>();
    private boolean busquedaHecha;
    private Cliente clienteSeleccionado;
 
    // 2. Viaje
    private List<PaqueteTuristico> paquetes;
    private Integer idPaquete;
    private LocalDate fechaInicio;
    private String origen;
 
    // 3. Pasajeros
    private List<PasajeroDatos> pasajeros = new ArrayList<>(List.of(new PasajeroDatos()));
 
    // 4. Estado inicial
    private boolean confirmarDeInmediato = true;
 
    public ReservaPersonalBean(ReservaService reservaService) {
        this.reservaService = reservaService;
    }
 
    // ================= Cliente =================
 
    public void buscarCliente() {
        resultadosCliente = reservaService.buscarClientesActivos(textoCliente);
        busquedaHecha = true;
    }
 
    public void elegirCliente(Integer idCliente) {
        for (Cliente c : resultadosCliente) {
            if (c.getId().equals(idCliente)) {
                clienteSeleccionado = c;
                break;
            }
        }
        if (clienteSeleccionado == null) {
            error("No se pudo elegir el cliente. Busca de nuevo.");
            return;
        }
        resultadosCliente = new ArrayList<>();
        busquedaHecha = false;
 
        // El primer pasajero suele ser el propio cliente: se rellena solo si aún está vacío
        PasajeroDatos primero = pasajeros.get(0);
        boolean vacio = (primero.getNombre() == null || primero.getNombre().isBlank())
                && (primero.getDocumento() == null || primero.getDocumento().isBlank());
        if (vacio) {
            pasajeros.set(0, datosDe(clienteSeleccionado));
        }
    }
 
    public void cambiarCliente() {
        clienteSeleccionado = null;
        resultadosCliente = new ArrayList<>();
        busquedaHecha = false;
        textoCliente = "";
    }
 
    private PasajeroDatos datosDe(Cliente c) {
        PasajeroDatos d = new PasajeroDatos();
        d.setNombre(c.getNombreCompleto());
        d.setDocumento(c.getDocumentoIdentidad());
        d.setNacionalidad(c.getNacionalidad());
        d.setSexo(c.getSexo());
        d.setFechaNacimiento(c.getFechaNacimiento());
        return d;
    }
 
    // ================= Viaje =================
 
    public List<PaqueteTuristico> getPaquetes() {
        if (paquetes == null) {
            paquetes = reservaService.paquetesDisponibles();
        }
        return paquetes;
    }
 
    /** null si no se eligió paquete o ya no tiene cupos. */
    public PaqueteTuristico getPaqueteElegido() {
        if (idPaquete == null) {
            return null;
        }
        for (PaqueteTuristico p : getPaquetes()) {
            if (p.getId().equals(idPaquete)) {
                return p;
            }
        }
        return null;
    }
 
    public LocalDate getFechaMinima() {
        return LocalDate.now().plusDays(1);
    }
 
    public LocalDate getFechaFin() {
        PaqueteTuristico p = getPaqueteElegido();
        return (fechaInicio == null || p == null) ? null : fechaInicio.plusDays(p.getDuracionDias() - 1L);
    }
 
    /** Botón "Actualizar resumen". También es la acción de la tecla Enter. */
    public void actualizar() {
        if (clienteSeleccionado == null && textoCliente != null && !textoCliente.isBlank()) {
            buscarCliente();
        }
    }
 
    // ================= Pasajeros =================
 
    public void agregar() {
        if (pasajeros.size() < maximoPasajeros()) {
            pasajeros.add(new PasajeroDatos());
        } else {
            error("No puedes agregar más pasajeros: máximo " + maximoPasajeros() + " por reserva o por los cupos del paquete.");
        }
    }
 
    public void quitar(PasajeroDatos p) {
        if (pasajeros.size() > 1) {
            pasajeros.remove(p);
        }
    }
 
    public boolean isPuedeAgregar() {
        return pasajeros.size() < maximoPasajeros();
    }
 
    private int maximoPasajeros() {
        PaqueteTuristico p = getPaqueteElegido();
        if (p == null || p.getCuposDisponibles() == null) {
            return MAX_PASAJEROS;
        }
        return Math.max(1, Math.min(MAX_PASAJEROS, p.getCuposDisponibles()));
    }
 
    public BigDecimal getTotal() {
        PaqueteTuristico p = getPaqueteElegido();
        return p == null ? BigDecimal.ZERO : p.getPrecio().multiply(BigDecimal.valueOf(pasajeros.size()));
    }
 
    // ================= Registrar =================
 
    public String registrar() {
        if (clienteSeleccionado == null) {
            error("Elige el cliente de la reserva.");
            return null;
        }
        if (idPaquete == null) {
            error("Elige el paquete.");
            return null;
        }
        try {
            Reserva r = reservaService.reservarPorPersonal(usuarioActual(), clienteSeleccionado.getId(),
                    idPaquete, fechaInicio, origen, pasajeros, confirmarDeInmediato);
            log.info("Reserva {} registrada por {} a nombre del cliente {} ({} pasajero(s))",
                    r.getCodigoUnico(), usuarioActual(), clienteSeleccionado.getId(), pasajeros.size());
            return "/operario/reservas?faces-redirect=true&nueva=" + r.getCodigoUnico();
        } catch (IllegalArgumentException e) {
            log.warn("Reserva del personal rechazada: {}", e.getMessage());
            error(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error al registrar la reserva del personal", e);
            error("No se pudo registrar la reserva. Intenta nuevamente.");
        }
        paquetes = null;   // vuelve a leer los cupos actuales
        return null;
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
 
    public String getTextoCliente() { return textoCliente; }
    public void setTextoCliente(String textoCliente) { this.textoCliente = textoCliente == null ? "" : textoCliente; }
 
    public List<Cliente> getResultadosCliente() { return resultadosCliente; }
    public boolean isBusquedaHecha() { return busquedaHecha; }
    public Cliente getClienteSeleccionado() { return clienteSeleccionado; }
 
    public Integer getIdPaquete() { return idPaquete; }
    public void setIdPaquete(Integer idPaquete) { this.idPaquete = idPaquete; }
 
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
 
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
 
    public List<PasajeroDatos> getPasajeros() { return pasajeros; }
 
    public boolean isConfirmarDeInmediato() { return confirmarDeInmediato; }
    public void setConfirmarDeInmediato(boolean confirmarDeInmediato) { this.confirmarDeInmediato = confirmarDeInmediato; }
}
 