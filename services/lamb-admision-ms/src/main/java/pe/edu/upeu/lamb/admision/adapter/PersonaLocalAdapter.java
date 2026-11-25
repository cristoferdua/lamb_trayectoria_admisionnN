package pe.edu.upeu.lamb.admision.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.lamb.admision.entity.Persona;
import pe.edu.upeu.lamb.admision.port.PersonaPort;
import pe.edu.upeu.lamb.admision.repository.PersonaRepository;

import java.time.Instant;
import java.util.Optional;

/**
 * Adaptador por defecto del puerto Personas. Guarda los datos personales en
 * la BD de Admision (tabla personas_admision). Se reemplaza por un cliente
 * HTTP cuando exista el ms Personas real.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PersonaLocalAdapter implements PersonaPort {

    private final PersonaRepository repo;

    @Override
    @Transactional
    public Long obtenerOCrearPersona(DatosPersona d) {
        return repo.findByDni(d.dni())
                .map(Persona::getId)
                .orElseGet(() -> {
                    Persona nueva = Persona.builder()
                            .dni(d.dni())
                            .nombres(d.nombres())
                            .apellidoPaterno(d.apellidoPaterno())
                            .apellidoMaterno(d.apellidoMaterno())
                            .fechaNacimiento(d.fechaNacimiento())
                            .sexo(d.sexo())
                            .correo(d.correo())
                            .telefono(d.telefono())
                            .fechaRegistro(Instant.now())
                            .build();
                    Persona guardada = repo.save(nueva);
                    log.info("component=personas-local accion=crear dni={} id={}", d.dni(), guardada.getId());
                    return guardada.getId();
                });
    }

    @Override
    public Optional<PersonaInfo> consultarPorDni(String dni) {
        return repo.findByDni(dni).map(this::toInfo);
    }

    @Override
    public Optional<PersonaInfo> consultarPorId(Long idPersona) {
        return repo.findById(idPersona).map(this::toInfo);
    }

    private PersonaInfo toInfo(Persona p) {
        String nombreCompleto = String.join(" ",
                nz(p.getNombres()), nz(p.getApellidoPaterno()), nz(p.getApellidoMaterno())).trim();
        return new PersonaInfo(p.getId(), p.getDni(), p.getNombres(),
                p.getApellidoPaterno(), p.getApellidoMaterno(), nombreCompleto,
                p.getCorreo(), p.getTelefono());
    }

    private String nz(String s) { return s == null ? "" : s; }
}
