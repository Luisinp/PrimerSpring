package com.std.ec.PrimerSpring.service;
 
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
 
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import com.std.ec.PrimerSpring.model.Cliente;
import com.std.ec.PrimerSpring.model.HistorialEstadoReserva;
import com.std.ec.PrimerSpring.model.Itinerario;
import com.std.ec.PrimerSpring.model.PaqueteTuristico;
import com.std.ec.PrimerSpring.model.Pasajero;
import com.std.ec.PrimerSpring.model.Reserva;
import com.std.ec.PrimerSpring.model.Usuario;
import com.std.ec.PrimerSpring.repository.ClienteRepository;
import com.std.ec.PrimerSpring.repository.HistorialEstadoReservaRepository;
import com.std.ec.PrimerSpring.repository.PaqueteRepository;
import com.std.ec.PrimerSpring.repository.ReservaRepository;
import com.std.ec.PrimerSpring.repository.UsuarioRepository;
 
/**
 * Reservas de paquetes (CU-02): las hace el cliente desde la web o el personal a nombre de un cliente.
 * Todo ocurre en una sola transacción: si algo falla, no se descuentan cupos ni queda nada a medias.
 */
@Service
public class ReservaService {
 
    private static final int MAX_PASAJEROS = 10;
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
 
    private final SecureRandom random = new SecureRandom();
 
    private final ReservaRepository reservaRepository;
    private final HistorialEstadoReservaRepository historialRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PaqueteRepository paqueteRepository;
 
    public ReservaService(ReservaRepository reservaRepository,
                          HistorialEstadoReservaRepository historialRepository,
                          ClienteRepository clienteRepository,
                          UsuarioRepository usuarioRepository,
                          PaqueteRepository paqueteRepository) {
        this.reservaRepository = reservaRepository;
        this.historialRepository = historialRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.paqueteRepository = paqueteRepository;
    }
 
    /** Datos del cliente para rellenar el primer pasajero (suele viajar él mismo). */
    @Transactional(readOnly = true)
    public PasajeroDatos datosDelCliente(String correoUsuario) {
        PasajeroDatos d = new PasajeroDatos();
        buscarCliente(correoUsuario).ifPresent(c -> {
            d.setNombre(c.getNombreCompleto());
            d.setDocumento(c.getDocumentoIdentidad());
            d.setNacionalidad(c.getNacionalidad());
            d.setSexo(c.getSexo());
            d.setFechaNacimiento(c.getFechaNacimiento());
        });
        return d;
    }
 
    @Transactional(readOnly = true)
    public List<Reserva> misReservas(String correoUsuario) {
        return buscarCliente(correoUsuario)
                .map(c -> reservaRepository.misReservas(c.getId()))
                .orElse(List.of());
    }
 
    // ---------------- Apoyo para la reserva hecha por el personal ----------------
 
    /** Clientes activos que coinciden con el texto (nombre, documento o correo). Máximo 15. */
    @Transactional(readOnly = true)
    public List<Cliente> buscarClientesActivos(String texto) {
        String t = texto == null ? "" : texto.trim();
        return clienteRepository.buscar(t, List.of(Boolean.TRUE), "TODOS").stream().limit(15).toList();
    }
 
    /** Paquetes publicados y activos que todavía tienen cupos. */
    @Transactional(readOnly = true)
    public List<PaqueteTuristico> paquetesDisponibles() {
        return paqueteRepository.catalogo(0, "").stream()
                .filter(p -> p.getCuposDisponibles() != null && p.getCuposDisponibles() > 0)
                .toList();
    }
 
    // ---------------- Reservar ----------------
 
    /** Reserva del cliente desde la web. Lanza IllegalArgumentException con un mensaje apto para mostrar. */
    @Transactional
    public Reserva reservar(String correoUsuario, Integer idPaquete, LocalDate fechaInicio,
                            String origen, List<PasajeroDatos> pasajeros) {
 
        Usuario usuario = usuarioRepository.findByCorreo(correoUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Inicia sesión para reservar."));
        Cliente cliente = clienteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Solo los clientes pueden reservar desde aquí. El personal registra las reservas desde su panel."));
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new IllegalArgumentException("Tu registro de cliente está inactivo. Acércate a la agencia.");
        }
 
        return crear(usuario, cliente, idPaquete, fechaInicio, origen, pasajeros);
    }
 
    /**
     * Reserva registrada por un agente o administrador a nombre de un cliente (con o sin cuenta web).
     * Si confirmarDeInmediato es true, queda CONFIRMADA y el historial guarda los dos movimientos.
     */
    @Transactional
    public Reserva reservarPorPersonal(String correoPersonal, Integer idCliente, Integer idPaquete,
                                       LocalDate fechaInicio, String origen,
                                       List<PasajeroDatos> pasajeros, boolean confirmarDeInmediato) {
 
        Usuario usuario = usuarioRepository.findByCorreo(correoPersonal)
                .orElseThrow(() -> new IllegalArgumentException("Inicia sesión para continuar."));
        String rol = usuario.getRol().getNombre();
        boolean esAgente = "AGENTE".equals(rol);
        boolean esAdmin = "ADMIN".equals(rol);
        if (!esAgente && !esAdmin) {
            throw new IllegalArgumentException("Solo el personal de la agencia puede registrar reservas a nombre de un cliente.");
        }
 
        if (idCliente == null) {
            throw new IllegalArgumentException("Elige el cliente de la reserva.");
        }
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new IllegalArgumentException("El cliente ya no existe."));
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new IllegalArgumentException("El cliente está inactivo y no puede tener nuevas reservas.");
        }
 
        Reserva reserva = crear(usuario, cliente, idPaquete, fechaInicio, origen, pasajeros);
 
        // Quien la registra queda como responsable (un agente o un administrador, nunca los dos)
        if (esAgente && reservaRepository.contarAgente(usuario.getId()) > 0) {
            reserva.setIdAgente(usuario.getId());
        } else if (esAdmin && reservaRepository.contarAdministrador(usuario.getId()) > 0) {
            reserva.setIdAdministrador(usuario.getId());
        }
 
        if (confirmarDeInmediato) {
            reserva.setEstado("CONFIRMADA");
            registrarHistorial(reserva, usuario, "PENDIENTE", "CONFIRMADA");
        }
        reservaRepository.save(reserva);
 
        return reserva;
    }
 
    // ---------------- Auxiliares ----------------
 
    /** Valida, descuenta cupos y guarda la reserva en estado PENDIENTE con su itinerario, pasajeros e historial. */
    private Reserva crear(Usuario usuario, Cliente cliente, Integer idPaquete, LocalDate fechaInicio,
                          String origen, List<PasajeroDatos> pasajeros) {
 
        if (idPaquete == null) {
            throw new IllegalArgumentException("Elige un paquete.");
        }
        PaqueteTuristico paquete = paqueteRepository.buscarPublicado(idPaquete)
                .orElseThrow(() -> new IllegalArgumentException("Este paquete ya no está disponible."));
 
        validarDatos(fechaInicio, origen, pasajeros);
 
        // Descuento atómico de cupos: si no alcanzan, no se hace nada
        int n = pasajeros.size();
        if (paqueteRepository.descontarCupos(idPaquete, n) == 0) {
            throw new IllegalArgumentException(
                    "No hay cupos suficientes para " + n + " pasajero(s). Quedan " + paquete.getCuposDisponibles() + ".");
        }
 
        // Precio por persona x número de pasajeros
        BigDecimal monto = paquete.getPrecio().multiply(BigDecimal.valueOf(n));
 
        Reserva reserva = new Reserva();
        reserva.setCodigoUnico(generarCodigo());
        reserva.setTipo("PAQUETE");
        reserva.setMontoTotal(monto);
        reserva.setEstado("PENDIENTE");
        reserva.setEstadoPago("PENDIENTE");
        reserva.setCliente(cliente);
        reserva.setRegistradoPor(usuario);
        reserva.setPaquete(paquete);
 
        Itinerario itinerario = new Itinerario();
        itinerario.setReserva(reserva);
        itinerario.setOrigen(limitar(origen.trim(), 80));
        itinerario.setDestino(limitar(paquete.getDestino().getNombre() + ", " + paquete.getDestino().getPais(), 80));
        itinerario.setFechaInicio(fechaInicio);
        itinerario.setFechaFin(fechaInicio.plusDays(paquete.getDuracionDias() - 1L));
        reserva.setItinerario(itinerario);
 
        for (PasajeroDatos d : pasajeros) {
            Pasajero p = new Pasajero();
            p.setReserva(reserva);
            p.setNombreCompleto(d.getNombre().trim());
            p.setDocumentoIdentidad(d.getDocumento().trim().toUpperCase());
            p.setNacionalidad(vacioANulo(d.getNacionalidad()));
            p.setSexo(vacioANulo(d.getSexo()));
            p.setFechaNacimiento(d.getFechaNacimiento());
            reserva.getPasajeros().add(p);
        }
        reservaRepository.save(reserva);
 
        registrarHistorial(reserva, usuario, null, "PENDIENTE");
 
        return reserva;
    }
 
    private void registrarHistorial(Reserva reserva, Usuario usuario, String anterior, String nuevo) {
        HistorialEstadoReserva historial = new HistorialEstadoReserva();
        historial.setReserva(reserva);
        historial.setUsuario(usuario);
        historial.setEstadoAnterior(anterior);
        historial.setEstadoNuevo(nuevo);
        historialRepository.save(historial);
    }
 
    private Optional<Cliente> buscarCliente(String correoUsuario) {
        if (correoUsuario == null) {
            return Optional.empty();
        }
        return usuarioRepository.findByCorreo(correoUsuario)
                .flatMap(u -> clienteRepository.findByUsuarioId(u.getId()));
    }
 
    private void validarDatos(LocalDate fechaInicio, String origen, List<PasajeroDatos> pasajeros) {
        if (fechaInicio == null || !fechaInicio.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Elige una fecha de viaje posterior a hoy.");
        }
        if (origen == null || origen.isBlank()) {
            throw new IllegalArgumentException("Indica la ciudad de salida.");
        }
        if (pasajeros == null || pasajeros.isEmpty()) {
            throw new IllegalArgumentException("Agrega al menos un pasajero.");
        }
        if (pasajeros.size() > MAX_PASAJEROS) {
            throw new IllegalArgumentException("Máximo " + MAX_PASAJEROS + " pasajeros por reserva.");
        }
        for (int i = 0; i < pasajeros.size(); i++) {
            PasajeroDatos d = pasajeros.get(i);
            String quien = "Pasajero " + (i + 1) + ": ";
            if (d.getNombre() == null || d.getNombre().isBlank()) {
                throw new IllegalArgumentException(quien + "escribe el nombre completo.");
            }
            if (d.getDocumento() == null || d.getDocumento().isBlank()) {
                throw new IllegalArgumentException(quien + "escribe el documento de identidad.");
            }
            if (d.getNombre().trim().length() > 120 || d.getDocumento().trim().length() > 30) {
                throw new IllegalArgumentException(quien + "el nombre o el documento es demasiado largo.");
            }
            if (d.getFechaNacimiento() != null && !d.getFechaNacimiento().isBefore(LocalDate.now())) {
                throw new IllegalArgumentException(quien + "la fecha de nacimiento no es válida.");
            }
        }
    }
 
    // Ejemplo: TZ-2026-K7M3QX
    private String generarCodigo() {
        String codigo;
        do {
            StringBuilder sb = new StringBuilder("TZ-").append(LocalDate.now().getYear()).append('-');
            for (int i = 0; i < 6; i++) {
                sb.append(ALFABETO.charAt(random.nextInt(ALFABETO.length())));
            }
            codigo = sb.toString();
        } while (reservaRepository.existsByCodigoUnico(codigo));
        return codigo;
    }
 
    private String limitar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max);
    }
 
    private String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
 