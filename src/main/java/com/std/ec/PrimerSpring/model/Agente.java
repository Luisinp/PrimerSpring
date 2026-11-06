package com.std.ec.PrimerSpring.model;
 
import java.time.LocalDate;
 
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
 
/**
 * Agente de viajes. Hereda de Usuario: su clave primaria es el mismo id_usuario
 * (PK y FK a la vez). El correo y la contraseña viven en la tabla usuarios.
 */
@Entity
@Table(name = "agentes")
public class Agente {
 
    @Id
    @Column(name = "id_usuario")
    private Integer id;
 
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;
 
    @Column(name = "nombre_completo", nullable = false)
    private String nombreCompleto;
 
    @Column(name = "documento_identidad", nullable = false, unique = true)
    private String documentoIdentidad;
 
    private String telefono;
 
    private String sexo;   // 'M' o 'F'
 
    private String nacionalidad;
 
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;
 
    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDate fechaIngreso;
 
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
 
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
 
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
 
    public String getDocumentoIdentidad() { return documentoIdentidad; }
    public void setDocumentoIdentidad(String documentoIdentidad) { this.documentoIdentidad = documentoIdentidad; }
 
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
 
    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }
 
    public String getNacionalidad() { return nacionalidad; }
    public void setNacionalidad(String nacionalidad) { this.nacionalidad = nacionalidad; }
 
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
 
    public LocalDate getFechaIngreso() { return fechaIngreso; }
    public void setFechaIngreso(LocalDate fechaIngreso) { this.fechaIngreso = fechaIngreso; }
}