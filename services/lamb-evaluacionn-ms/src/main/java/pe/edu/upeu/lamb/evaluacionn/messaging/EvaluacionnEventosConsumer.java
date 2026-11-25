package pe.edu.upeu.lamb.evaluacionn.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pe.edu.upeu.lamb.evaluacionn.event.EvaluacionnEvento;

@Slf4j
@Component
public class EvaluacionnEventosConsumer {

    @KafkaListener(topics = "${app.kafka.topic.evaluacionn}")
    public void alRecibirEvento(EvaluacionnEvento evento) {
        log.info("component=consumer eventType={} id={} mensaje=\"{}\" status=consumed",
                evento.getTipoEvento(), evento.getId(), evento.getMensaje());
    }
}
