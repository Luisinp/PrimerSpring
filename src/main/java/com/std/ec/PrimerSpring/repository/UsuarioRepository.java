package com.std.ec.PrimerSpring.repository;
 
import java.util.Optional;
 
import org.springframework.data.jpa.repository.JpaRepository;
 
import com.std.ec.PrimerSpring.model.Usuario;
 
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
 
    Optional<Usuario> findByCorreo(String correo);
 
    boolean existsByCorreo(String correo);
}
 