package pe.edu.upeu.lamb.admision.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lamb.admision.entity.EstadoPostulante;
import pe.edu.upeu.lamb.admision.entity.Postulante;
import pe.edu.upeu.lamb.admision.exception.BusinessRuleException;
import pe.edu.upeu.lamb.admision.exception.ResourceNotFoundException;
import pe.edu.upeu.lamb.admision.port.FinanzasPort;
import pe.edu.upeu.lamb.admision.repository.PostulanteRepository;

import java.math.BigDecimal;

/**
 * Servicio del admin: formaliza un postulante PRE_INSCRITO. Marca FORMALIZADO
 * y crea el cargo del derecho de admision via FinanzasPort. El postulante
 * pasara a PAGADO cuando Finanzas confirme el pago (evento).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FormalizacionService {

    private static final BigDecimal MONTO_DERECHO_ADMISION = new BigDecimal("450.00");

    private final PostulanteRepository postulanteRepo;
    private final FinanzasPort finanzasPort;

    @Transactional
    public Long formalizar(Long idPostulante) {
        Postulante p = postulanteRepo.findById(idPostulante)
                .orElseThrow(() -> new ResourceNotFoundException("Postulante no encontrado: " + idPostulante));
        if (p.getEstado() != EstadoPostulante.PRE_INSCRITO) {
            throw new BusinessRuleException("Solo se formaliza un postulante en PRE_INSCRITO (actual: " + p.getEstado() + ")");
        }
        String periodo = p.getConvocatoria().getPeriodo();
        Long idCargo = finanzasPort.crearCargoDerechoAdmision(p.getId(), MONTO_DERECHO_ADMISION, periodo);
        p.setEstado(EstadoPostulante.FORMALIZADO);
        log.info("component=formalizacion postulante={} cargo={} periodo={}", p.getId(), idCargo, periodo);
        return idCargo;
    }
}
