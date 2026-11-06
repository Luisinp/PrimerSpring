package com.std.ec.PrimerSpring.service;
 
import java.time.LocalDate;
import java.util.List;
 
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import com.std.ec.PrimerSpring.model.Cliente;
import com.std.ec.PrimerSpring.model.Usuario;
import com.std.ec.PrimerSpring.repository.ClienteRepository;
import com.std.ec.PrimerSpring.repository.UsuarioRepository;
 
/**
 * Gestión de clientes (CU-04). La usan el agente y el administrador.
 * Los errores de negocio se lanzan como IllegalArgumentException con un
 * mensaje apto para mostrar al usuario.
 */
@Service
public class ClienteService {
 
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
 
    public ClienteService(ClienteRepository clienteRepository, UsuarioRepository usuarioRepository) {
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
    }
 
    /** estado: TODOS, ACTIVOS, INACTIVOS. cuenta: TODOS, CON, SIN. */
    @Transactional(readOnly = true)
    public List<Cliente> buscar(String texto, String estado, String cuenta) {
        String t = texto == null ? "" : texto.trim();
        List<Boolean> estados;
        if ("ACTIVOS".equals(estado)) {
            estados = List.of(true);
        } else if ("INACTIVOS".equals(estado)) {
            estados = List.of(false);
        } else {
            estados = List.of(true, false);
        }
        String c = "CON".equals(cuenta) || "SIN".equals(cuenta) ? cuenta : "TODOS";
        return clienteRepository.buscar(t, estados, c);
    }
 
    /** Registro en mostrador: no crea cuenta web. */
    @Transactional
    public void crear(String nombre, String documento, String correo, String telefono,
                      String sexo, String nacionalidad, LocalDate fechaNacimiento) {
 
        String documentoN = documento.trim().toUpperCase();
        if (clienteRepository.findByDocumentoIdentidad(documentoN).isPresent()) {
            throw new IllegalArgumentException("Ese documento ya está registrado como cliente.");
        }
        validarFecha(fechaNacimiento);
 
        Cliente c = new Cliente();
        c.setNombreCompleto(nombre.trim());
        c.setDocumentoIdentidad(documentoN);
        c.setCorreo(correo.trim().toLowerCase());
        c.setTelefono(vacioANulo(telefono));
        c.setSexo(vacioANulo(sexo));
        c.setNacionalidad(vacioANulo(nacionalidad));
        c.setFechaNacimiento(fechaNacimiento);
        c.setActivo(true);
        clienteRepository.save(c);
    }
 
    /**
     * Si el cliente tiene cuenta web, su correo NO se modifica aquí: ese correo
     * es también su usuario del login y debe seguir coincidiendo.
     */
    @Transactional
    public void actualizar(Integer id, String nombre, String documento, String correo, String telefono,
                           String sexo, String nacionalidad, LocalDate fechaNacimiento) {
 
        Cliente c = clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El cliente no existe."));
 
        String documentoN = documento.trim().toUpperCase();
        if (!documentoN.equals(c.getDocumentoIdentidad())
                && clienteRepository.findByDocumentoIdentidad(documentoN).isPresent()) {
            throw new IllegalArgumentException("Ese documento ya pertenece a otro cliente.");
        }
        validarFecha(fechaNacimiento);
 
        c.setNombreCompleto(nombre.trim());
        c.setDocumentoIdentidad(documentoN);
        if (c.getUsuario() == null && correo != null && !correo.isBlank()) {
            c.setCorreo(correo.trim().toLowerCase());
        }
        c.setTelefono(vacioANulo(telefono));
        c.setSexo(vacioANulo(sexo));
        c.setNacionalidad(vacioANulo(nacionalidad));
        c.setFechaNacimiento(fechaNacimiento);
        clienteRepository.save(c);
    }
 
    /** Devuelve el estado final (true = activo). Un cliente inactivo no puede hacer reservas nuevas. */
    @Transactional
    public boolean cambiarEstado(Integer id) {
        Cliente c = clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El cliente no existe."));
        c.setActivo(!Boolean.TRUE.equals(c.getActivo()));
        clienteRepository.save(c);
        return c.getActivo();
    }
 
    /**
     * Elimina solo si no tiene reservas ni movimientos. Si tiene cuenta web,
     * también se elimina esa cuenta (quedaría huérfana).
     */
    @Transactional
    public void eliminar(Integer id) {
        Cliente c = clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El cliente no existe."));
        Usuario u = c.getUsuario();
 
        long vinculos = clienteRepository.contarReservas(c.getId());
        if (u != null) {
            vinculos += clienteRepository.contarMovimientosUsuario(u.getId());
        }
        if (vinculos > 0) {
            throw new IllegalArgumentException(
                    "No se puede eliminar: tiene reservas o movimientos asociados. "
                    + "Desactívalo para conservar su historial.");
        }
 
        clienteRepository.delete(c);
        clienteRepository.flush();
        if (u != null) {
            usuarioRepository.delete(u);
        }
    }
 
    private void validarFecha(LocalDate fechaNacimiento) {
        if (fechaNacimiento != null && !fechaNacimiento.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento no es válida.");
        }
    }
 
    private String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
 