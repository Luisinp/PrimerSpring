package com.std.ec.PrimerSpring.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.std.ec.PrimerSpring.model.Administrador;

public interface AdministradorRepository extends JpaRepository<Administrador, Integer> {

    boolean existsByDocumentoIdentidad(String documentoIdentidad);

    @Query("""
            select a from Administrador a
            join fetch a.usuario u
            where (lower(a.nombreCompleto) like lower(concat('%', :texto, '%'))
                   or lower(a.documentoIdentidad) like lower(concat('%', :texto, '%'))
                   or lower(u.correo) like lower(concat('%', :texto, '%')))
              and u.activo in :estados
            order by a.nombreCompleto
            """)
    List<Administrador> buscar(@Param("texto") String texto, @Param("estados") List<Boolean> estados);

    // Administradores con la cuenta activa (para no quedarse sin ninguno)
    @Query("select count(a) from Administrador a where a.usuario.activo = true")
    long contarActivos();

    // Registros que impiden eliminar a un administrador
    @Query(value = """
            select (select count(*) from reservas where id_administrador = :id)
                 + (select count(*) from reservas where id_usuario_registro = :id)
                 + (select count(*) from pagos where id_administrador = :id)
                 + (select count(*) from reportes where id_administrador = :id)
                 + (select count(*) from historial_estado_reserva where id_usuario = :id)
            """, nativeQuery = true)
    long contarVinculos(@Param("id") Integer id);
}