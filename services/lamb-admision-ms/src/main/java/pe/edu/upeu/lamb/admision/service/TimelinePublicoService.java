package pe.edu.upeu.lamb.admision.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lamb.admision.dto.TimelineResponse;
import pe.edu.upeu.lamb.admision.dto.TimelineResponse.EventoTimeline;
import pe.edu.upeu.lamb.admision.entity.EstadoPostulante;
import pe.edu.upeu.lamb.admision.entity.Postulante;
import pe.edu.upeu.lamb.admision.exception.ResourceNotFoundException;
import pe.edu.upeu.lamb.admision.port.PersonaPort;
import pe.edu.upeu.lamb.admision.repository.CalificacionRepository;
import pe.edu.upeu.lamb.admision.repository.PostulanteRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Construye el timeline consultable publicamente por DNI (sin login).
 */
@Service
@RequiredArgsConstructor
public class TimelinePublicoService {

    private final PersonaPort personaPort;
    private final PostulanteRepository postulanteRepo;
    private final CalificacionRepository calificacionRepo;

    @Transactional(readOnly = true)
    public TimelineResponse porDni(String dni) {
        PersonaPort.PersonaInfo persona = personaPort.consultarPorDni(dni)
                .orElseThrow(() -> new ResourceNotFoundException("No existe postulacion para el DNI " + dni));
        Postulante p = postulanteRepo.findFirstByIdPersonaOrderByFechaRegistroDesc(persona.id())
                .orElseThrow(() -> new ResourceNotFoundException("No existe postulacion para el DNI " + dni));

        BigDecimal puntaje = calificacionRepo.findByPostulanteId(p.getId()).stream()
                .findFirst()
                .map(c -> c.getPuntajeTotal())
                .orElse(null);

        EstadoPostulante e = p.getEstado();
        List<EventoTimeline> items = new ArrayList<>();
        items.add(evento("Pre-inscripcion recibida", "Codigo " + p.getCodigoPostulante(),
                p.getFechaRegistro(), alcanzado(e, EstadoPostulante.PRE_INSCRITO)));
        items.add(evento("Documentos entregados y formalizacion", "Admin recibio sus documentos",
                null, alcanzado(e, EstadoPostulante.FORMALIZADO)));
        items.add(evento("Derecho de admision pagado", "Finanzas confirmo el pago",
                null, alcanzado(e, EstadoPostulante.PAGADO)));
        items.add(evento("Apto para examen", "Puede rendir el examen",
                null, alcanzado(e, EstadoPostulante.APTO_EXAMEN)));
        items.add(evento("Examen calificado",
                puntaje != null ? "Puntaje: " + puntaje : "Pendiente de calificacion",
                null, alcanzado(e, EstadoPostulante.CALIFICADO)));
        items.add(evento("Resultado final",
                e == EstadoPostulante.ADMITIDO ? "ADMITIDO" :
                        (e == EstadoPostulante.NO_ADMITIDO ? "NO ADMITIDO" : "Pendiente de adjudicacion"),
                null, e == EstadoPostulante.ADMITIDO || e == EstadoPostulante.NO_ADMITIDO));

        return TimelineResponse.builder()
                .dni(dni)
                .nombreCompleto(persona.nombreCompleto())
                .idPostulante(p.getId())
                .codigoPostulante(p.getCodigoPostulante())
                .estado(e.name())
                .periodo(p.getConvocatoria() != null ? p.getConvocatoria().getPeriodo() : null)
                .puntajeExamen(puntaje)
                .timeline(items)
                .build();
    }

    private EventoTimeline evento(String etiqueta, String detalle, java.time.Instant fecha, boolean cumplido) {
        return EventoTimeline.builder().etiqueta(etiqueta).detalle(detalle).fecha(fecha).cumplido(cumplido).build();
    }

    private boolean alcanzado(EstadoPostulante actual, EstadoPostulante objetivo) {
        return ordinal(actual) >= ordinal(objetivo);
    }

    private int ordinal(EstadoPostulante e) {
        return switch (e) {
            case PRE_INSCRITO -> 1;
            case FORMALIZADO -> 2;
            case PAGADO -> 3;
            case APTO_EXAMEN -> 4;
            case CALIFICADO -> 5;
            case ADMITIDO, NO_ADMITIDO -> 6;
            case ANULADO -> 0;
        };
    }
}
