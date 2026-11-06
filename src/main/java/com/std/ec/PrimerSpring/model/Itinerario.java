package com.std.ec.PrimerSpring.model;
 
import java.time.LocalDate;
 
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
 
@Entity
@Table(name = "itinerarios")
public class Itinerario {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_itinerario")
    private Integer id;
 
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_reserva", nullable = false, unique = true)
    private Reserva reserva;
 
    @Column(nullable = false)
    private String origen;
 
    @Column(nullable = false)
    private String destino;
 
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;
 
    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;
 
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
 
    public Reserva getReserva() { return reserva; }
    public void setReserva(Reserva reserva) { this.reserva = reserva; }
 
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
 
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
 
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
 
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
}
 