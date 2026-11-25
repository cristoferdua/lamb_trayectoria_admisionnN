package pe.edu.upeu.lamb.evaluacionn.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluacionnEvento {

    private String tipoEvento;
    private Long id;
    private String mensaje;
    private String origen;
    private Long timestamp;
}
