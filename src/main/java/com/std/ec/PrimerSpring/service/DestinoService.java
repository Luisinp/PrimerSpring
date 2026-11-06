package com.std.ec.PrimerSpring.service;
 
import java.util.List;
 
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import com.std.ec.PrimerSpring.model.Destino;
import com.std.ec.PrimerSpring.repository.DestinoRepository;
 
/** Gestión de destinos turísticos (RF01). La usan el agente y el administrador. */
@Service
public class DestinoService {
 
    private final DestinoRepository destinoRepository;
 
    public DestinoService(DestinoRepository destinoRepository) {
        this.destinoRepository = destinoRepository;
    }
 
    @Transactional(readOnly = true)
    public List<Destino> buscar(String texto, String estado) {
        String t = texto == null ? "" : texto.trim();
        List<Boolean> estados;
        if ("ACTIVOS".equals(estado)) {
            estados = List.of(true);
        } else if ("INACTIVOS".equals(estado)) {
            estados = List.of(false);
        } else {
            estados = List.of(true, false);
        }
        return destinoRepository.buscar(t, estados);
    }
 
    /** Para el selector de destino en el formulario de paquetes. */
    @Transactional(readOnly = true)
    public List<Destino> listarActivos() {
        return destinoRepository.findByActivoTrueOrderByNombre();
    }
 
    @Transactional
    public void crear(String nombre, String pais, String descripcion) {
        String n = nombre.trim();
        String p = pais.trim();
        if (destinoRepository.existsByNombreIgnoreCaseAndPaisIgnoreCase(n, p)) {
            throw new IllegalArgumentException("Ese destino ya está registrado.");
        }
        Destino d = new Destino();
        d.setNombre(n);
        d.setPais(p);
        d.setDescripcion(vacioANulo(descripcion));
        d.setActivo(true);
        destinoRepository.save(d);
    }
 
    @Transactional
    public void actualizar(Integer id, String nombre, String pais, String descripcion) {
        Destino d = destinoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El destino no existe."));
        String n = nombre.trim();
        String p = pais.trim();
        boolean cambioClave = !n.equalsIgnoreCase(d.getNombre()) || !p.equalsIgnoreCase(d.getPais());
        if (cambioClave && destinoRepository.existsByNombreIgnoreCaseAndPaisIgnoreCase(n, p)) {
            throw new IllegalArgumentException("Ya existe otro destino con ese nombre y país.");
        }
        d.setNombre(n);
        d.setPais(p);
        d.setDescripcion(vacioANulo(descripcion));
        destinoRepository.save(d);
    }
 
    /** Devuelve el estado final (true = activo). */
    @Transactional
    public boolean cambiarEstado(Integer id) {
        Destino d = destinoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El destino no existe."));
        d.setActivo(!Boolean.TRUE.equals(d.getActivo()));
        destinoRepository.save(d);
        return d.getActivo();
    }
 
    @Transactional
    public void eliminar(Integer id) {
        Destino d = destinoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El destino no existe."));
        if (destinoRepository.contarPaquetes(id) > 0) {
            throw new IllegalArgumentException(
                    "No se puede eliminar: tiene paquetes asociados. Desactívalo si ya no se usa.");
        }
        destinoRepository.delete(d);
    }
 
    private String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
 