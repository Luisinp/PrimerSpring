package com.std.ec.PrimerSpring.repository;
 
import java.math.BigDecimal;
 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
 
import com.std.ec.PrimerSpring.model.Pago;
 
public interface PagoRepository extends JpaRepository<Pago, Integer> {
 
    // Función de la base de datos: monto de la reserva menos sus pagos (ver travelzen_logica.sql)
    @Query(value = "select fn_saldo_reserva(:idReserva)", nativeQuery = true)
    BigDecimal saldo(@Param("idReserva") Integer idReserva);
 
    // Cantidad de pagos de una reserva (si tiene alguno, no se puede cancelar)
    long countByReservaId(Integer idReserva);
}