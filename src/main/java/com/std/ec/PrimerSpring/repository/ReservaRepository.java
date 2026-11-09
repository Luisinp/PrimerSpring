package com.std.ec.PrimerSpring.repository;
 
import java.util.List;
import java.util.Optional;
 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
 
import com.std.ec.PrimerSpring.model.Reserva;
 
import jakarta.persistence.LockModeType;
 
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
 
    // ---------- Panel del personal ----------
    // estado: TODOS, PENDIENTE, CONFIRMADA, CANCELADA o FINALIZADA. texto = "" no filtra.
    @Query("""
            select distinct r from Reserva r
            join fetch r.cliente c
            join fetch r.registradoPor u
            left join fetch r.paquete p
            left join fetch p.destino
            left join fetch r.itinerario
            left join fetch r.pasajeros
            where (:estado = 'TODOS' or r.estado = :estado)
              and (:texto = ''
                   or lower(r.codigoUnico) like lower(concat('%', :texto, '%'))
                   or lower(c.nombreCompleto) like lower(concat('%', :texto, '%'))
                   or lower(c.documentoIdentidad) like lower(concat('%', :texto, '%')))
            order by r.fechaRegistro desc
            """)
    List<Reserva> buscarParaPersonal(@Param("estado") String estado, @Param("texto") String texto);
 
    @Query("select r.estado, count(r) from Reserva r group by r.estado")
    List<Object[]> contarPorEstado();
 
    // La clave de agentes y administradores es su id_usuario; se comprueba antes de asignarlo
    @Query(value = "select count(*) from agentes where id_usuario = :id", nativeQuery = true)
    long contarAgente(@Param("id") Integer id);
 
    @Query(value = "select count(*) from administradores where id_usuario = :id", nativeQuery = true)
    long contarAdministrador(@Param("id") Integer id);
 
    // ---------- Pagos ----------
    // Una reserva con su cliente, paquete e itinerario (página de pagos)
    @Query("""
            select r from Reserva r
            join fetch r.cliente
            left join fetch r.paquete
            left join fetch r.itinerario
            where r.id = :id
            """)
    Optional<Reserva> detalle(@Param("id") Integer id);
 
    // Bloquea la fila de la reserva mientras dura la transacción: dos cobros simultáneos
    // de la misma reserva se atienden uno después del otro (SELECT ... FOR UPDATE)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.id = :id")
    Optional<Reserva> buscarParaActualizar(@Param("id") Integer id);
}
 