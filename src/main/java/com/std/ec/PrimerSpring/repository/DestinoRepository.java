package com.std.ec.PrimerSpring.repository;
 
import java.util.List;
 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
 
import com.std.ec.PrimerSpring.model.Destino;
 
public interface DestinoRepository extends JpaRepository<Destino, Integer> {
 
    boolean existsByNombreIgnoreCaseAndPaisIgnoreCase(String nombre, String pais);
 
    List<Destino> findByActivoTrueOrderByNombre();
 
    @Query("""
            select d from Destino d
            where (lower(d.nombre) like lower(concat('%', :texto, '%'))
                   or lower(d.pais) like lower(concat('%', :texto, '%')))
              and d.activo in :estados
            order by d.nombre
            """)
    List<Destino> buscar(@Param("texto") String texto, @Param("estados") List<Boolean> estados);
 
    // Paquetes que impiden eliminar un destino
    @Query(value = "select count(*) from paquetes_turisticos where id_destino = :id", nativeQuery = true)
    long contarPaquetes(@Param("id") Integer id);
 
    // Destinos que tienen al menos un paquete visible en el catálogo (para los filtros)
    @Query("""
            select d from Destino d
            where d.activo = true
              and exists (select 1 from PaqueteTuristico p
                          where p.destino = d and p.publicado = true and p.activo = true)
            order by d.nombre
            """)
    List<Destino> conPaquetesPublicados();
}
 