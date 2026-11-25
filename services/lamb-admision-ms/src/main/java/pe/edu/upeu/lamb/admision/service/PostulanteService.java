package pe.edu.upeu.lamb.admision.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lamb.admision.dto.PostulanteRequest;
import pe.edu.upeu.lamb.admision.dto.PostulanteResponse;
import pe.edu.upeu.lamb.admision.entity.*;
import pe.edu.upeu.lamb.admision.exception.BusinessRuleException;
import pe.edu.upeu.lamb.admision.exception.ResourceNotFoundException;
import pe.edu.upeu.lamb.admision.mapper.PostulanteMapper;
import pe.edu.upeu.lamb.admision.port.FinanzasPort;
import pe.edu.upeu.lamb.admision.repository.ConvocatoriaRepository;
import pe.edu.upeu.lamb.admision.repository.ModalidadAdmisionRepository;
import pe.edu.upeu.lamb.admision.repository.PostulanteRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostulanteService {

    private final PostulanteRepository repo;
    private final ConvocatoriaRepository convocatoriaRepo;
    private final ModalidadAdmisionRepository modalidadRepo;
    private final PostulanteMapper mapper;
    private final FinanzasPort finanzasPort;

    private static final BigDecimal MONTO_DERECHO_ADMISION = new BigDecimal("450.00");

    @Transactional(readOnly = true)
    public List<PostulanteResponse> listar(Long idConvocatoria) {
        List<Postulante> data = idConvocatoria != null
                ? repo.findByConvocatoriaId(idConvocatoria)
                : repo.findAll();
        return data.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PostulanteResponse obtener(Long id) {
        return mapper.toResponse(buscar(id));
    }

    @Transactional
    public PostulanteResponse crear(PostulanteRequest r) {
        if (repo.existsByIdPersonaAndConvocatoriaId(r.getIdPersona(), r.getIdConvocatoria())) {
            throw new BusinessRuleException("La persona ya esta registrada en esta convocatoria");
        }
        Convocatoria conv = convocatoriaRepo.findById(r.getIdConvocatoria())
                .orElseThrow(() -> new ResourceNotFoundException("Convocatoria no encontrada: " + r.getIdConvocatoria()));
        if (conv.getEstado() != EstadoConvocatoria.ABIERTA) {
            throw new BusinessRuleException("La convocatoria " + conv.getCodigo() + " no esta ABIERTA");
        }
        ModalidadAdmision mod = modalidadRepo.findById(r.getIdModalidad())
                .orElseThrow(() -> new ResourceNotFoundException("Modalidad no encontrada: " + r.getIdModalidad()));

        // Autogenerar codigo si no viene en el request (secretaria no lo escribe)
        String codigo = (r.getCodigoPostulante() == null || r.getCodigoPostulante().isBlank())
                ? generarCodigo(conv)
                : r.getCodigoPostulante();
        if (repo.existsByCodigoPostulante(codigo)) {
            throw new BusinessRuleException("Ya existe un postulante con codigo " + codigo);
        }

        Postulante p = Postulante.builder()
                .idPersona(r.getIdPersona())
                .convocatoria(conv)
                .idPrograma(r.getIdPrograma())
                .modalidad(mod)
                .codigoPostulante(codigo)
                .colegioProcedencia(r.getColegioProcedencia())
                .anioEgresoColegio(r.getAnioEgresoColegio())
                .promedioColegio(r.getPromedioColegio())
                .telefonoContacto(r.getTelefonoContacto())
                .correoContacto(r.getCorreoContacto())
                .nombreApoderado(r.getNombreApoderado())
                .telefonoApoderado(r.getTelefonoApoderado())
                .estado(EstadoPostulante.PRE_INSCRITO)
                .fechaRegistro(Instant.now())
                .build();
        Postulante guardado = repo.save(p);
        finanzasPort.crearCargoDerechoAdmision(guardado.getId(), MONTO_DERECHO_ADMISION, conv.getPeriodo());
        return mapper.toResponse(guardado);
    }

    @Transactional
    public PostulanteResponse actualizar(Long id, PostulanteRequest r) {
        Postulante p = buscar(id);
        if (repo.existsByCodigoPostulanteAndIdNot(r.getCodigoPostulante(), id)) {
            throw new BusinessRuleException("Ya existe otro postulante con codigo " + r.getCodigoPostulante());
        }
        p.setCodigoPostulante(r.getCodigoPostulante());
        p.setColegioProcedencia(r.getColegioProcedencia());
        p.setAnioEgresoColegio(r.getAnioEgresoColegio());
        p.setPromedioColegio(r.getPromedioColegio());
        p.setTelefonoContacto(r.getTelefonoContacto());
        p.setCorreoContacto(r.getCorreoContacto());
        p.setNombreApoderado(r.getNombreApoderado());
        p.setTelefonoApoderado(r.getTelefonoApoderado());
        return mapper.toResponse(p);
    }

    @Transactional
    public void eliminar(Long id) {
        Postulante p = buscar(id);
        if (p.getEstado() != EstadoPostulante.PRE_INSCRITO) {
            throw new BusinessRuleException("Solo se puede eliminar un postulante en estado PRE_INSCRITO");
        }
        repo.delete(p);
    }

    private Postulante buscar(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Postulante no encontrado: " + id));
    }

    /**
     * Genera un codigo unico por convocatoria: POS-{periodo}-{correlativo4}.
     * Ej: POS-2026-2-0001. El correlativo se calcula contando postulantes de la convocatoria.
     * TODO: cuando haya alta concurrencia, usar una secuencia dedicada en BD para evitar colisiones.
     */
    private String generarCodigo(Convocatoria conv) {
        long correlativo = repo.findByConvocatoriaId(conv.getId()).size() + 1L;
        String periodo = conv.getPeriodo() == null ? "0000-0" : conv.getPeriodo();
        return String.format("POS-%s-%04d", periodo, correlativo);
    }
}
