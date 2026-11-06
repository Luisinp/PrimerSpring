package com.std.ec.PrimerSpring.service;
 
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
 
import com.std.ec.PrimerSpring.model.Usuario;
import com.std.ec.PrimerSpring.repository.UsuarioRepository;
 
/**
 * Reemplaza a los usuarios en memoria: Spring Security llama a este
 * servicio con el correo que se escribe en el login y compara la
 * contraseña con el hash BCrypt guardado en la tabla usuarios.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {
 
    private final UsuarioRepository usuarioRepository;
 
    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }
 
    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario u = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("No existe el usuario: " + correo));
 
        return User.withUsername(u.getCorreo())
                .password(u.getContrasenaCifrada())
                .disabled(!Boolean.TRUE.equals(u.getActivo()))
                .authorities("ROLE_" + u.getRol().getNombre())   // ROLE_ADMIN, ROLE_AGENTE, ROLE_CLIENTE the mthod getNombre89 is undefined for the type rol
                .build();
    }
}
 