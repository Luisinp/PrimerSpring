package com.std.ec.PrimerSpring.model;
 
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
 
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
 
/**
 * Reserva de un paquete (el tipo VUELO llega en un sprint posterior).
 * Estados: PENDIENTE, CONFIRMADA, CANCELADA, FINALIZADA.
 * Estado de pago: PENDIENTE, PARCIAL, COMPLETADO.
 */
@Entity
@Table(name = "reservas")
public class Reserva {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva")
    private Integer id;
 
    @Column(name = "codigo_unico", nullable = false, unique = true)
    private String codigoUnico;
 
    @Column(nullable = false)
    private String tipo;               // PAQUETE
 
    // La fecha la pone la base de datos (DEFAULT CURRENT_TIMESTAMP)
    @Column(name = "fecha_registro", insertable = false, updatable = false)
    private LocalDateTime fechaRegistro;
 
    @Column(name = "monto_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal;
 
    @Column(nullable = false)
    private String estado;
 
    @Column(name = "estado_pago", nullable = false)
    private String estadoPago;
 
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;
 
    // Quién la registró: el propio cliente o el personal
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario_registro", nullable = false)
    private Usuario registradoPor;
 
    // Quién la controla (solo personal; vacío mientras está pendiente)
    @Column(name = "id_agente")
    private Integer idAgente;
 
    @Column(name = "id_administrador")
    private Integer idAdministrador;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_paquete")
    private PaqueteTuristico paquete;
 
    @OneToOne(mappedBy = "reserva", cascade = CascadeType.ALL)
    private Itinerario itinerario;
 
    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL)
    private List<Pasajero> pasajeros = new ArrayList<>();
 
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
 
    public String getCodigoUnico() { return codigoUnico; }
    public void setCodigoUnico(String codigoUnico) { this.codigoUnico = codigoUnico; }
 
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
 
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
 
    public BigDecimal getMontoTotal() { return montoTotal; }
    public void setMontoTotal(BigDecimal montoTotal) { this.montoTotal = montoTotal; }
 
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
 
    public String getEstadoPago() { return estadoPago; }
    public void setEstadoPago(String estadoPago) { this.estadoPago = estadoPago; }
 
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
 
    public Usuario getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(Usuario registradoPor) { this.registradoPor = registradoPor; }
 
    public Integer getIdAgente() { return idAgente; }
    public void setIdAgente(Integer idAgente) { this.idAgente = idAgente; }
 
    public Integer getIdAdministrador() { return idAdministrador; }
    public void setIdAdministrador(Integer idAdministrador) { this.idAdministrador = idAdministrador; }
 
    public PaqueteTuristico getPaquete() { return paquete; }
    public void setPaquete(PaqueteTuristico paquete) { this.paquete = paquete; }
 
    public Itinerario getItinerario() { return itinerario; }
    public void setItinerario(Itinerario itinerario) { this.itinerario = itinerario; }
 
    public List<Pasajero> getPasajeros() { return pasajeros; }
    public void setPasajeros(List<Pasajero> pasajeros) { this.pasajeros = pasajeros; }
}
 
