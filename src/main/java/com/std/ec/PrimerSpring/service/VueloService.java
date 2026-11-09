package com.std.ec.PrimerSpring.service;
 
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
 
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import com.std.ec.PrimerSpring.model.Vuelo;
import com.std.ec.PrimerSpring.repository.VueloRepository;
 
/**
 * Gestión de vuelos (CU-05): registrar, actualizar tarifa y disponibilidad, desactivar y eliminar.
 * Un vuelo desactivado deja de recibir reservas nuevas pero conserva las existentes;
 * un vuelo con reservas asociadas no se elimina.
 */
@Service
public class VueloService {
 
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final BigDecimal TARIFA_MAXIMA = new BigDecimal("99999999.99");
    private static final int ASIENTOS_MAXIMOS = 1000;
 
    private final VueloRepository vueloRepository;
 
    public VueloService(VueloRepository vueloRepository) {
        this.vueloRepository = vueloRepository;
    }
 
    // ---------------- Consultas ----------------
 
    /** estado: TODOS, ACTIVOS o INACTIVOS. texto = "" no filtra. */
    @Transactional(readOnly = true)
    public List<Vuelo> buscar(String texto, String estado) {
        String t = texto == null ? "" : texto.trim();
        List<Boolean> estados;
        if ("ACTIVOS".equals(estado)) {
            estados = List.of(Boolean.TRUE);
        } else if ("INACTIVOS".equals(estado)) {
            estados = List.of(Boolean.FALSE);
        } else {
            estados = List.of(Boolean.TRUE, Boolean.FALSE);
        }
        return vueloRepository.buscar(t, estados);
    }
 
    @Transactional(readOnly = true)
    public long contarActivos() {
        return vueloRepository.countByActivo(Boolean.TRUE);
    }
 
    @Transactional(readOnly = true)
    public long contarInactivos() {
        return vueloRepository.countByActivo(Boolean.FALSE);
    }
 
    @Transactional(readOnly = true)
    public Optional<Vuelo> obtener(Integer id) {
        return vueloRepository.findById(id);
    }
 
    // ---------------- Registrar y actualizar ----------------
 
    /**
     * Crea el vuelo (id null) o actualiza el existente.
     * Lanza IllegalArgumentException con un mensaje apto para mostrar al personal.
     */
    @Transactional
    public Vuelo guardar(Integer id, String aerolinea, String origen, String destino,
                         LocalDate fecha, String hora, BigDecimal tarifa, Integer asientos) {
 
        String a = limpiar(aerolinea);
        String o = limpiar(origen);
        String d = limpiar(destino);
        if (a.isEmpty()) {
            throw new IllegalArgumentException("Escribe la aerolínea (proveedor).");
        }
        if (o.isEmpty()) {
            throw new IllegalArgumentException("Escribe la ciudad de origen.");
        }
        if (d.isEmpty()) {
            throw new IllegalArgumentException("Escribe la ciudad de destino.");
        }
        if (a.length() > 80 || o.length() > 80 || d.length() > 80) {
            throw new IllegalArgumentException("La aerolínea, el origen y el destino admiten hasta 80 caracteres.");
        }
        if (o.equalsIgnoreCase(d)) {
            throw new IllegalArgumentException("El origen y el destino no pueden ser iguales.");
        }
        if (fecha == null) {
            throw new IllegalArgumentException("Indica la fecha del vuelo.");
        }
        LocalTime h = parsearHora(hora);
 
        if (tarifa == null) {
            throw new IllegalArgumentException("Escribe la tarifa por pasajero.");
        }
        BigDecimal t = tarifa.setScale(2, RoundingMode.HALF_UP);
        if (t.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La tarifa debe ser mayor que cero.");
        }
        if (t.compareTo(TARIFA_MAXIMA) > 0) {
            throw new IllegalArgumentException("La tarifa es demasiado alta.");
        }
        if (asientos == null) {
            throw new IllegalArgumentException("Indica los asientos disponibles.");
        }
        if (asientos < 0) {
            throw new IllegalArgumentException("Los asientos disponibles no pueden ser negativos.");
        }
        if (asientos > ASIENTOS_MAXIMOS) {
            throw new IllegalArgumentException("Los asientos disponibles no pueden superar " + ASIENTOS_MAXIMOS + ".");
        }
 
        Vuelo vuelo;
        if (id == null) {
            if (fecha.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("La fecha del vuelo no puede ser anterior a hoy.");
            }
            vuelo = new Vuelo();
            vuelo.setActivo(true);
        } else {
            vuelo = vueloRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("El vuelo ya no existe."));
            // Un vuelo ya registrado puede conservar su fecha; si se cambia, debe ser futura
            if (!fecha.equals(vuelo.getFecha()) && fecha.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("La nueva fecha del vuelo no puede ser anterior a hoy.");
            }
        }
 
        vuelo.setAerolinea(a);
        vuelo.setOrigen(o);
        vuelo.setDestino(d);
        vuelo.setFecha(fecha);
        vuelo.setHora(h);
        vuelo.setTarifa(t);
        vuelo.setAsientosDisponibles(asientos);
        return vueloRepository.save(vuelo);
    }
 
    // ---------------- Desactivar, activar y eliminar ----------------
 
    /** Un vuelo inactivo no recibe reservas nuevas, pero conserva las existentes. */
    @Transactional
    public Vuelo cambiarActivo(Integer id, boolean activo) {
        Vuelo vuelo = vueloRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El vuelo ya no existe."));
        vuelo.setActivo(activo);
        return vueloRepository.save(vuelo);
    }
 
    /** No se elimina un vuelo con reservas asociadas: se desactiva. */
    @Transactional
    public void eliminar(Integer id) {
        Vuelo vuelo = vueloRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El vuelo ya no existe."));
        long reservas = vueloRepository.contarReservas(id);
        if (reservas > 0) {
            throw new IllegalArgumentException("No se puede eliminar el vuelo " + vuelo.getOrigen() + " a "
                    + vuelo.getDestino() + ": tiene " + reservas
                    + " reserva(s) asociada(s). Puedes desactivarlo para que no reciba reservas nuevas.");
        }
        vueloRepository.delete(vuelo);
    }
 
    // ---------------- Auxiliares ----------------
 
    private static String limpiar(String texto) {
        return texto == null ? "" : texto.trim();
    }
 
    private static LocalTime parsearHora(String hora) {
        if (hora == null || hora.isBlank()) {
            throw new IllegalArgumentException("Indica la hora del vuelo (formato HH:mm).");
        }
        try {
            return LocalTime.parse(hora.trim(), HORA);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La hora debe tener el formato HH:mm, por ejemplo 08:30.");
        }
    }
}
 