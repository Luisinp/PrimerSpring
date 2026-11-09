package com.std.ec.PrimerSpring.model;
 
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
 
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
 
/**
 * Vuelo internacional que ofrece la agencia (CU-05). La información (aerolínea, tarifa y
 * disponibilidad) la entrega el proveedor y se registra manualmente.
 */
@Entity
@Table(name = "vuelos")
public class Vuelo {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vuelo")
    private Integer id;
 
    @Column(nullable = false)
    private String aerolinea;
 
    @Column(nullable = false)
    private String origen;
 
    @Column(nullable = false)
    private String destino;
 
    @Column(nullable = false)
    private LocalDate fecha;
 
    @Column(nullable = false)
    private LocalTime hora;
 
    // Tarifa por pasajero
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tarifa;
 
    @Column(name = "asientos_disponibles", nullable = false)
    private Integer asientosDisponibles;
 
    @Column(nullable = false)
    private Boolean activo = true;
 
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
 
    public String getAerolinea() { return aerolinea; }
    public void setAerolinea(String aerolinea) { this.aerolinea = aerolinea; }
 
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
 
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
 
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
 
    public LocalTime getHora() { return hora; }
    public void setHora(LocalTime hora) { this.hora = hora; }
 
    public BigDecimal getTarifa() { return tarifa; }
    public void setTarifa(BigDecimal tarifa) { this.tarifa = tarifa; }
 
    public Integer getAsientosDisponibles() { return asientosDisponibles; }
    public void setAsientosDisponibles(Integer asientosDisponibles) { this.asientosDisponibles = asientosDisponibles; }
 
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
 