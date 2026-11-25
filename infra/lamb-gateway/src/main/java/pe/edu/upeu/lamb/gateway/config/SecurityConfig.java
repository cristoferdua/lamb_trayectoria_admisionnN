package pe.edu.upeu.lamb.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
public class SecurityConfig {

    // Rutas de gestion academica (coordinacion/admision/curriculo/evaluacion/trayectoria)
    private static final String[] ACADEMICO = {
            "/api/v1/planes-estudio/**", "/api/v1/cursos/**", "/api/v1/postulaciones/**"
    };

    // Planificacion: lectura para cualquier autenticado, escritura de coordinacion
    private static final String[] PLANIFICACION = {
            "/api/v1/grupos/**", "/api/v1/horarios-grupo/**", "/api/v1/asignaciones-docente/**",
            "/api/v1/tipos-periodo/**", "/api/v1/periodos-academicos/**", "/api/v1/calendarios-academicos/**",
            "/api/v1/hitos-calendario/**", "/api/v1/dias-no-lectivos/**", "/api/v1/ofertas-academicas/**",
            "/api/v1/grupos-taller/**", "/api/v1/cupos-grupo/**", "/api/v1/cupos-taller/**", "/api/v1/cursos-nivelacion/**",
            "/api/v1/limites-creditos/**", "/api/v1/parametros-matricula/**", "/api/v1/pabellones/**",
            "/api/v1/tipos-ambiente/**", "/api/v1/ambientes/**", "/api/v1/equipamientos-ambiente/**",
            "/api/v1/aulas-virtuales/**", "/api/v1/cargas-lectivas/**", "/api/v1/cargas-no-lectivas/**",
            "/api/v1/equivalencias-curso/**", "/api/v1/grupos-habilitados-equivalencia/**", "/api/v1/vistas/horario-estudiante"
    };

    // Planificacion que el propio docente mantiene: su silabo, sus sesiones, su disponibilidad y preferencias
    private static final String[] PLANIFICACION_DOCENTE = {
            "/api/v1/silabos-grupo/**", "/api/v1/sesiones-clase/**", "/api/v1/cronogramas-sesion/**",
            "/api/v1/disponibilidades-docente/**", "/api/v1/preferencias-curso-docente/**"
    };

    // Solicitudes de gestion de planificacion (CRUD 28 a 32): las presenta coordinacion o el docente
    private static final String[] SOLICITUDES_PLANIFICACION = {
            "/api/v1/solicitudes-apertura-grupo/**", "/api/v1/solicitudes-cierre-fusion/**",
            "/api/v1/solicitudes-cambio-docente/**", "/api/v1/solicitudes-cambio-horario/**", "/api/v1/solicitudes-cambio-aula/**"
    };

    // Curriculo (46 CRUD ademas del plan de estudios): lectura para autenticados, escritura de coordinacion
    private static final String[] CURRICULO = {
            "/api/v1/modalidades/**", "/api/v1/denominaciones-programa/**", "/api/v1/grados-academicos/**",
            "/api/v1/titulos-profesionales/**", "/api/v1/ciclos-plan/**", "/api/v1/creditos-ciclo/**",
            "/api/v1/vigencias-cohorte/**", "/api/v1/resoluciones-plan/**", "/api/v1/mallas-curriculares/**",
            "/api/v1/departamentos-academicos/**", "/api/v1/cursos-catalogo/**", "/api/v1/tipos-curso/**",
            "/api/v1/sumillas-curso/**", "/api/v1/silabos-base/**", "/api/v1/unidades-aprendizaje/**",
            "/api/v1/resultados-aprendizaje/**", "/api/v1/distribuciones-horas/**", "/api/v1/requisitos-matricula/**",
            "/api/v1/areas-curriculares/**", "/api/v1/grupos-electivos/**", "/api/v1/ejes-curriculares/**",
            "/api/v1/menciones/**", "/api/v1/lineas-investigacion/**", "/api/v1/cursos-investigacion/**",
            "/api/v1/actividades-formacion/**", "/api/v1/voluntariados/**", "/api/v1/proyecciones-sociales/**",
            "/api/v1/perfiles-ingreso/**", "/api/v1/perfiles-egreso/**", "/api/v1/competencias-generales/**",
            "/api/v1/competencias-especificas/**", "/api/v1/requisitos-egreso/**", "/api/v1/requisitos-idioma/**",
            "/api/v1/requisitos-practicas/**", "/api/v1/requisitos-investigacion/**", "/api/v1/requisitos-extracurriculares/**",
            "/api/v1/modalidades-titulacion/**", "/api/v1/equivalencias-plan/**", "/api/v1/planes-adecuacion/**",
            "/api/v1/tablas-convalidacion/**", "/api/v1/universidades-origen/**", "/api/v1/cursos-externos-convalidables/**",
            "/api/v1/comisiones-curriculares/**", "/api/v1/miembros-comision/**", "/api/v1/evaluaciones-curriculares/**"
    };

    // Evaluacion que el docente registra en su grupo
    private static final String[] EVALUACION_DOCENTE = {
            "/api/v1/configuraciones-evaluacion/**", "/api/v1/rubricas/**", "/api/v1/criterios-rubrica/**",
            "/api/v1/evaluaciones-programadas/**", "/api/v1/calificaciones-criterio/**", "/api/v1/registros-asistencia/**",
            "/api/v1/solicitudes-apertura-notas/**", "/api/v1/firmas-acta/**", "/api/v1/solicitudes-modificacion-curricular/**"
    };

    // Evaluacion que define o resuelve coordinacion / registros academicos
    private static final String[] EVALUACION_COORDINACION = {
            "/api/v1/componentes-evaluacion/**", "/api/v1/escalas-calificacion/**", "/api/v1/inhabilitaciones-inasistencia/**",
            "/api/v1/actas-rectificacion/**", "/api/v1/actas-complementarias/**", "/api/v1/respuestas-solicitud-apertura/**",
            "/api/v1/plazos-apertura/**", "/api/v1/encuestas-docente/**", "/api/v1/preguntas-encuesta/**"
    };

    // Lo que presenta el propio estudiante: justificar una falta y responder la evaluacion docente
    private static final String[] EVALUACION_ESTUDIANTE = {
            "/api/v1/justificaciones-inasistencia/**", "/api/v1/respuestas-encuesta/**"
    };

    // Rutas institucionales y de personas: lectura abierta a cualquier autenticado, escritura restringida
    private static final String[] INSTITUCIONAL_PERSONAS = {
            "/api/v1/facultades/**", "/api/v1/programas/**", "/api/v1/personas/**"
    };

    // Trayectoria: 6 CRUD del documento v3 + vistas calculadas
    private static final String[] TRAYECTORIA = {
            "/api/v1/trayectorias/**", "/api/v1/alertas-riesgo/**", "/api/v1/cambios-academicos/**",
            "/api/v1/casos-seguimiento/**", "/api/v1/controles-repitencia/**", "/api/v1/cumplimientos-requisito/**",
            "/api/v1/egresos/**", "/api/v1/informes-trayectoria/**", "/api/v1/niveles-riesgo/**",
            "/api/v1/planes-refuerzo/**", "/api/v1/programas-tutoria/**", "/api/v1/reconocimientos/**",
            "/api/v1/reglas-alerta/**", "/api/v1/sesiones-tutoria/**", "/api/v1/situaciones-cierre/**",
            "/api/v1/solicitudes-constancia/**", "/api/v1/tutorias/**",
            "/api/v1/vistas/avance-curricular", "/api/v1/vistas/historial-academico",
            "/api/v1/vistas/promedio-ponderado", "/api/v1/vistas/rendimiento-por-curso",
            "/api/v1/vistas/historial-estados"
    };

    // realm_access.roles (formato Keycloak) -> ROLE_<rol> para hasRole(...)
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            Object roles = realmAccess == null ? null : realmAccess.get("roles");
            if (!(roles instanceof Collection<?> lista)) {
                return List.<GrantedAuthority>of();
            }
            return lista.stream()
                    .map(rol -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + rol))
                    .toList();
        });
        return converter;
    }

    // Primera linea: reglas por ruta, metodo y rol. Spring aplica la primera regla que coincide.
    // Segunda linea (JwtAuthenticationConverter + oauth2ResourceServer): la valida cada microservicio por su cuenta.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/**", "/error").permitAll()
                        // DEV: lectura y escritura de admision/trayectoria sin token (Fase 1, Keycloak va en fase 2)
                        .requestMatchers("/api/v1/postulaciones/**", "/api/v1/postulantes/**",
                                "/api/v1/inscripciones/**", "/api/v1/convocatorias/**",
                                "/api/v1/modalidades-admision/**", "/api/v1/examenes-admision/**",
                                "/api/v1/calificaciones/**", "/api/v1/adjudicaciones/**",
                                "/api/v1/casos-especiales/**", "/api/v1/seguimientos/**",
                                "/api/v1/tipos-documento/**",
                                "/api/v1/admision/**",
                                "/api/v1/public/**",
                                "/api/v1/derecho-admision/**").permitAll()
                        .requestMatchers(TRAYECTORIA).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/examenes/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/examenes/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/examenes/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/examenes/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, ACADEMICO).authenticated()
                        .requestMatchers(ACADEMICO).hasAnyRole("ADMIN", "COORDINADOR")
                        // Aprobar o rechazar una solicitud de gestion es solo de coordinacion
                        .requestMatchers(HttpMethod.POST, "/api/v1/solicitudes-*/*/responder",
                                "/api/v1/justificaciones-inasistencia/*/responder", "/api/v1/actas-rectificacion/*/responder")
                        .hasAnyRole("ADMIN", "COORDINADOR")
                        .requestMatchers(HttpMethod.GET, CURRICULO).authenticated()
                        .requestMatchers(CURRICULO).hasAnyRole("ADMIN", "COORDINADOR")
                        .requestMatchers(HttpMethod.GET, EVALUACION_DOCENTE).authenticated()
                        .requestMatchers(EVALUACION_DOCENTE).hasAnyRole("ADMIN", "COORDINADOR", "DOCENTE")
                        .requestMatchers(HttpMethod.GET, EVALUACION_COORDINACION).authenticated()
                        .requestMatchers(EVALUACION_COORDINACION).hasAnyRole("ADMIN", "COORDINADOR")
                        .requestMatchers(HttpMethod.GET, EVALUACION_ESTUDIANTE).authenticated()
                        .requestMatchers(EVALUACION_ESTUDIANTE).hasAnyRole("ADMIN", "COORDINADOR", "ESTUDIANTE")
                        .requestMatchers(HttpMethod.GET, PLANIFICACION).authenticated()
                        .requestMatchers(PLANIFICACION).hasAnyRole("ADMIN", "COORDINADOR")
                        .requestMatchers(HttpMethod.GET, PLANIFICACION_DOCENTE).authenticated()
                        .requestMatchers(PLANIFICACION_DOCENTE).hasAnyRole("ADMIN", "COORDINADOR", "DOCENTE")
                        .requestMatchers(HttpMethod.GET, SOLICITUDES_PLANIFICACION).authenticated()
                        .requestMatchers(SOLICITUDES_PLANIFICACION).hasAnyRole("ADMIN", "COORDINADOR", "DOCENTE")
                        // CRUD 35: el estudiante pide cursar en un grupo habilitado por equivalencia
                        .requestMatchers("/api/v1/solicitudes-grupo-equivalente/**").hasAnyRole("ADMIN", "COORDINADOR", "ESTUDIANTE")
                        .requestMatchers("/api/v1/actas/**").hasAnyRole("ADMIN", "COORDINADOR", "DOCENTE")
                        .requestMatchers(HttpMethod.GET, "/api/v1/trayectorias/me").authenticated()
                        .requestMatchers(HttpMethod.GET, TRAYECTORIA).authenticated()
                        .requestMatchers(TRAYECTORIA).hasAnyRole("ADMIN", "COORDINADOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/notas/**").authenticated()
                        .requestMatchers("/api/v1/notas/**").hasAnyRole("ADMIN", "DOCENTE")
                        .requestMatchers(HttpMethod.GET, INSTITUCIONAL_PERSONAS).authenticated()
                        .requestMatchers(INSTITUCIONAL_PERSONAS).hasAnyRole("ADMIN", "COORDINADOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/matriculas/**").authenticated()
                        .requestMatchers("/api/v1/matriculas/**").hasAnyRole("ADMIN", "COORDINADOR", "ESTUDIANTE")
                        .requestMatchers(HttpMethod.GET, "/api/v1/pagos/**").authenticated()
                        .requestMatchers("/api/v1/pagos/**").hasAnyRole("ADMIN", "COORDINADOR")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of("http://localhost:4200", "http://localhost:4300"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
