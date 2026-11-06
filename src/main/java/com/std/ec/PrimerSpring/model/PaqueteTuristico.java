package com.std.ec.PrimerSpring.model;
 
import java.math.BigDecimal;
 
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
 
@Entity
@Table(name = "paquetes_turisticos")
public class PaqueteTuristico {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_paquete")
    private Integer id;
 
    @Column(nullable = false)
    private String nombre;
 
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_destino", nullable = false)
    private Destino destino;
 
    @Column(name = "duracion_dias", nullable = false)
    private Integer duracionDias;
 
    @Column(columnDefinition = "TEXT")
    private String descripcion;
 
    @Column(name = "servicios_incluidos", columnDefinition = "TEXT")
    private String serviciosIncluidos;
 
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;
 
    @Column(name = "cupos_disponibles", nullable = false)
    private Integer cuposDisponibles = 0;
 
    // Nombre del archivo de la foto (la carpeta la define travelzen.uploads.dir)
    @Column(name = "imagen_portada")
    private String imagenPortada;
 
    // publicado = aparece en el catálogo público
    @Column(nullable = false)
    private Boolean publicado = false;
 
    // activo = false significa retirado (no se publica ni se reserva)
    @Column(nullable = false)
    private Boolean activo = true;
 
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
 
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
 
    public Destino getDestino() { return destino; }
    public void setDestino(Destino destino) { this.destino = destino; }
 
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
 
    public String getImagenPortada() { return imagenPortada; }
    public void setImagenPortada(String imagenPortada) { this.imagenPortada = imagenPortada; }
 
    public Boolean getPublicado() { return publicado; }
    public void setPublicado(Boolean publicado) { this.publicado = publicado; }
 
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
 