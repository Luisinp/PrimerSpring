package com.std.ec.PrimerSpring.service;
 
import java.math.BigDecimal;
import java.util.List;
 
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import com.std.ec.PrimerSpring.model.Destino;
import com.std.ec.PrimerSpring.model.PaqueteTuristico;
import com.std.ec.PrimerSpring.repository.DestinoRepository;
import com.std.ec.PrimerSpring.repository.PaqueteRepository;
 
/** Gestión de paquetes turísticos (RF02, CU-06). La usan el agente y el administrador. */
@Service
public class PaqueteService {
 
    private final PaqueteRepository paqueteRepository;
    private final DestinoRepository destinoRepository;
    private final ImagenService imagenService;
 
    public PaqueteService(PaqueteRepository paqueteRepository,
                          DestinoRepository destinoRepository,
                          ImagenService imagenService) {
        this.paqueteRepository = paqueteRepository;
        this.destinoRepository = destinoRepository;
        this.imagenService = imagenService;
    }
 
    /** estado: TODOS, ACTIVOS, INACTIVOS. publicacion: TODOS, PUBLICADOS, BORRADORES. */
    @Transactional(readOnly = true)
    public List<PaqueteTuristico> buscar(String texto, String estado, String publicacion) {
        String t = texto == null ? "" : texto.trim();
        List<Boolean> estados;
        if ("ACTIVOS".equals(estado)) {
            estados = List.of(true);
        } else if ("INACTIVOS".equals(estado)) {
            estados = List.of(false);
        } else {
            estados = List.of(true, false);
        }
        String pub = "PUBLICADOS".equals(publicacion) || "BORRADORES".equals(publicacion) ? publicacion : "TODOS";
        return paqueteRepository.buscar(t, estados, pub);
    }
 
    /** El paquete se crea como borrador: se publica con el botón Publicar. */
    @Transactional
    public void crear(String nombre, Integer idDestino, Integer duracionDias, String descripcion,
                      String servicios, BigDecimal precio, Integer cupos,
                      byte[] imagen, String nombreImagen) {
 
        Destino destino = validar(nombre, idDestino, duracionDias, precio, cupos);
 
        String archivo = null;
        if (imagen != null && imagen.length > 0) {
            archivo = imagenService.guardar(imagen, nombreImagen);
        }
        try {
            PaqueteTuristico p = new PaqueteTuristico();
            asignar(p, nombre, destino, duracionDias, descripcion, servicios, precio, cupos);
            p.setImagenPortada(archivo);
            p.setPublicado(false);
            p.setActivo(true);
            paqueteRepository.save(p);
        } catch (RuntimeException e) {
            imagenService.borrar(archivo);   // no dejar la foto suelta si falló el guardado
            throw e;
        }
    }
 
    /** Si no se envía imagen nueva, se conserva la actual. */
    @Transactional
    public void actualizar(Integer id, String nombre, Integer idDestino, Integer duracionDias,
                           String descripcion, String servicios, BigDecimal precio, Integer cupos,
                           byte[] imagen, String nombreImagen) {
 
        PaqueteTuristico p = paqueteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El paquete no existe."));
        Destino destino = validar(nombre, idDestino, duracionDias, precio, cupos);
 
        String anterior = p.getImagenPortada();
        String nueva = null;
        if (imagen != null && imagen.length > 0) {
            nueva = imagenService.guardar(imagen, nombreImagen);
        }
        try {
            asignar(p, nombre, destino, duracionDias, descripcion, servicios, precio, cupos);
            if (nueva != null) {
                p.setImagenPortada(nueva);
            }
            paqueteRepository.save(p);
        } catch (RuntimeException e) {
            imagenService.borrar(nueva);
            throw e;
        }
        if (nueva != null) {
            imagenService.borrar(anterior);
        }
    }
 
    /** Publica u oculta del catálogo. Solo se puede publicar un paquete activo. Devuelve el estado final. */
    @Transactional
    public boolean cambiarPublicacion(Integer id) {
        PaqueteTuristico p = paqueteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El paquete no existe."));
        boolean publicar = !Boolean.TRUE.equals(p.getPublicado());
        if (publicar && !Boolean.TRUE.equals(p.getActivo())) {
            throw new IllegalArgumentException("Activa el paquete antes de publicarlo.");
        }
        p.setPublicado(publicar);
        paqueteRepository.save(p);
        return publicar;
    }
 
    /** Desactivar también lo oculta del catálogo. Devuelve el estado final (true = activo). */
    @Transactional
    public boolean cambiarEstado(Integer id) {
        PaqueteTuristico p = paqueteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El paquete no existe."));
        boolean activar = !Boolean.TRUE.equals(p.getActivo());
        p.setActivo(activar);
        if (!activar) {
            p.setPublicado(false);
        }
        paqueteRepository.save(p);
        return activar;
    }
 
    /** Solo se elimina si no tiene reservas. Si las tiene, hay que desactivarlo. */
    @Transactional
    public void eliminar(Integer id) {
        PaqueteTuristico p = paqueteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El paquete no existe."));
        if (paqueteRepository.contarReservas(id) > 0) {
            throw new IllegalArgumentException(
                    "No se puede eliminar: tiene reservas asociadas. Desactívalo para conservarlas.");
        }
        String imagen = p.getImagenPortada();
        paqueteRepository.delete(p);
        paqueteRepository.flush();
        imagenService.borrar(imagen);
    }
 
    // ---------------- Auxiliares ----------------
 
    private Destino validar(String nombre, Integer idDestino, Integer duracionDias,
                            BigDecimal precio, Integer cupos) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }
        if (precio == null || precio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor que cero.");
        }
        if (duracionDias == null || duracionDias <= 0) {
            throw new IllegalArgumentException("La duración debe ser de al menos 1 día.");
        }
        if (cupos != null && cupos < 0) {
            throw new IllegalArgumentException("Los cupos no pueden ser negativos.");
        }
        Destino destino = destinoRepository.findById(idDestino == null ? -1 : idDestino)
                .orElseThrow(() -> new IllegalArgumentException("Elige un destino válido."));
        if (!Boolean.TRUE.equals(destino.getActivo())) {
            throw new IllegalArgumentException("El destino elegido está inactivo.");
        }
        return destino;
    }
 
    private void asignar(PaqueteTuristico p, String nombre, Destino destino, Integer duracionDias,
                         String descripcion, String servicios, BigDecimal precio, Integer cupos) {
        p.setNombre(nombre.trim());
        p.setDestino(destino);
        p.setDuracionDias(duracionDias);
        p.setDescripcion(vacioANulo(descripcion));
        p.setServiciosIncluidos(vacioANulo(servicios));
        p.setPrecio(precio);
        p.setCuposDisponibles(cupos == null ? 0 : cupos);
    }
 
    private String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
 