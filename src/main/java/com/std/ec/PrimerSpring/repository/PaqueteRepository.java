package com.std.ec.PrimerSpring.repository;
 
import java.util.List;
 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
 
import com.std.ec.PrimerSpring.model.PaqueteTuristico;
 
public interface PaqueteRepository extends JpaRepository<PaqueteTuristico, Integer> {
 
    // publicacion: TODOS, PUBLICADOS o BORRADORES
    @Query("""
            select p from PaqueteTuristico p
            join fetch p.destino d
            where (lower(p.nombre) like lower(concat('%', :texto, '%'))
                   or lower(d.nombre) like lower(concat('%', :texto, '%')))
              and p.activo in :estados
              and (:publicacion = 'TODOS'
                   or (:publicacion = 'PUBLICADOS' and p.publicado = true)
                   or (:publicacion = 'BORRADORES' and p.publicado = false))
            order by p.nombre
            """)
    List<PaqueteTuristico> buscar(@Param("texto") String texto,
                                  @Param("estados") List<Boolean> estados,
                                  @Param("publicacion") String publicacion);
 
    // Reservas que impiden eliminar un paquete
    @Query(value = "select count(*) from reservas where id_paquete = :id", nativeQuery = true)
    long contarReservas(@Param("id") Integer id);
 
    // ---------- Catálogo público: solo publicados y activos, de destinos activos ----------
    // idDestino = 0 significa "todos los destinos"
    @Query("""
            select p from PaqueteTuristico p
            join fetch p.destino d
            where p.publicado = true and p.activo = true and d.activo = true
              and (:idDestino = 0 or d.id = :idDestino)
              and (lower(p.nombre) like lower(concat('%', :texto, '%'))
                   or lower(d.nombre) like lower(concat('%', :texto, '%'))
                   or lower(d.pais) like lower(concat('%', :texto, '%')))
            order by p.id desc
            """)
    List<PaqueteTuristico> catalogo(@Param("idDestino") int idDestino, @Param("texto") String texto);
 
    @Query("""
            select p from PaqueteTuristico p
            join fetch p.destino d
            where p.id = :id and p.publicado = true and p.activo = true and d.activo = true
            """)
    java.util.Optional<PaqueteTuristico> buscarPublicado(@Param("id") Integer id);
 
    /**
     * Descuenta cupos de forma atómica: solo actúa si alcanzan. Devuelve las filas afectadas
     * (0 = no había cupos suficientes). Así dos clientes no pueden vender de más el mismo cupo.
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            update PaqueteTuristico p
            set p.cuposDisponibles = p.cuposDisponibles - :n
            where p.id = :id and p.cuposDisponibles >= :n
            """)
    int descontarCupos(@Param("id") Integer id, @Param("n") int n);
}
 