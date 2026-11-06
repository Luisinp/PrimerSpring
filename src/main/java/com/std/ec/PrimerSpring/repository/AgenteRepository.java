package com.std.ec.PrimerSpring.repository;
 
import java.util.List;
 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
 
import com.std.ec.PrimerSpring.model.Agente;
 
public interface AgenteRepository extends JpaRepository<Agente, Integer> {
 
    boolean existsByDocumentoIdentidad(String documentoIdentidad);
 
    // Busca por nombre, documento o correo, y filtra por estado de la cuenta.
    // texto = "" devuelve todos.
    @Query("""
            select a from Agente a
            join fetch a.usuario u
            where (lower(a.nombreCompleto) like lower(concat('%', :texto, '%'))
                   or lower(a.documentoIdentidad) like lower(concat('%', :texto, '%'))
                   or lower(u.correo) like lower(concat('%', :texto, '%')))
              and u.activo in :estados
            order by a.nombreCompleto
            """)
    List<Agente> buscar(@Param("texto") String texto, @Param("estados") List<Boolean> estados);
 
    // Registros que impiden eliminar a un agente (reservas, pagos, historial de estados)
    @Query(value = """
            select (select count(*) from reservas where id_agente = :id or id_usuario_registro = :id)
                 + (select count(*) from pagos where id_agente = :id)
                 + (select count(*) from historial_estado_reserva where id_usuario = :id)
            """, nativeQuery = true)
    long contarVinculos(@Param("id") Integer id);
}