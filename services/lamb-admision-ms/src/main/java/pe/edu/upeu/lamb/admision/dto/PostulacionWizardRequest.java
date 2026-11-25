package pe.edu.upeu.lamb.admision.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Payload del wizard "Nueva postulación completa". El usuario solo escribe DNI + datos de contacto
 * y colegio; el resto (idPersona, código, estado, fecha) lo resuelve el backend.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostulacionWizardRequest {

    // Paso 1 — identificación
    @NotBlank @Pattern(regexp = "\\d{8}", message = "DNI debe tener 8 dígitos")
    private String dni;
    @Size(max = 100) private String nombres;          // viene de RENIEC
    @Size(max = 100) private String apellidoPaterno;  // viene de RENIEC
    @Size(max = 100) private String apellidoMaterno;  // viene de RENIEC

    // Paso 2 — selección académica
    @NotNull private Long idConvocatoria;
    @NotNull private Long idPrograma;
    @NotNull private Long idModalidad;

    // Paso 3 — datos complementarios
    @Size(max = 200) private String colegioProcedencia;
    @Min(1950) @Max(2100) private Integer anioEgresoColegio;
    @DecimalMin("0.0") @DecimalMax("20.0") private BigDecimal promedioColegio;
    @Size(max = 20) private String telefonoContacto;
    @Email @Size(max = 100) private String correoContacto;
    @Size(max = 150) private String nombreApoderado;
    @Size(max = 20) private String telefonoApoderado;
}
