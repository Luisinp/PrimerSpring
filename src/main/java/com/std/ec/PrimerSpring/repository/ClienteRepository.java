package com.std.ec.PrimerSpring.repository;
 
import java.util.List;
import java.util.Optional;
 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
 
import com.std.ec.PrimerSpring.model.Cliente;
 
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {
 
    Optional<Cliente> findByDocumentoIdentidad(String documentoIdentidad);
 
    // La ficha de cliente que pertenece a una cuenta web
    Optional<Cliente> findByUsuarioId(Integer idUsuario);
 
    // texto = "" devuelve todos. cuenta: TODOS, CON (tiene cuenta web) o SIN.
    @Query("""
            select c from Cliente c
            where (lower(c.nombreCompleto) like lower(concat('%', :texto, '%'))
                   or lower(c.documentoIdentidad) like lower(concat('%', :texto, '%'))
                   or lower(c.correo) like lower(concat('%', :texto, '%')))
              and c.activo in :estados
              and (:cuenta = 'TODOS'
                   or (:cuenta = 'CON' and c.usuario is not null)
                   or (:cuenta = 'SIN' and c.usuario is null))
            order by c.nombreCompleto
            """)
    List<Cliente> buscar(@Param("texto") String texto,
                         @Param("estados") List<Boolean> estados,
                         @Param("cuenta") String cuenta);
 
    // Registros que impiden eliminar a un cliente
    @Query(value = "select count(*) from reservas where id_cliente = :id", nativeQuery = true)
    long contarReservas(@Param("id") Integer id);
 
    // Movimientos hechos con la cuenta web del cliente (reservas registradas, cambios de estado)
    @Query(value = """
            select (select count(*) from reservas where id_usuario_registro = :idUsuario)
                 + (select count(*) from historial_estado_reserva where id_usuario = :idUsuario)
            """, nativeQuery = true)
    long contarMovimientosUsuario(@Param("idUsuario") Integer idUsuario);
}
 