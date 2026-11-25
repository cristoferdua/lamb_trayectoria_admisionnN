package pe.edu.upeu.lamb.admision.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineResponse {
    private String dni;
    private String nombreCompleto;
    private Long idPostulante;
    private String codigoPostulante;
    private String estado;
    private String periodo;
    private BigDecimal puntajeExamen;
    private List<EventoTimeline> timeline;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventoTimeline {
        private String etiqueta;
        private String detalle;
        private Instant fecha;
        private boolean cumplido;
    }
}
