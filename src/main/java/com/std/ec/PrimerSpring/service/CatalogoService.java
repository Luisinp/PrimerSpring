package com.std.ec.PrimerSpring.service;
 
import java.util.List;
import java.util.Optional;
 
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import com.std.ec.PrimerSpring.model.Destino;
import com.std.ec.PrimerSpring.model.PaqueteTuristico;
import com.std.ec.PrimerSpring.repository.DestinoRepository;
import com.std.ec.PrimerSpring.repository.PaqueteRepository;
 
/** Consultas del catálogo público (RF03). Solo lectura, sin necesidad de sesión. */
@Service
public class CatalogoService {
 
    private final PaqueteRepository paqueteRepository;
    private final DestinoRepository destinoRepository;
 
    public CatalogoService(PaqueteRepository paqueteRepository, DestinoRepository destinoRepository) {
        this.paqueteRepository = paqueteRepository;
        this.destinoRepository = destinoRepository;
    }
 
    /** idDestino = 0 para todos los destinos; texto vacío para no filtrar por texto. */
    @Transactional(readOnly = true)
    public List<PaqueteTuristico> catalogo(int idDestino, String texto) {
        return paqueteRepository.catalogo(idDestino, texto == null ? "" : texto.trim());
    }
 
    @Transactional(readOnly = true)
    public Optional<PaqueteTuristico> detalle(Integer id) {
        if (id == null) {
            return Optional.empty();
        }
        return paqueteRepository.buscarPublicado(id);
    }
 
    @Transactional(readOnly = true)
    public List<Destino> destinosConPaquetes() {
        return destinoRepository.conPaquetesPublicados();
    }
}
 