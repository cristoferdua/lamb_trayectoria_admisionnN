package pe.edu.upeu.lamb.admision.port;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Puerto hacia el modulo Personas (Base Universal). Admision es el primer
 * modulo que encuentra al humano, pero el dueno del dato persona es Personas.
 * Hoy el adaptador local guarda en la BD de admision; manana se reemplaza
 * por un adaptador HTTP sin tocar el dominio.
 */
public interface PersonaPort {

    /** Idempotente: si ya existe persona con ese DNI devuelve su id; si no, la crea. */
    Long obtenerOCrearPersona(DatosPersona datos);

    Optional<PersonaInfo> consultarPorDni(String dni);

    Optional<PersonaInfo> consultarPorId(Long idPersona);

    record DatosPersona(
            String dni,
            String nombres,
            String apellidoPaterno,
            String apellidoMaterno,
            LocalDate fechaNacimiento,
            String sexo,
            String correo,
            String telefono
    ) {}

    record PersonaInfo(
            Long id,
            String dni,
            String nombres,
            String apellidoPaterno,
            String apellidoMaterno,
            String nombreCompleto,
            String correo,
            String telefono
    ) {}
}
