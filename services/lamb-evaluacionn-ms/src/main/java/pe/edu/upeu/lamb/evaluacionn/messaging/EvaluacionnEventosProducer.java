package pe.edu.upeu.lamb.evaluacionn.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pe.edu.upeu.lamb.evaluacionn.event.EvaluacionnEvento;

@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluacionnEventosProducer {

    private final KafkaTemplate<String, EvaluacionnEvento> kafkaTemplate;

    @Value("${app.kafka.topic.evaluacionn}")
    private String topicEvaluacionn;

    public void publicarTrasCommit(EvaluacionnEvento evento) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enviar(evento);
                }
            });
        } else {
            enviar(evento);
        }
    }

    private void enviar(EvaluacionnEvento evento) {
        kafkaTemplate.send(topicEvaluacionn, String.valueOf(evento.getId()), evento)
                .whenComplete((resultado, ex) -> {
                    if (ex != null) {
                        log.error("component=producer topic={} eventType={} id={} status=error error=\"{}\"",
                                topicEvaluacionn, evento.getTipoEvento(), evento.getId(), ex.getMessage());
                        return;
                    }
                    log.info("component=producer topic={} partition={} offset={} eventType={} id={} status=published",
                            resultado.getRecordMetadata().topic(),
                            resultado.getRecordMetadata().partition(),
                            resultado.getRecordMetadata().offset(),
                            evento.getTipoEvento(), evento.getId());
                });
    }
}
