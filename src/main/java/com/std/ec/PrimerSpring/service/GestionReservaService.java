package com.std.ec.PrimerSpring.service;
 
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
 
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import com.std.ec.PrimerSpring.model.HistorialEstadoReserva;
import com.std.ec.PrimerSpring.model.Reserva;
import com.std.ec.PrimerSpring.model.Usuario;
import com.std.ec.PrimerSpring.repository.HistorialEstadoReservaRepository;
import com.std.ec.PrimerSpring.repository.PagoRepository;
import com.std.ec.PrimerSpring.repository.PaqueteRepository;
import com.std.ec.PrimerSpring.repository.ReservaRepository;
import com.std.ec.PrimerSpring.repository.UsuarioRepository;
 
/**
 * Gestión de reservas por el personal (CU-02, flujo 8b): confirmar, cancelar y finalizar,
 * con su historial. Cada cambio se hace en una sola transacción.
 *
 * Transiciones válidas: PENDIENTE a CONFIRMADA o CANCELADA; CONFIRMADA a CANCELADA o FINALIZADA.
 * Una reserva cancelada o finalizada no vuelve a activarse.
 *
 * Reglas con pagos:
 *  - No se cancela una reserva que ya tiene pagos registrados.
 *  - Solo se finaliza si el viaje ya terminó y el pago está completado.
 */
@Service
public class GestionReservaService {
 
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
 
    private static final List<String> ESTADOS = List.of("PENDIENTE", "CONFIRMADA", "CANCELADA", "FINALIZADA");
 
    private static final Map<String, Set<String>> TRANSICIONES = Map.of(
            "PENDIENTE", Set.of("CONFIRMADA", "CANCELADA"),
            "CONFIRMADA", Set.of("CANCELADA", "FINALIZADA"),
            "CANCELADA", Set.of(),
            "FINALIZADA", Set.of());
 
    private final ReservaRepository reservaRepository;
    private final HistorialEstadoReservaRepository historialRepository;
    private final PaqueteRepository paqueteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PagoRepository pagoRepository;
 
    public GestionReservaService(ReservaRepository reservaRepository,
                                 HistorialEstadoReservaRepository historialRepository,
                                 PaqueteRepository paqueteRepository,
                                 UsuarioRepository usuarioRepository,
                                 PagoRepository pagoRepository) {
        this.reservaRepository = reservaRepository;
        this.historialRepository = historialRepository;
        this.paqueteRepository = paqueteRepository;
        this.usuarioRepository = usuarioRepository;
        this.pagoRepository = pagoRepository;
    }
 
    @Transactional(readOnly = true)
    public List<Reserva> buscar(String estado, String texto) {
        String e = ESTADOS.contains(estado) ? estado : "TODOS";
        String t = texto == null ? "" : texto.trim();
        return reservaRepository.buscarParaPersonal(e, t);
    }
 
    /** Cantidad de reservas por estado (todos los estados, aunque sea 0). */
    @Transactional(readOnly = true)
    public Map<String, Long> contarPorEstado() {
        Map<String, Long> conteo = new LinkedHashMap<>();
        ESTADOS.forEach(e -> conteo.put(e, 0L));
        for (Object[] fila : reservaRepository.contarPorEstado()) {
            conteo.put((String) fila[0], (Long) fila[1]);
        }
        return conteo;
    }
 
    @Transactional(readOnly = true)
    public List<HistorialEstadoReserva> historial(Integer idReserva) {
        return historialRepository.porReserva(idReserva);
    }
 
    /** Lanza IllegalArgumentException con un mensaje apto para mostrar al personal. */
    @Transactional
    public Reserva cambiarEstado(Integer idReserva, String nuevoEstado, String correoPersonal) {
 
        Usuario usuario = usuarioRepository.findByCorreo(correoPersonal)
                .orElseThrow(() -> new IllegalArgumentException("Inicia sesión para continuar."));
        String rol = usuario.getRol().getNombre();
        boolean esAgente = "AGENTE".equals(rol);
        boolean esAdmin = "ADMIN".equals(rol);
        if (!esAgente && !esAdmin) {
            throw new IllegalArgumentException("Solo el personal de la agencia puede cambiar el estado de una reserva.");
        }
 
        Reserva reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new IllegalArgumentException("La reserva ya no existe."));
 
        String actual = reserva.getEstado();
        if (!TRANSICIONES.getOrDefault(actual, Set.of()).contains(nuevoEstado)) {
            throw new IllegalArgumentException("La reserva " + reserva.getCodigoUnico() + " está "
                    + etiqueta(actual) + " y no puede pasar a " + etiqueta(nuevoEstado) + ".");
        }
 
        // Reglas que dependen de los pagos
        if ("CANCELADA".equals(nuevoEstado)
                && pagoRepository.countByReservaId(reserva.getId()) > 0) {
            throw new IllegalArgumentException("No se puede cancelar la reserva " + reserva.getCodigoUnico()
                    + ": ya tiene pagos registrados. Resuelve primero los pagos con la agencia.");
        }
        if ("FINALIZADA".equals(nuevoEstado)) {
            LocalDate fin = reserva.getItinerario() == null ? null : reserva.getItinerario().getFechaFin();
            if (fin != null && !LocalDate.now().isAfter(fin)) {
                throw new IllegalArgumentException("No se puede finalizar la reserva " + reserva.getCodigoUnico()
                        + ": el viaje termina el " + fin.format(FECHA) + ".");
            }
            if (!"COMPLETADO".equals(reserva.getEstadoPago())) {
                throw new IllegalArgumentException("No se puede finalizar la reserva " + reserva.getCodigoUnico()
                        + ": el pago no está completado.");
            }
        }
 
        // Al cancelar, los cupos vuelven al paquete (un cupo por pasajero)
        if ("CANCELADA".equals(nuevoEstado) && reserva.getPaquete() != null) {
            paqueteRepository.devolverCupos(reserva.getPaquete().getId(), reserva.getPasajeros().size());
        }
 
        // Quien la gestiona primero queda como responsable (un agente o un administrador, nunca los dos)
        if (reserva.getIdAgente() == null && reserva.getIdAdministrador() == null) {
            if (esAgente && reservaRepository.contarAgente(usuario.getId()) > 0) {
                reserva.setIdAgente(usuario.getId());
            } else if (esAdmin && reservaRepository.contarAdministrador(usuario.getId()) > 0) {
                reserva.setIdAdministrador(usuario.getId());
            }
        }
 
        reserva.setEstado(nuevoEstado);
        reservaRepository.save(reserva);
 
        HistorialEstadoReserva cambio = new HistorialEstadoReserva();
        cambio.setReserva(reserva);
        cambio.setUsuario(usuario);
        cambio.setEstadoAnterior(actual);
        cambio.setEstadoNuevo(nuevoEstado);
        historialRepository.save(cambio);
 
        return reserva;
    }
 
    private static String etiqueta(String estado) {
        if (estado == null) {
            return "—";
        }
        return switch (estado) {
            case "PENDIENTE" -> "pendiente";
            case "CONFIRMADA" -> "confirmada";
            case "CANCELADA" -> "cancelada";
            case "FINALIZADA" -> "finalizada";
            default -> estado.toLowerCase();
        };
    }
}
 