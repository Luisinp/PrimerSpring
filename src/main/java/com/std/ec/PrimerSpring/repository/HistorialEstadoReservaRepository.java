package com.std.ec.PrimerSpring.repository;
 
import java.util.List;
 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
 
import com.std.ec.PrimerSpring.model.HistorialEstadoReserva;
 
public interface HistorialEstadoReservaRepository extends JpaRepository<HistorialEstadoReserva, Integer> {
 
    // Cambios de estado de una reserva, del más antiguo al más reciente
    @Query("""
            select h from HistorialEstadoReserva h
            join fetch h.usuario u
            where h.reserva.id = :idReserva
            order by h.fechaCambio asc, h.id asc
            """)
    List<HistorialEstadoReserva> porReserva(@Param("idReserva") Integer idReserva);
}