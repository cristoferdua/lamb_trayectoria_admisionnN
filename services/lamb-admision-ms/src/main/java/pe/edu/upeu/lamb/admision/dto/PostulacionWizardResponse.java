package pe.edu.upeu.lamb.admision.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostulacionWizardResponse {
    private Long idPostulante;
    private Long idPersona;
    private String codigoPostulante;
    private String nombreCompleto;
    private String estado;
    private String periodo;
    private String mensajeConfirmacion;
    private boolean correoProgramado;   // true si se encoló envío after-commit
    private String correoDestino;        // email al que se enviará
}
