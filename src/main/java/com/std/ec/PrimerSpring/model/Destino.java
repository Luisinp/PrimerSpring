package com.std.ec.PrimerSpring.model;
 
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
 
@Entity
@Table(name = "destinos")
public class Destino {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_destino")
    private Integer id;
 
    @Column(nullable = false)
    private String nombre;
 
    @Column(nullable = false)
    private String pais;
 
    @Column(columnDefinition = "TEXT")
    private String descripcion;
 
    @Column(nullable = false)
    private Boolean activo = true;
 
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
 
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
 
    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }
 
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
 
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
 