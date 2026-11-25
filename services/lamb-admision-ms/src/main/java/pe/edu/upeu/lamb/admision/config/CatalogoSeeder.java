package pe.edu.upeu.lamb.admision.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.edu.upeu.lamb.admision.entity.ModalidadAdmision;
import pe.edu.upeu.lamb.admision.entity.ProgramaAcademico;
import pe.edu.upeu.lamb.admision.entity.Sede;
import pe.edu.upeu.lamb.admision.repository.ModalidadAdmisionRepository;
import pe.edu.upeu.lamb.admision.repository.ProgramaAcademicoRepository;
import pe.edu.upeu.lamb.admision.repository.SedeRepository;

/**
 * STUB — catalogo organizacional (sedes + carreras).
 *
 * El dueno real de estos datos es:
 *   - Sedes      → ms Institucional (Dominio 1 de la referencia)
 *   - Carreras   → ms Curriculo    (Dominio 4 de la referencia)
 *
 * Mientras esos modulos no existan, este seeder los carga en Admision al
 * arrancar. Lista oficial basada en upeu.edu.pe por sede (Juliaca, Lima,
 * Tarapoto). TODO: migrar a cliente HTTP y ELIMINAR este seeder cuando
 * existan los ms Institucional y Curriculo.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CatalogoSeeder {

    private final SedeRepository sedeRepo;
    private final ProgramaAcademicoRepository programaRepo;
    private final ModalidadAdmisionRepository modalidadRepo;

    @Bean
    public ApplicationRunner sembrarCatalogo() {
        return args -> {
            sembrarModalidades();
            if (sedeRepo.count() > 0) return;

            Sede lima     = sedeRepo.save(Sede.builder().codigo("LIM").nombre("Sede Central Lima (Nana)").activo(true).build());
            Sede juliaca  = sedeRepo.save(Sede.builder().codigo("JUL").nombre("Campus Juliaca").activo(true).build());
            Sede tarapoto = sedeRepo.save(Sede.builder().codigo("TPP").nombre("Campus Tarapoto").activo(true).build());

            // =========== LIMA (Nana) ===========
            String ing = "Ingenieria y Arquitectura";
            agregar(lima, ing, "ARQ", "Arquitectura y Urbanismo");
            agregar(lima, ing, "IAM", "Ingenieria Ambiental");
            agregar(lima, ing, "ICV", "Ingenieria Civil");
            agregar(lima, ing, "ICB", "Ingenieria de Ciberseguridad");
            agregar(lima, ing, "IIA", "Ingenieria de Industrias Alimentarias");
            agregar(lima, ing, "ISI", "Ingenieria de Sistemas");
            agregar(lima, ing, "ISW", "Ingenieria de Software");
            agregar(lima, ing, "IDA", "Ingenieria en Ciencia de Datos e IA");
            agregar(lima, ing, "IND", "Ingenieria Industrial");

            String emp = "Ciencias Empresariales";
            agregar(lima, emp, "ADM", "Administracion");
            agregar(lima, emp, "CPC", "Contabilidad, Gestion Tributaria y Aduanera");
            agregar(lima, emp, "MKT", "Marketing y Negocios Internacionales");

            String sal = "Ciencias de la Salud";
            agregar(lima, sal, "ENF", "Enfermeria");
            agregar(lima, sal, "MED", "Medicina Humana");
            agregar(lima, sal, "NUT", "Nutricion Humana");
            agregar(lima, sal, "PSI", "Psicologia");
            agregar(lima, sal, "TML", "Tec. Medica - Laboratorio Clinico y Anatomia Patologica");
            agregar(lima, sal, "TMF", "Tec. Medica - Terapia Fisica y Rehabilitacion");

            String hum = "Ciencias Humanas y Educacion";
            agregar(lima, hum, "CAM", "Comunicacion Audiovisual y Medios Interactivos");
            agregar(lima, hum, "COR", "Comunicacion Organizacional");
            agregar(lima, hum, "CPE", "Comunicacion y Periodismo");
            agregar(lima, hum, "CPU", "Comunicacion y Publicidad");
            agregar(lima, hum, "DER", "Derecho");
            agregar(lima, hum, "ECN", "Educacion - Ciencias Naturales y Tecnologia");
            agregar(lima, hum, "EIE", "Educacion - Ingles y Espanol");
            agregar(lima, hum, "EMA", "Educacion - Musica y Artes Visuales");
            agregar(lima, hum, "EIP", "Educacion Inicial y Puericultura");

            String teo = "Teologia";
            agregar(lima, teo, "TEO", "Teologia");

            // =========== JULIACA ===========
            agregar(juliaca, ing, "ARQ", "Arquitectura");
            agregar(juliaca, ing, "IAL", "Ingenieria de Alimentos");
            agregar(juliaca, ing, "IAM", "Ingenieria Ambiental");
            agregar(juliaca, ing, "ICV", "Ingenieria Civil");
            agregar(juliaca, ing, "ISI", "Ingenieria de Sistemas");

            agregar(juliaca, emp, "ADM", "Administracion y Negocios Internacionales");
            agregar(juliaca, emp, "AGE", "Asistencia Gerencial");
            agregar(juliaca, emp, "CPC", "Contabilidad y Gestion Tributaria");

            agregar(juliaca, sal, "ENF", "Enfermeria");
            agregar(juliaca, sal, "NUT", "Nutricion Humana");
            agregar(juliaca, sal, "PSI", "Psicologia");

            agregar(juliaca, hum, "EIP", "Educacion Inicial y Puericultura");
            agregar(juliaca, hum, "EIE", "Educacion Linguistica e Ingles");
            agregar(juliaca, hum, "EPR", "Educacion Primaria");

            // =========== TARAPOTO ===========
            agregar(tarapoto, ing, "ARQ", "Arquitectura");
            agregar(tarapoto, ing, "IAM", "Ingenieria Ambiental");
            agregar(tarapoto, ing, "ISI", "Ingenieria de Sistemas");

            agregar(tarapoto, emp, "ADM", "Administracion");
            agregar(tarapoto, emp, "AGE", "Asistencia Administrativa");
            agregar(tarapoto, emp, "CPC", "Contabilidad y Finanzas");
            agregar(tarapoto, emp, "MKT", "Marketing y Negocios Internacionales");

            agregar(tarapoto, sal, "PSI", "Psicologia");

            log.info("component=catalogo-seeder sedes={} programas={}",
                    sedeRepo.count(), programaRepo.count());
        };
    }

    private void sembrarModalidades() {
        crearModalidadSiFalta("ORDINARIO", "Ordinario", "Postulacion general por examen de admision", 100);
        crearModalidadSiFalta("PRIMEROS_PUESTOS", "Primeros Puestos", "Primero y segundo puesto del colegio", 20);
        crearModalidadSiFalta("DEPORTISTA", "Deportista Calificado", "Deportistas con reconocimiento IPD", 10);
        crearModalidadSiFalta("TRASLADO_EXTERNO", "Traslado Externo", "Estudiantes de otras universidades", 15);
        crearModalidadSiFalta("CONVENIO", "Convenio Institucional", "Convenios con instituciones aliadas", 10);
        log.info("component=catalogo-seeder modalidades={}", modalidadRepo.count());
    }

    private void crearModalidadSiFalta(String codigo, String nombre, String desc, int vacantes) {
        if (modalidadRepo.findByCodigo(codigo).isPresent()) return;
        modalidadRepo.save(ModalidadAdmision.builder()
                .codigo(codigo).nombre(nombre).descripcion(desc)
                .vacantesDisponibles(vacantes).vacantesOcupadas(0)
                .activo(true).build());
    }

    private void agregar(Sede sede, String facultad, String codigo, String nombre) {
        programaRepo.save(ProgramaAcademico.builder()
                .codigo(sede.getCodigo() + "-" + codigo)
                .nombre(nombre)
                .facultad(facultad)
                .sede(sede)
                .activo(true)
                .build());
    }
}
