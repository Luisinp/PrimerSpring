package com.std.ec.PrimerSpring.repository;
 
import java.util.List;
 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
 
import com.std.ec.PrimerSpring.model.Vuelo;
 
public interface VueloRepository extends JpaRepository<Vuelo, Integer> {
 
    // texto = "" devuelve todos. Busca en aerolínea, origen y destino.
    @Query("""
            select v from Vuelo v
            where (lower(v.aerolinea) like lower(concat('%', :texto, '%'))
                   or lower(v.origen) like lower(concat('%', :texto, '%'))
                   or lower(v.destino) like lower(concat('%', :texto, '%')))
              and v.activo in :estados
            order by v.fecha asc, v.hora asc
            """)
    List<Vuelo> buscar(@Param("texto") String texto, @Param("estados") List<Boolean> estados);
 
    long countByActivo(Boolean activo);
 
    // Reservas que impiden eliminar un vuelo
    @Query(value = "select count(*) from reservas where id_vuelo = :id", nativeQuery = true)
    long contarReservas(@Param("id") Integer id);
}
 