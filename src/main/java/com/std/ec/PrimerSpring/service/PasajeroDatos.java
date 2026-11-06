package com.std.ec.PrimerSpring.service;
 
import java.time.LocalDate;
 
/** Datos de un pasajero tal como se escriben en el formulario de reserva. */
public class PasajeroDatos {
 
    private String nombre;
    private String documento;
    private String nacionalidad;
    private String sexo;
    private LocalDate fechaNacimiento;
 
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
 
    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }
 
    public String getNacionalidad() { return nacionalidad; }
    public void setNacionalidad(String nacionalidad) { this.nacionalidad = nacionalidad; }
 
    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }
 
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
}
 