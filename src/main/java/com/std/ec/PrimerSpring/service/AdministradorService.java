package com.std.ec.PrimerSpring.service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.std.ec.PrimerSpring.model.Administrador;
import com.std.ec.PrimerSpring.model.Rol;
import com.std.ec.PrimerSpring.model.Usuario;
import com.std.ec.PrimerSpring.repository.AdministradorRepository;
import com.std.ec.PrimerSpring.repository.RolRepository;
import com.std.ec.PrimerSpring.repository.UsuarioRepository;

/**
 * Gestión de administradores (CU-09). Solo el administrador accede a las páginas que usan este
 * servicio (regla /admin/** de SecurityConfig). Errores de negocio: IllegalArgumentException.
 */
@Service
public class AdministradorService {

    // Sin caracteres ambiguos (0/O, 1/l/I)
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int LARGO_CLAVE = 10;

    private final SecureRandom random = new SecureRandom();

    private final AdministradorRepository administradorRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public AdministradorService(AdministradorRepository administradorRepository,
                                UsuarioRepository usuarioRepository,
                                RolRepository rolRepository,
                                PasswordEncoder passwordEncoder) {
        this.administradorRepository = administradorRepository;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** estado: TODOS, ACTIVOS o INACTIVOS */
    @Transactional(readOnly = true)
    public List<Administrador> buscar(String texto, String estado) {
        String t = texto == null ? "" : texto.trim();
        List<Boolean> estados;
        if ("ACTIVOS".equals(estado)) {
            estados = List.of(true);
        } else if ("INACTIVOS".equals(estado)) {
            estados = List.of(false);
        } else {
            estados = List.of(true, false);
        }
        return administradorRepository.buscar(t, estados);
    }

    /** Crea el usuario (rol ADMIN) y el administrador. Devuelve la contraseña temporal (solo se muestra una vez). */
    @Transactional
    public String crear(String nombre, String documento, String correo, String telefono,
                        String sexo, String nacionalidad,
                        LocalDate fechaNacimiento, LocalDate fechaIngreso) {

        validar(nombre, documento, correo, fechaNacimiento);
        String correoN = correo.trim().toLowerCase();
        String documentoN = documento.trim().toUpperCase();

        if (usuarioRepository.existsByCorreo(correoN)) {
            throw new IllegalArgumentException("Ese correo ya está registrado.");
        }
        if (administradorRepository.existsByDocumentoIdentidad(documentoN)) {
            throw new IllegalArgumentException("Ya existe un administrador con ese documento.");
        }

        Rol rol = rolRepository.findByNombre("ADMIN")
                .orElseThrow(() -> new IllegalStateException("Falta el rol ADMIN en la base de datos"));

        String clave = generarClave();

        Usuario usuario = new Usuario();
        usuario.setCorreo(correoN);
        usuario.setContrasenaCifrada(passwordEncoder.encode(clave));
        usuario.setActivo(true);
        usuario.setRol(rol);
        usuarioRepository.save(usuario);

        Administrador admin = new Administrador();
        admin.setUsuario(usuario);
        admin.setNombreCompleto(nombre.trim());
        admin.setDocumentoIdentidad(documentoN);
        admin.setTelefono(vacioANulo(telefono));
        admin.setSexo(vacioANulo(sexo));
        admin.setNacionalidad(vacioANulo(nacionalidad));
        admin.setFechaNacimiento(fechaNacimiento);
        admin.setFechaIngreso(fechaIngreso != null ? fechaIngreso : LocalDate.now());
        administradorRepository.save(admin);

        return clave;
    }

    /** Actualiza los datos. El correo (usuario del login) vive en usuarios; no se cambia el propio. */
    @Transactional
    public void actualizar(Integer idUsuario, String correoActual, String nombre, String documento,
                           String correo, String telefono, String sexo, String nacionalidad,
                           LocalDate fechaNacimiento, LocalDate fechaIngreso) {

        validar(nombre, documento, correo, fechaNacimiento);
        Administrador admin = administradorRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("El administrador no existe."));
        Usuario usuario = admin.getUsuario();

        String correoN = correo.trim().toLowerCase();
        String documentoN = documento.trim().toUpperCase();
        boolean correoCambia = !correoN.equalsIgnoreCase(usuario.getCorreo());

        if (correoCambia && usuario.getCorreo().equalsIgnoreCase(correoActual)) {
            throw new IllegalArgumentException("No puedes cambiar tu propio correo mientras tienes la sesión abierta.");
        }
        if (correoCambia && usuarioRepository.existsByCorreo(correoN)) {
            throw new IllegalArgumentException("Ese correo ya está registrado.");
        }
        if (!documentoN.equals(admin.getDocumentoIdentidad())
                && administradorRepository.existsByDocumentoIdentidad(documentoN)) {
            throw new IllegalArgumentException("Ya existe un administrador con ese documento.");
        }

        usuario.setCorreo(correoN);
        usuarioRepository.save(usuario);

        admin.setNombreCompleto(nombre.trim());
        admin.setDocumentoIdentidad(documentoN);
        admin.setTelefono(vacioANulo(telefono));
        admin.setSexo(vacioANulo(sexo));
        admin.setNacionalidad(vacioANulo(nacionalidad));
        admin.setFechaNacimiento(fechaNacimiento);
        admin.setFechaIngreso(fechaIngreso != null ? fechaIngreso : admin.getFechaIngreso());
        administradorRepository.save(admin);
    }

    /** Activa o desactiva la cuenta. Devuelve el estado final (true = activo). */
    @Transactional
    public boolean cambiarEstado(Integer idUsuario, String correoActual) {
        Administrador admin = administradorRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("El administrador no existe."));
        Usuario usuario = admin.getUsuario();
        boolean activo = Boolean.TRUE.equals(usuario.getActivo());

        if (activo) {
            if (usuario.getCorreo().equalsIgnoreCase(correoActual)) {
                throw new IllegalArgumentException("No puedes desactivar tu propia cuenta.");
            }
            if (administradorRepository.contarActivos() <= 1) {
                throw new IllegalArgumentException("Debe quedar al menos un administrador activo.");
            }
        }
        usuario.setActivo(!activo);
        usuarioRepository.save(usuario);
        return usuario.getActivo();
    }

    /** Genera una contraseña temporal nueva y la devuelve (solo se muestra una vez). */
    @Transactional
    public String restablecerContrasena(Integer idUsuario) {
        Administrador admin = administradorRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("El administrador no existe."));
        Usuario usuario = admin.getUsuario();
        String clave = generarClave();
        usuario.setContrasenaCifrada(passwordEncoder.encode(clave));
        usuarioRepository.save(usuario);
        return clave;
    }

    /** Elimina al administrador y su cuenta solo si no tiene registros asociados; si los tiene, se desactiva. */
    @Transactional
    public void eliminar(Integer idUsuario, String correoActual) {
        Administrador admin = administradorRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("El administrador no existe."));
        Usuario usuario = admin.getUsuario();

        if (usuario.getCorreo().equalsIgnoreCase(correoActual)) {
            throw new IllegalArgumentException("No puedes eliminar tu propia cuenta.");
        }
        if (Boolean.TRUE.equals(usuario.getActivo()) && administradorRepository.contarActivos() <= 1) {
            throw new IllegalArgumentException("Debe quedar al menos un administrador activo.");
        }
        if (administradorRepository.contarVinculos(idUsuario) > 0) {
            throw new IllegalArgumentException(
                    "No se puede eliminar: tiene reservas, pagos, reportes o movimientos asociados. "
                    + "Desactívalo para conservar su historial.");
        }

        administradorRepository.delete(admin);
        administradorRepository.flush();
        usuarioRepository.delete(usuario);
    }

    // ---------------- Auxiliares ----------------

    private void validar(String nombre, String documento, String correo, LocalDate fechaNacimiento) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }
        if (documento == null || documento.isBlank()) {
            throw new IllegalArgumentException("El documento es obligatorio.");
        }
        if (correo == null || !correo.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("El correo no tiene un formato válido.");
        }
        if (nombre.trim().length() > 120 || documento.trim().length() > 30 || correo.trim().length() > 100) {
            throw new IllegalArgumentException("El nombre, el documento o el correo es demasiado largo.");
        }
        if (fechaNacimiento != null && !fechaNacimiento.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento no es válida.");
        }
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