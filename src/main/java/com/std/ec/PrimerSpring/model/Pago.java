package com.std.ec.PrimerSpring.model;
 
import java.math.BigDecimal;
import java.time.LocalDate;
 
import org.hibernate.annotations.Immutable;
 
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
 
/**
 * Pago total o parcial de una reserva. Lo registra un agente o un administrador (nunca los dos).
 * Un pago registrado no se edita.
 */
@Entity
@Immutable
@Table(name = "pagos")
public class Pago {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Integer id;
 
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_reserva", nullable = false)
    private Reserva reserva;
 
    // Quién lo registró: el id es el id_usuario del agente o del administrador
    @Column(name = "id_agente")
    private Integer idAgente;
 
    @Column(name = "id_administrador")
    private Integer idAdministrador;
 
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;
 
    @Column(nullable = false)
    private LocalDate fecha;
 
    @Column(nullable = false)
    private String metodo;          // EFECTIVO o TRANSFERENCIA
 
    private String concepto;
 
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
 
    public Reserva getReserva() { return reserva; }
    public void setReserva(Reserva reserva) { this.reserva = reserva; }
 
    public Integer getIdAgente() { return idAgente; }
    public void setIdAgente(Integer idAgente) { this.idAgente = idAgente; }
 
    public Integer getIdAdministrador() { return idAdministrador; }
    public void setIdAdministrador(Integer idAdministrador) { this.idAdministrador = idAdministrador; }
 
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
 
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
 
    public String getMetodo() { return metodo; }
    public void setMetodo(String metodo) { this.metodo = metodo; }
 
    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }
}
 