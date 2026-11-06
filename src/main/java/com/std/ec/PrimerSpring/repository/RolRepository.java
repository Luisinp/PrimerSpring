package com.std.ec.PrimerSpring.repository;
 
import java.util.Optional;
 
import org.springframework.data.jpa.repository.JpaRepository;
 
import com.std.ec.PrimerSpring.model.Rol;
 
public interface RolRepository extends JpaRepository<Rol, Integer> {
 
    Optional<Rol> findByNombre(String nombre);
}
 