package pe.edu.upeu.lamb.admision.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReniecResponse {
    private String dni;
    private String nombres;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String nombreCompleto;
    private boolean encontrado;
    private String fuente;
}
