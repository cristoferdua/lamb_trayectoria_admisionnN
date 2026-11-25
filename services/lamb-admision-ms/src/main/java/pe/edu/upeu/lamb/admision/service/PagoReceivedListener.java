package pe.edu.upeu.lamb.admision.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lamb.admision.entity.EstadoPostulante;
import pe.edu.upeu.lamb.admision.entity.Postulante;
import pe.edu.upeu.lamb.admision.repository.PostulanteRepository;

/**
 * Reacciona cuando Finanzas confirma el pago de un cargo. Hoy lo invoca el
 * controller del adaptador fake; manana lo disparara un @KafkaListener sobre
 * el topico finanzas.cargo-pagado sin tocar nada de este servicio.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PagoReceivedListener {

    private final PostulanteRepository postulanteRepo;

    @Transactional
    public void onCargoPagado(Long idPostulante) {
        Postulante p = postulanteRepo.findById(idPostulante).orElse(null);
        if (p == null) {
            log.warn("component=pago-listener postulante={} status=no-encontrado", idPostulante);
            return;
        }
        if (p.getEstado() == EstadoPostulante.FORMALIZADO) {
            // PAGADO es una transicion conceptual intermedia: en cuanto Finanzas confirma el
            // pago el postulante queda directamente APTO_EXAMEN. No persistimos PAGADO.
            p.setEstado(EstadoPostulante.APTO_EXAMEN);
            log.info("component=pago-listener postulante={} status=apto-examen", p.getId());
        } else {
            log.info("component=pago-listener postulante={} status=ignorado estado={}", p.getId(), p.getEstado());
        }
    }
}
