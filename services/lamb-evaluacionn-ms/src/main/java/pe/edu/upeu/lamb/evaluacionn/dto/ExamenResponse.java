package pe.edu.upeu.lamb.evaluacionn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamenResponse {
    private Long id;
    private String titulo;
    private String curso;
}
