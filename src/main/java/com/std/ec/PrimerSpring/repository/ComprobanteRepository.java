package com.std.ec.PrimerSpring.repository;
 
import java.util.List;
import java.util.Optional;
 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
 
import com.std.ec.PrimerSpring.model.Comprobante;
 
public interface ComprobanteRepository extends JpaRepository<Comprobante, Integer> {
 
    // Función de la base de datos: entrega el próximo número sin repetir ni saltar (ver travelzen_logica.sql)
    @Query(value = "select fn_siguiente_comprobante()", nativeQuery = true)
    Integer siguienteNumero();
 
    // Comprobantes de una reserva (uno por pago), del más reciente al más antiguo
    @Query("""
            select c from Comprobante c
            join fetch c.pago p
            where p.reserva.id = :idReserva
            order by c.numeroCorrelativo desc
            """)
    List<Comprobante> porReserva(@Param("idReserva") Integer idReserva);
 
    // Todo lo necesario para mostrar e imprimir un comprobante
    @Query("""
            select c from Comprobante c
            join fetch c.pago p
            join fetch p.reserva r
            join fetch r.cliente
            left join fetch r.paquete
            left join fetch r.itinerario
            where c.id = :id
            """)
    Optional<Comprobante> detalle(@Param("id") Integer id);
}
 