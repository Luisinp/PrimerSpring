package com.std.ec.PrimerSpring.model;
 
import java.math.BigDecimal;
import java.time.LocalDateTime;
 
import org.hibernate.annotations.Immutable;
 
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
 
/**
 * Comprobante interno de un pago, con número correlativo único. Sin validez fiscal.
 * Una vez emitido no se modifica (también lo protege un trigger en la base de datos).
 */
@Entity
@Immutable
@Table(name = "comprobantes")
public class Comprobante {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comprobante")
    private Integer id;
 
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pago", nullable = false, unique = true)
    private Pago pago;
 
    @Column(name = "numero_correlativo", nullable = false, unique = true)
    private Integer numeroCorrelativo;
 
    // La fecha la pone la base de datos (DEFAULT CURRENT_TIMESTAMP)
    @Column(name = "fecha_emision", insertable = false, updatable = false)
    private LocalDateTime fechaEmision;
 
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal importe;
 
    @Column(name = "saldo_restante", nullable = false, precision = 10, scale = 2)
    private BigDecimal saldoRestante;
 
    private String concepto;
 
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
 
    public Pago getPago() { return pago; }
    public void setPago(Pago pago) { this.pago = pago; }
 
    public Integer getNumeroCorrelativo() { return numeroCorrelativo; }
    public void setNumeroCorrelativo(Integer numeroCorrelativo) { this.numeroCorrelativo = numeroCorrelativo; }
 
    public LocalDateTime getFechaEmision() { return fechaEmision; }
 
    public BigDecimal getImporte() { return importe; }
    public void setImporte(BigDecimal importe) { this.importe = importe; }
 
    public BigDecimal getSaldoRestante() { return saldoRestante; }
    public void setSaldoRestante(BigDecimal saldoRestante) { this.saldoRestante = saldoRestante; }
 
    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }
}
 