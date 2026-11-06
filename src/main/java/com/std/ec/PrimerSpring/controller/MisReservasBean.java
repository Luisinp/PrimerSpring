package com.std.ec.PrimerSpring.controller;
 
import java.util.List;
 
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
 
import com.std.ec.PrimerSpring.model.Reserva;
import com.std.ec.PrimerSpring.service.ReservaService;
 
import jakarta.faces.context.FacesContext;
 
/** Bean de cliente/mis-reservas.xhtml: solo lectura (CU-03). */
@Component("misReservasBean")
@Scope("request")
public class MisReservasBean {
 
    private final ReservaService reservaService;
 
    private List<Reserva> reservas;
 
    public MisReservasBean(ReservaService reservaService) {
        this.reservaService = reservaService;
    }
 
    public List<Reserva> getReservas() {
        if (reservas == null) {
            String correo = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
            reservas = reservaService.misReservas(correo);
        }
        return reservas;
    }
}
 