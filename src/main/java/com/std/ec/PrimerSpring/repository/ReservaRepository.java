package com.std.ec.PrimerSpring.repository;
 
import java.util.List;
 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
 
import com.std.ec.PrimerSpring.model.Reserva;
 
public interface ReservaRepository extends JpaRepository<Reserva, Integer> {
 
    boolean existsByCodigoUnico(String codigoUnico);
 
    // Reservas de un cliente, con todo lo necesario para mostrarlas
    @Query("""
            select distinct r from Reserva r
            left join fetch r.paquete p
            left join fetch p.destino
            left join fetch r.itinerario
            left join fetch r.pasajeros
            where r.cliente.id = :idCliente
            order by r.fechaRegistro desc
            """)
    List<Reserva> misReservas(@Param("idCliente") Integer idCliente);
}