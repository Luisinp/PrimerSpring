package com.std.ec.PrimerSpring.service;
 
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;
 
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import com.std.ec.PrimerSpring.model.Agente;
import com.std.ec.PrimerSpring.model.Rol;
import com.std.ec.PrimerSpring.model.Usuario;
import com.std.ec.PrimerSpring.repository.AgenteRepository;
import com.std.ec.PrimerSpring.repository.RolRepository;
import com.std.ec.PrimerSpring.repository.UsuarioRepository;
 
/**
 * Gestión de agentes (CU-09). Solo el administrador accede a las páginas
 * que usan este servicio (regla /admin/** del SecurityConfig).
 */
@Service
public class AgenteService {
 
    // Sin caracteres ambiguos (0/O, 1/l/I)
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int LARGO_CLAVE = 10;
 
    private final SecureRandom random = new SecureRandom();
 
    private final AgenteRepository agenteRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
 
    public AgenteService(AgenteRepository agenteRepository,
                         UsuarioRepository usuarioRepository,
                         RolRepository rolRepository,
                         PasswordEncoder passwordEncoder) {
        this.agenteRepository = agenteRepository;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }
 
    /** estado: TODOS, ACTIVOS o INACTIVOS */
    @Transactional(readOnly = true)
    public List<Agente> buscar(String texto, String estado) {
        String t = texto == null ? "" : texto.trim();
        List<Boolean> estados;
        if ("ACTIVOS".equals(estado)) {
            estados = List.of(true);
        } else if ("INACTIVOS".equals(estado)) {
            estados = List.of(false);
        } else {
            estados = List.of(true, false);
        }
        return agenteRepository.buscar(t, estados);
    }
 
    /**
     * Crea el usuario (rol AGENTE) y el agente en una sola transacción.
     * Devuelve la contraseña temporal en texto: es la única vez que existe así;
     * en la base solo se guarda su hash BCrypt.
     */
    @Transactional
    public String crear(String nombre, String documento, String correo, String telefono,
                        String sexo, String nacionalidad,
                        LocalDate fechaNacimiento, LocalDate fechaIngreso) {
 
        String correoN = correo.trim().toLowerCase();
        String documentoN = documento.trim().toUpperCase();
 
        if (usuarioRepository.existsByCorreo(correoN)) {
            throw new IllegalArgumentException("Ese correo ya está registrado.");
        }
        if (agenteRepository.existsByDocumentoIdentidad(documentoN)) {
            throw new IllegalArgumentException("Ya existe un agente con ese documento.");
        }
        if (fechaNacimiento != null && !fechaNacimiento.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento no es válida.");
        }
 
        Rol rolAgente = rolRepository.findByNombre("AGENTE")
                .orElseThrow(() -> new IllegalStateException("Falta el rol AGENTE en la base de datos"));
 
        String clave = generarClave();
 
        Usuario usuario = new Usuario();
        usuario.setCorreo(correoN);
        usuario.setContrasenaCifrada(passwordEncoder.encode(clave));
        usuario.setActivo(true);
        usuario.setRol(rolAgente);
        usuarioRepository.save(usuario);
 
        Agente agente = new Agente();
        agente.setUsuario(usuario);
        agente.setNombreCompleto(nombre.trim());
        agente.setDocumentoIdentidad(documentoN);
        agente.setTelefono(vacioANulo(telefono));
        agente.setSexo(vacioANulo(sexo));
        agente.setNacionalidad(vacioANulo(nacionalidad));
        agente.setFechaNacimiento(fechaNacimiento);
        agente.setFechaIngreso(fechaIngreso != null ? fechaIngreso : LocalDate.now());
        agenteRepository.save(agente);
 
        return clave;
    }
 
    /** Activa o desactiva la cuenta. Devuelve el estado final (true = activo). */
    @Transactional
    public boolean cambiarEstado(Integer idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("El agente no existe."));
        usuario.setActivo(!Boolean.TRUE.equals(usuario.getActivo()));
        usuarioRepository.save(usuario);
        return usuario.getActivo();
    }
 
    /** Genera una contraseña temporal nueva y la devuelve (solo se muestra una vez). */
    @Transactional
    public String restablecerContrasena(Integer idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("El agente no existe."));
        String clave = generarClave();
        usuario.setContrasenaCifrada(passwordEncoder.encode(clave));
        usuarioRepository.save(usuario);
        return clave;
    }
 
    /** Actualiza los datos del agente. El correo (usuario del login) vive solo en usuarios. */
    @Transactional
    public void actualizar(Integer idUsuario, String nombre, String documento, String correo,
                           String telefono, String sexo, String nacionalidad,
                           LocalDate fechaNacimiento, LocalDate fechaIngreso) {
 
        Agente agente = agenteRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("El agente no existe."));
        Usuario usuario = agente.getUsuario();
 
        String correoN = correo.trim().toLowerCase();
        String documentoN = documento.trim().toUpperCase();
 
        if (!correoN.equalsIgnoreCase(usuario.getCorreo()) && usuarioRepository.existsByCorreo(correoN)) {
            throw new IllegalArgumentException("Ese correo ya está registrado.");
        }
        if (!documentoN.equals(agente.getDocumentoIdentidad())
                && agenteRepository.existsByDocumentoIdentidad(documentoN)) {
            throw new IllegalArgumentException("Ya existe un agente con ese documento.");
        }
        if (fechaNacimiento != null && !fechaNacimiento.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento no es válida.");
        }
 
        usuario.setCorreo(correoN);
        usuarioRepository.save(usuario);
 
        agente.setNombreCompleto(nombre.trim());
        agente.setDocumentoIdentidad(documentoN);
        agente.setTelefono(vacioANulo(telefono));
        agente.setSexo(vacioANulo(sexo));
        agente.setNacionalidad(vacioANulo(nacionalidad));
        agente.setFechaNacimiento(fechaNacimiento);
        agente.setFechaIngreso(fechaIngreso != null ? fechaIngreso : agente.getFechaIngreso());
        agenteRepository.save(agente);
    }
 
    /**
     * Elimina al agente y su cuenta solo si no tiene reservas, pagos ni movimientos.
     * Si los tiene, hay que desactivarlo para conservar el historial.
     */
    @Transactional
    public void eliminar(Integer idUsuario) {
        Agente agente = agenteRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("El agente no existe."));
 
        if (agenteRepository.contarVinculos(idUsuario) > 0) {
            throw new IllegalArgumentException(
                    "No se puede eliminar: tiene reservas, pagos o movimientos asociados. "
                    + "Desactívalo para conservar su historial.");
        }
 
        Usuario usuario = agente.getUsuario();
        agenteRepository.delete(agente);
        agenteRepository.flush();
        usuarioRepository.delete(usuario);
    }
 
    private String generarClave() {
        StringBuilder sb = new StringBuilder(LARGO_CLAVE);
        for (int i = 0; i < LARGO_CLAVE; i++) {
            sb.append(ALFABETO.charAt(random.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }
 
    private String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}