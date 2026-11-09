package com.std.ec.PrimerSpring.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.std.ec.PrimerSpring.model.Reporte;
import com.std.ec.PrimerSpring.model.Usuario;
import com.std.ec.PrimerSpring.repository.ReporteRepository;
import com.std.ec.PrimerSpring.repository.ReservaRepository;
import com.std.ec.PrimerSpring.repository.UsuarioRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * Reportes del administrador (CU-10): reservas por estado y periodo, ingresos y pagos,
 * clientes y paquetes comercializados. Cada reporte generado queda anotado en la tabla reportes.
 */
@Service
public class ReporteService {

    public static final List<String> TIPOS = List.of("RESERVAS", "INGRESOS", "CLIENTES", "PAQUETES");
    private static final List<String> FORMATOS = List.of("PANTALLA", "PDF", "EXCEL");
    private static final List<String> ESTADOS = List.of("PENDIENTE", "CONFIRMADA", "CANCELADA", "FINALIZADA");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @PersistenceContext
    private EntityManager em;

    private final ReporteRepository reporteRepository;
    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;

    public ReporteService(ReporteRepository reporteRepository,
                          ReservaRepository reservaRepository,
                          UsuarioRepository usuarioRepository) {
        this.reporteRepository = reporteRepository;
        this.reservaRepository = reservaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<Reporte> recientes() {
        return reporteRepository.findTop8ByOrderByIdDesc();
    }

    /** Lanza IllegalArgumentException con un mensaje apto para mostrar al administrador. */
    @Transactional
    public ReporteTabla generar(String correoAdmin, String tipo, LocalDate ini, LocalDate fin,
                                String estado, String formato) {

        Usuario usuario = usuarioRepository.findByCorreo(correoAdmin)
                .orElseThrow(() -> new IllegalArgumentException("Inicia sesión para continuar."));
        if (!"ADMIN".equals(usuario.getRol().getNombre())) {
            throw new IllegalArgumentException("Solo el administrador puede generar reportes.");
        }
        if (reservaRepository.contarAdministrador(usuario.getId()) == 0) {
            throw new IllegalArgumentException("Tu usuario no tiene ficha de administrador, por eso no puede generar reportes.");
        }
        if (tipo == null || !TIPOS.contains(tipo)) {
            throw new IllegalArgumentException("Elige el tipo de reporte.");
        }
        if (ini == null || fin == null) {
            throw new IllegalArgumentException("Indica la fecha inicial y la final.");
        }
        if (fin.isBefore(ini)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la inicial.");
        }
        if (ChronoUnit.DAYS.between(ini, fin) > 366) {
            throw new IllegalArgumentException("El periodo no puede superar un año.");
        }
        if (formato == null || !FORMATOS.contains(formato)) {
            throw new IllegalArgumentException("Formato no válido.");
        }

        ReporteTabla tabla = switch (tipo) {
            case "RESERVAS" -> reservas(ini, fin, estado);
            case "INGRESOS" -> ingresos(ini, fin);
            case "CLIENTES" -> clientes(ini, fin);
            default -> paquetes(ini, fin);
        };

        Reporte r = new Reporte();
        r.setIdAdministrador(usuario.getId());
        r.setTipo(tipo);
        r.setFechaInicio(ini);
        r.setFechaFin(fin);
        r.setFormato(formato);
        reporteRepository.save(r);

        return tabla;
    }

    // ================= Reservas por estado y periodo =================

    private ReporteTabla reservas(LocalDate ini, LocalDate fin, String estado) {
        String e = ESTADOS.contains(estado) ? estado : "TODOS";
        List<List<Object>> filas = consultar("""
                select r.codigo_unico, c.nombre_completo, coalesce(p.nombre, '-'),
                       concat(upper(left(r.estado, 1)), lower(substr(r.estado, 2))),
                       concat(upper(left(r.estado_pago, 1)), lower(substr(r.estado_pago, 2))),
                       r.monto_total, date_format(r.fecha_registro, '%d/%m/%Y')
                from reservas r
                join clientes c on c.id_cliente = r.id_cliente
                left join paquetes_turisticos p on p.id_paquete = r.id_paquete
                where r.fecha_registro >= :ini and r.fecha_registro < :fin
                  and (:estado = 'TODOS' or r.estado = :estado)
                order by r.fecha_registro desc, r.id_reserva desc
                """, Map.of("ini", ini, "fin", fin.plusDays(1), "estado", e));

        String periodo = periodo(ini, fin) + ("TODOS".equals(e) ? "" : " · Estado: " + e.toLowerCase());
        ReporteTabla t = new ReporteTabla("Reservas por estado y periodo", periodo,
                List.of("Código", "Cliente", "Paquete", "Estado", "Pago", "Total (USD)", "Registrada"),
                filas, Set.of(5));

        t.agregarResumen("Reservas", String.valueOf(filas.size()));
        t.agregarResumen("Pendientes", String.valueOf(contar(filas, 3, "Pendiente")));
        t.agregarResumen("Confirmadas", String.valueOf(contar(filas, 3, "Confirmada")));
        t.agregarResumen("Finalizadas", String.valueOf(contar(filas, 3, "Finalizada")));
        t.agregarResumen("Canceladas", String.valueOf(contar(filas, 3, "Cancelada")));
        t.agregarResumen("Monto sin canceladas (USD)",
                ReporteTabla.dinero(sumaSi(filas, 5, 3, "Cancelada", false)));
        return t;
    }

    // ================= Ingresos y pagos =================

    private ReporteTabla ingresos(LocalDate ini, LocalDate fin) {
        List<List<Object>> filas = consultar("""
                select lpad(cp.numero_correlativo, 6, '0'), date_format(pg.fecha, '%d/%m/%Y'),
                       r.codigo_unico, c.nombre_completo,
                       case pg.metodo when 'EFECTIVO' then 'Efectivo' else 'Transferencia' end,
                       pg.monto, u.correo
                from pagos pg
                join comprobantes cp on cp.id_pago = pg.id_pago
                join reservas r on r.id_reserva = pg.id_reserva
                join clientes c on c.id_cliente = r.id_cliente
                join usuarios u on u.id_usuario = coalesce(pg.id_agente, pg.id_administrador)
                where pg.fecha between :ini and :fin
                order by pg.fecha desc, cp.numero_correlativo desc
                """, Map.of("ini", ini, "fin", fin));

        ReporteTabla t = new ReporteTabla("Ingresos y pagos", periodo(ini, fin),
                List.of("N.º", "Fecha", "Reserva", "Cliente", "Método", "Monto (USD)", "Registró"),
                filas, Set.of(5));

        t.agregarResumen("Pagos", String.valueOf(filas.size()));
        t.agregarResumen("Total cobrado (USD)", ReporteTabla.dinero(suma(filas, 5)));
        t.agregarResumen("En efectivo (USD)", ReporteTabla.dinero(sumaSi(filas, 5, 4, "Efectivo", true)));
        t.agregarResumen("Por transferencia (USD)", ReporteTabla.dinero(sumaSi(filas, 5, 4, "Transferencia", true)));
        return t;
    }

    // ================= Clientes =================

    private ReporteTabla clientes(LocalDate ini, LocalDate fin) {
        List<List<Object>> filas = consultar("""
                select c.nombre_completo, c.documento_identidad, c.correo,
                       case when c.id_usuario is null then 'Mostrador' else 'Cuenta web' end,
                       case when c.activo then 'Activo' else 'Inactivo' end,
                       count(r.id_reserva), coalesce(sum(r.monto_total), 0.00)
                from clientes c
                left join reservas r on r.id_cliente = c.id_cliente
                     and r.estado <> 'CANCELADA'
                     and r.fecha_registro >= :ini and r.fecha_registro < :fin
                group by c.id_cliente, c.nombre_completo, c.documento_identidad, c.correo, c.id_usuario, c.activo
                order by c.nombre_completo
                """, Map.of("ini", ini, "fin", fin.plusDays(1)));

        ReporteTabla t = new ReporteTabla("Clientes y sus reservas", periodo(ini, fin) + " · Reservas no canceladas",
                List.of("Cliente", "Documento", "Correo", "Tipo", "Estado", "Reservas", "Total (USD)"),
                filas, Set.of(6));

        long conReservas = filas.stream()
                .filter(f -> f.get(5) instanceof Number n && n.longValue() > 0).count();
        t.agregarResumen("Clientes", String.valueOf(filas.size()));
        t.agregarResumen("Con reservas en el periodo", String.valueOf(conReservas));
        t.agregarResumen("Total reservado (USD)", ReporteTabla.dinero(suma(filas, 6)));
        return t;
    }

    // ================= Paquetes comercializados =================

    private ReporteTabla paquetes(LocalDate ini, LocalDate fin) {
        List<List<Object>> filas = consultar("""
                select p.nombre, d.nombre, count(r.id_reserva),
                       coalesce(sum(ps.n), 0), coalesce(sum(r.monto_total), 0.00)
                from paquetes_turisticos p
                join destinos d on d.id_destino = p.id_destino
                left join reservas r on r.id_paquete = p.id_paquete
                     and r.estado <> 'CANCELADA'
                     and r.fecha_registro >= :ini and r.fecha_registro < :fin
                left join (select id_reserva, count(*) as n from pasajeros group by id_reserva) ps
                     on ps.id_reserva = r.id_reserva
                group by p.id_paquete, p.nombre, d.nombre
                order by 5 desc, p.nombre
                """, Map.of("ini", ini, "fin", fin.plusDays(1)));

        ReporteTabla t = new ReporteTabla("Paquetes comercializados", periodo(ini, fin) + " · Reservas no canceladas",
                List.of("Paquete", "Destino", "Reservas", "Pasajeros", "Total (USD)"),
                filas, Set.of(4));

        t.agregarResumen("Paquetes", String.valueOf(filas.size()));
        t.agregarResumen("Reservas", String.valueOf(suma(filas, 2).longValue()));
        t.agregarResumen("Pasajeros", String.valueOf(suma(filas, 3).longValue()));
        t.agregarResumen("Total vendido (USD)", ReporteTabla.dinero(suma(filas, 4)));
        return t;
    }

    // ================= Auxiliares =================

    private static String periodo(LocalDate ini, LocalDate fin) {
        return "Del " + ini.format(FECHA) + " al " + fin.format(FECHA);
    }

    private List<List<Object>> consultar(String sql, Map<String, Object> parametros) {
        Query q = em.createNativeQuery(sql);
        for (Map.Entry<String, Object> p : parametros.entrySet()) {
            q.setParameter(p.getKey(), p.getValue());
        }
        List<List<Object>> filas = new ArrayList<>();
        for (Object fila : q.getResultList()) {
            Object[] columnas = (Object[]) fila;
            List<Object> f = new ArrayList<>(columnas.length);
            for (Object c : columnas) {
                f.add(normalizar(c));
            }
            filas.add(f);
        }
        return filas;
    }

    private static Object normalizar(Object v) {
        if (v == null) {
            return "";
        }
        if (v instanceof byte[] b) {
            return new String(b, StandardCharsets.UTF_8);
        }
        if (v instanceof BigInteger b) {
            return b.longValue();
        }
        if (v instanceof Number n && !(v instanceof BigDecimal)) {
            return n.longValue();
        }
        return v;
    }

    private static BigDecimal suma(List<List<Object>> filas, int col) {
        BigDecimal total = BigDecimal.ZERO;
        for (List<Object> f : filas) {
            if (f.get(col) instanceof Number n) {
                total = total.add(new BigDecimal(n.toString()));
            }
        }
        return total;
    }

    // Suma la columna col de las filas cuya columna colTexto es (igual = true) o no es (igual = false) el valor dado
    private static BigDecimal sumaSi(List<List<Object>> filas, int col, int colTexto, String valor, boolean igual) {
        BigDecimal total = BigDecimal.ZERO;
        for (List<Object> f : filas) {
            boolean coincide = valor.equals(String.valueOf(f.get(colTexto)));
            if (coincide == igual && f.get(col) instanceof Number n) {
                total = total.add(new BigDecimal(n.toString()));
            }
        }
        return total;
    }

    private static long contar(List<List<Object>> filas, int col, String valor) {
        return filas.stream().filter(f -> valor.equals(String.valueOf(f.get(col)))).count();
    }
}