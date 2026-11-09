package com.std.ec.PrimerSpring.service;
 
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
 
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import com.std.ec.PrimerSpring.model.Comprobante;
import com.std.ec.PrimerSpring.model.Pago;
import com.std.ec.PrimerSpring.model.Reserva;
import com.std.ec.PrimerSpring.model.Usuario;
import com.std.ec.PrimerSpring.repository.ComprobanteRepository;
import com.std.ec.PrimerSpring.repository.PagoRepository;
import com.std.ec.PrimerSpring.repository.ReservaRepository;
import com.std.ec.PrimerSpring.repository.UsuarioRepository;
 
/**
 * Pagos y comprobantes (CU-07 y CU-08). Un pago, el cambio del estado de pago de la reserva
 * y su comprobante se guardan en una sola transacción: si algo falla, no queda nada a medias.
 *
 * Java valida y explica los errores; la base de datos aporta el saldo (fn_saldo_reserva),
 * el número correlativo (fn_siguiente_comprobante) y triggers de respaldo.
 */
@Service
public class PagoService {
 
    private static final List<String> METODOS = List.of("EFECTIVO", "TRANSFERENCIA");
 
    private final PagoRepository pagoRepository;
    private final ComprobanteRepository comprobanteRepository;
    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
 
    public PagoService(PagoRepository pagoRepository,
                       ComprobanteRepository comprobanteRepository,
                       ReservaRepository reservaRepository,
                       UsuarioRepository usuarioRepository) {
        this.pagoRepository = pagoRepository;
        this.comprobanteRepository = comprobanteRepository;
        this.reservaRepository = reservaRepository;
        this.usuarioRepository = usuarioRepository;
    }
 
    // ---------------- Consultas ----------------
 
    @Transactional(readOnly = true)
    public Optional<Reserva> reserva(Integer idReserva) {
        return reservaRepository.detalle(idReserva);
    }
 
    /** Saldo pendiente: monto de la reserva menos los pagos registrados. */
    @Transactional(readOnly = true)
    public BigDecimal saldo(Integer idReserva) {
        BigDecimal saldo = pagoRepository.saldo(idReserva);
        return saldo == null ? BigDecimal.ZERO : saldo;
    }
 
    /** Pagos de la reserva (cada uno con su comprobante), del más reciente al más antiguo. */
    @Transactional(readOnly = true)
    public List<Comprobante> pagosDeReserva(Integer idReserva) {
        return comprobanteRepository.porReserva(idReserva);
    }
 
    @Transactional(readOnly = true)
    public Optional<Comprobante> comprobante(Integer idComprobante) {
        return comprobanteRepository.detalle(idComprobante);
    }
 
    /** Correo de quien registró el pago (agente o administrador). */
    @Transactional(readOnly = true)
    public String atendidoPor(Pago pago) {
        Integer id = pago.getIdAgente() != null ? pago.getIdAgente() : pago.getIdAdministrador();
        if (id == null) {
            return "—";
        }
        return usuarioRepository.findById(id).map(Usuario::getCorreo).orElse("—");
    }
 
    // ---------------- Registrar un pago ----------------
 
    /** Lanza IllegalArgumentException con un mensaje apto para mostrar al personal. */
    @Transactional
    public Comprobante registrarPago(String correoPersonal, Integer idReserva, BigDecimal monto,
                                     String metodo, LocalDate fecha, String concepto) {
 
        // 1. Quién registra: un agente o un administrador, con su ficha en la base
        Usuario usuario = usuarioRepository.findByCorreo(correoPersonal)
                .orElseThrow(() -> new IllegalArgumentException("Inicia sesión para continuar."));
        String rol = usuario.getRol().getNombre();
        boolean esAgente = "AGENTE".equals(rol);
        boolean esAdmin = "ADMIN".equals(rol);
        if (!esAgente && !esAdmin) {
            throw new IllegalArgumentException("Solo el personal de la agencia puede registrar pagos.");
        }
        if (esAgente && reservaRepository.contarAgente(usuario.getId()) == 0) {
            throw new IllegalArgumentException("Tu usuario no tiene ficha de agente, por eso no puede registrar pagos.");
        }
        if (esAdmin && reservaRepository.contarAdministrador(usuario.getId()) == 0) {
            throw new IllegalArgumentException("Tu usuario no tiene ficha de administrador, por eso no puede registrar pagos.");
        }
 
        // 2. La reserva (bloqueada hasta terminar, para que dos cobros no se pisen)
        if (idReserva == null) {
            throw new IllegalArgumentException("No se indicó la reserva.");
        }
        Reserva reserva = reservaRepository.buscarParaActualizar(idReserva)
                .orElseThrow(() -> new IllegalArgumentException("La reserva ya no existe."));
        if ("CANCELADA".equals(reserva.getEstado())) {
            throw new IllegalArgumentException("La reserva " + reserva.getCodigoUnico()
                    + " está cancelada y no admite pagos.");
        }
        if ("FINALIZADA".equals(reserva.getEstado())) {
            throw new IllegalArgumentException("La reserva " + reserva.getCodigoUnico()
                    + " está finalizada y no admite pagos.");
        }
 
        // 3. Datos del pago
        if (monto == null) {
            throw new IllegalArgumentException("Escribe el monto del pago.");
        }
        monto = monto.setScale(2, RoundingMode.HALF_UP);
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero.");
        }
        if (metodo == null || !METODOS.contains(metodo)) {
            throw new IllegalArgumentException("Elige el método de pago (efectivo o transferencia).");
        }
        if (fecha == null) {
            throw new IllegalArgumentException("Indica la fecha del pago.");
        }
        if (fecha.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha del pago no puede ser posterior a hoy.");
        }
 
        // 4. El pago nunca supera el saldo
        BigDecimal saldo = saldo(idReserva);
        if (saldo.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La reserva " + reserva.getCodigoUnico() + " ya está pagada por completo.");
        }
        if (monto.compareTo(saldo) > 0) {
            throw new IllegalArgumentException("El pago (" + usd(monto) + ") supera el saldo pendiente (" + usd(saldo) + ").");
        }
 
        String textoConcepto = concepto == null ? "" : concepto.trim();
        if (textoConcepto.isEmpty()) {
            textoConcepto = "Pago de la reserva " + reserva.getCodigoUnico();
        }
        if (textoConcepto.length() > 150) {
            textoConcepto = textoConcepto.substring(0, 150);
        }
 
        // 5. Se guarda el pago (el trigger de la base lo revisa otra vez al insertar)
        Pago pago = new Pago();
        pago.setReserva(reserva);
        if (esAgente) {
            pago.setIdAgente(usuario.getId());
        } else {
            pago.setIdAdministrador(usuario.getId());
        }
        pago.setMonto(monto);
        pago.setFecha(fecha);
        pago.setMetodo(metodo);
        pago.setConcepto(textoConcepto);
        pagoRepository.saveAndFlush(pago);
 
        // 6. Nuevo saldo y estado de pago de la reserva
        BigDecimal nuevoSaldo = saldo(idReserva);
        reserva.setEstadoPago(nuevoSaldo.compareTo(BigDecimal.ZERO) <= 0 ? "COMPLETADO" : "PARCIAL");
        reservaRepository.save(reserva);
 
        // 7. Comprobante con número correlativo
        Comprobante comprobante = new Comprobante();
        comprobante.setPago(pago);
        comprobante.setNumeroCorrelativo(comprobanteRepository.siguienteNumero());
        comprobante.setImporte(monto);
        comprobante.setSaldoRestante(nuevoSaldo);
        comprobante.setConcepto(textoConcepto);
        comprobanteRepository.save(comprobante);
 
        return comprobante;
    }
 
    private static String usd(BigDecimal valor) {
        return "USD " + valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}