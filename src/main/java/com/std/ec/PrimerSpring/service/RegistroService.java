package com.std.ec.PrimerSpring.service;
 
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.std.ec.PrimerSpring.model.Cliente;
import com.std.ec.PrimerSpring.model.Rol;
import com.std.ec.PrimerSpring.model.Usuario;
import com.std.ec.PrimerSpring.repository.ClienteRepository;
import com.std.ec.PrimerSpring.repository.RolRepository;
import com.std.ec.PrimerSpring.repository.UsuarioRepository;
 
/**
 * Registro de un cliente desde la web.
 * Crea el usuario (rol CLIENTE) y el cliente enlazado en una sola transacción:
 * si algo falla, no queda un usuario sin cliente ni al revés.
 */
@Service
public class RegistroService {
 
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
 
    public RegistroService(UsuarioRepository usuarioRepository,
                           ClienteRepository clienteRepository,
                           RolRepository rolRepository,
                           PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }
 
    /** Lanza IllegalArgumentException con un mensaje apto para mostrar al usuario. */
    @Transactional
    public void registrarCliente(String nombre, String documento, String correo,
                                 String telefono, String nacionalidad, String contrasena) {
 
        String correoN = correo.trim().toLowerCase();
        String documentoN = documento.trim().toUpperCase();
 
        if (usuarioRepository.existsByCorreo(correoN)) {
            throw new IllegalArgumentException("Ese correo ya está registrado. Inicia sesión.");
        }
 
        Cliente cliente = clienteRepository.findByDocumentoIdentidad(documentoN).orElse(null);
 
        if (cliente == null) {
            // Cliente nuevo
            cliente = new Cliente();
            cliente.setNombreCompleto(nombre.trim());
            cliente.setDocumentoIdentidad(documentoN);
            cliente.setCorreo(correoN);
            cliente.setTelefono(telefono == null || telefono.isBlank() ? null : telefono.trim());
            cliente.setNacionalidad(nacionalidad.trim());
            cliente.setActivo(true);
        } else {
            // El agente ya lo registró en mostrador: solo se le asigna la cuenta
            if (cliente.getUsuario() != null) {
                throw new IllegalArgumentException("Ese documento ya tiene una cuenta. Inicia sesión.");
            }
            if (!Boolean.TRUE.equals(cliente.getActivo())) {
                throw new IllegalArgumentException("Tu registro está inactivo. Acércate a la agencia.");
            }
            if (!correoN.equalsIgnoreCase(cliente.getCorreo())) {
                throw new IllegalArgumentException(
                        "Ese documento ya está registrado en la agencia con otro correo. "
                        + "Acércate a la agencia para vincular tu cuenta.");
            }
        }
 
        Rol rolCliente = rolRepository.findByNombre("CLIENTE")
                .orElseThrow(() -> new IllegalStateException("Falta el rol CLIENTE en la base de datos"));
 
        Usuario usuario = new Usuario();
        usuario.setCorreo(correoN);
        usuario.setContrasenaCifrada(passwordEncoder.encode(contrasena));
        usuario.setActivo(true);
        usuario.setRol(rolCliente);
        usuarioRepository.save(usuario);
 
        cliente.setUsuario(usuario);
        clienteRepository.save(cliente);
    }
}