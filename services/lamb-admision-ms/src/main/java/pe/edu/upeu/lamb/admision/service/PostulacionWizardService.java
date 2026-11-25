package pe.edu.upeu.lamb.admision.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lamb.admision.dto.PostulacionWizardRequest;
import pe.edu.upeu.lamb.admision.dto.PostulacionWizardResponse;
import pe.edu.upeu.lamb.admision.entity.*;
import pe.edu.upeu.lamb.admision.event.PostulanteAdmitidoEvento;
import pe.edu.upeu.lamb.admision.exception.BusinessRuleException;
import pe.edu.upeu.lamb.admision.exception.ResourceNotFoundException;
import pe.edu.upeu.lamb.admision.messaging.AdmisionEventosProducer;
import pe.edu.upeu.lamb.admision.port.PersonaPort;
import pe.edu.upeu.lamb.admision.repository.ConvocatoriaRepository;
import pe.edu.upeu.lamb.admision.repository.ModalidadAdmisionRepository;
import pe.edu.upeu.lamb.admision.repository.PostulanteRepository;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

/**
 * Orquesta la creación completa de un postulante en una sola transacción.
 *
 * Toca 2-3 entidades + publica evento Kafka. Si cualquier paso falla, rollback de todo.
 *
 * Flujo:
 *   1. Resuelve idPersona (temporal: hash del DNI hasta que exista lamb-personas-ms)
 *   2. Crea Postulante con código autogenerado
 *   3. Publica evento 'postulante.creado' tras commit
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostulacionWizardService {

    private final PostulanteRepository postulanteRepo;
    private final ConvocatoriaRepository convocatoriaRepo;
    private final ModalidadAdmisionRepository modalidadRepo;
    private final AdmisionEventosProducer eventosProducer;
    private final EmailService emailService;
    private final PersonaPort personaPort;

    @Transactional
    public PostulacionWizardResponse registrar(PostulacionWizardRequest r) {
        // Paso 1 — resolver idPersona via puerto (hoy local; manana ms Personas)
        Long idPersona = personaPort.obtenerOCrearPersona(new PersonaPort.DatosPersona(
                r.getDni(), r.getNombres(), r.getApellidoPaterno(), r.getApellidoMaterno(),
                null, null, r.getCorreoContacto(), r.getTelefonoContacto()));

        // Paso 2 — validar y cargar catálogos
        Convocatoria conv = convocatoriaRepo.findById(r.getIdConvocatoria())
                .orElseThrow(() -> new ResourceNotFoundException("Convocatoria no encontrada: " + r.getIdConvocatoria()));
        if (conv.getEstado() != EstadoConvocatoria.ABIERTA) {
            throw new BusinessRuleException("La convocatoria " + conv.getCodigo() + " no está ABIERTA");
        }
        ModalidadAdmision mod = modalidadRepo.findById(r.getIdModalidad())
                .orElseThrow(() -> new ResourceNotFoundException("Modalidad no encontrada: " + r.getIdModalidad()));

        if (postulanteRepo.existsByIdPersonaAndConvocatoriaId(idPersona, conv.getId())) {
            throw new BusinessRuleException("DNI " + r.getDni() + " ya tiene una postulación en esta convocatoria");
        }

        // Paso 3 — autogenerar código y guardar
        String codigo = generarCodigo(conv);
        Postulante p = Postulante.builder()
                .idPersona(idPersona)
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
        Postulante guardado = postulanteRepo.save(p);
        log.info("wizard=postulacion idPostulante={} codigo={} idPersona={} dni={} status=creado",
                guardado.getId(), codigo, idPersona, r.getDni());

        // Paso 4 — publicar evento tras commit (otros módulos reaccionan)
        PostulanteAdmitidoEvento evento = PostulanteAdmitidoEvento.builder()
                .tipoEvento("postulante.pre-inscrito")
                .postulacionId(guardado.getId())
                .idPersona(idPersona)
                .idPrograma(r.getIdPrograma())
                .periodo(conv.getPeriodo())
                .puntajeExamen(null) // aún no evaluado
                .origen("wizard-admision")
                .timestamp(Instant.now().toEpochMilli())
                .build();
        eventosProducer.publicarTrasCommit(evento);

        // Paso 5 — armar respuesta
        String nombreCompleto = String.join(" ",
                nz(r.getNombres()), nz(r.getApellidoPaterno()), nz(r.getApellidoMaterno())).trim();
        String nombreFinal = nombreCompleto.isBlank() ? "DNI " + r.getDni() : nombreCompleto;

        // Paso 6 — encolar envío de correo DESPUÉS del commit
        // Si la BD falla y rollback, el correo nunca se envía.
        boolean correoProgramado = false;
        if (r.getCorreoContacto() != null && !r.getCorreoContacto().isBlank()) {
            final String emailDestino = r.getCorreoContacto();
            final String periodoFinal = conv.getPeriodo();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    String html = emailService.plantillaBienvenidaPostulante(nombreFinal, codigo, periodoFinal);
                    emailService.enviarHtml(emailDestino, nombreFinal,
                            "Bienvenido a UPeU - Código " + codigo, html);
                }
            });
            correoProgramado = true;
        }

        return PostulacionWizardResponse.builder()
                .idPostulante(guardado.getId())
                .idPersona(idPersona)
                .codigoPostulante(codigo)
                .nombreCompleto(nombreFinal)
                .estado(guardado.getEstado().name())
                .periodo(conv.getPeriodo())
                .mensajeConfirmacion("Postulación registrada correctamente. Guarde el código " + codigo + " para seguimiento.")
                .correoProgramado(correoProgramado)
                .correoDestino(r.getCorreoContacto())
                .build();
    }

    private String generarCodigo(Convocatoria conv) {
        String periodo = conv.getPeriodo() == null ? "0000-0" : conv.getPeriodo();
        long base = postulanteRepo.countByConvocatoriaId(conv.getId());
        for (int intento = 1; intento <= 20; intento++) {
            String candidato = String.format("POS-%s-%04d", periodo, base + intento);
            if (!postulanteRepo.existsByCodigoPostulante(candidato)) return candidato;
        }
        throw new IllegalStateException("No se pudo generar codigo unico para convocatoria " + conv.getId());
    }

    private String nz(String s) { return s == null ? "" : s; }
}
