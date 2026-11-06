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
@Table(name = "clientes")
public class Cliente {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Integer id;
 
    @Column(name = "nombre_completo", nullable = false)
    private String nombreCompleto;
 
    @Column(name = "documento_identidad", nullable = false, unique = true)
    private String documentoIdentidad;
 
    @Column(nullable = false)
    private String correo;
 
    private String telefono;
 
    private String sexo;   // 'M' o 'F'
 
    private String nacionalidad;
 
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;
 
    @Column(nullable = false)
    private Boolean activo = true;
 
    // NULL = cliente registrado en mostrador, sin cuenta web
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", unique = true)
    private Usuario usuario;
 
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
 
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
 
    public String getDocumentoIdentidad() { return documentoIdentidad; }
    public void setDocumentoIdentidad(String documentoIdentidad) { this.documentoIdentidad = documentoIdentidad; }
 
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
 
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
 
    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }
 
    public String getNacionalidad() { return nacionalidad; }
    public void setNacionalidad(String nacionalidad) { this.nacionalidad = nacionalidad; }
 
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
 
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
 
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
 