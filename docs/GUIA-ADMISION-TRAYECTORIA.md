# Guia para construir los CRUD de admision y trayectoria

Para quien construya los CRUD de `lamb-admision-ms` (32) y `lamb-trayectoria-ms` (27) del documento
*CRUD_Admision_a_Trayectoria_UPeU_v3*. Curriculo (47), planificacion (35) y evaluacion (20) ya estan hechos y probados;
este archivo dice como quedaron para que los dos microservicios que faltan sean coherentes con ellos.

Complementa a `docs/FUSION-IDS-EXTERNOS.md` (ids externos y cambios de contrato) y a
`docs/PLANIFICACION-TRANSACCIONAL.md` (ejemplo de microservicio transaccional).

---

## 1. Primero: lo que hay que corregir en trayectoria (ya rompe hoy)

La seccion de planificacion paso a llamarse **grupo**. El evento `acta.cerrada` (topic `evaluacion-eventos`) ahora trae
`grupoId` en vez de `seccionId`:

```json
{"tipoEvento":"acta.cerrada","actaId":3,"grupoId":3,"estado":"CERRADA","codigoCurso":"IS401","periodo":"2027-I",
 "creditos":4,"resultados":[{"idPersona":3,"notaFinal":14.00,"aprobado":true}],"origen":"lamb-evaluacion-ms","timestamp":1793300000000}
```

- `event/ActaCerradaEvento.java`: renombrar `seccionId` -> `grupoId`.
- `entity/HistorialCurso.java`: `idSeccion` -> `idGrupo` y migracion Flyway nueva
  (`ALTER TABLE ... RENAME COLUMN id_seccion TO id_grupo`; renombrar tambien `uk_historial_persona_seccion`).
  Ejemplo hecho igual en evaluacion: `services/lamb-evaluacion-ms/src/main/resources/db/migration/V4__grupos.sql`.
- `TrayectoriaService` (`existsByIdPersonaAndIdSeccion`, `acta.getSeccionId()`) y el repositorio.
- Hasta corregirlo, el consumidor falla: `id_seccion` es `NOT NULL` y el campo llega `null`.
- `notaFinal` ahora es **ponderada** por los pesos del grupo (evaluacion CRUD 2) y `aprobado` usa la escala vigente
  (evaluacion CRUD 3). Trayectoria no debe recalcular nada: usa lo que trae el evento.

---

## 2. Convenciones que siguen los tres microservicios ya hechos

### Estructura de cada CRUD (una tabla = un CRUD = un controller)

```
entity/X.java            @Entity, Lombok @Getter @Setter @Builder, @ManyToOne LAZY para relaciones internas
dto/XRequest.java        jakarta.validation (@NotNull, @NotBlank, @Size, @Min/@Max); relaciones como idAlgo (Long)
dto/XResponse.java       id + campos + idAlgo y codigoAlgo de cada relacion (nunca devolver la entidad JPA)
mapper/XMapper.java      toEntity(request) sin relaciones; toResponse(entity)
repository/XRepository   findByPadreId(...) para filtrar; existsBy... / existsBy...AndIdNot para unicidad
service/XService.java    listar(filtro opcional), obtener, crear, actualizar, eliminar, buscarOFallar
                         @Transactional en escrituras, @Transactional(readOnly = true) en lecturas
                         validar(request, id): reglas -> BusinessRuleException (409)
controller/XController   @Tag(name = "...", description = "CRUD N · ...") + @RequestMapping("/api/v1/<plural-con-guiones>")
                         GET (?filtro), GET /{id}, POST (201), PUT /{id}, DELETE /{id} (204)
```

- El `@Tag` hace que cada CRUD salga como un grupo propio en Swagger. Hay que poner el numero del documento en la descripcion: `CRUD 5 · Postulante`.
- Las rutas van en plural con guiones: `/api/v1/procesos-admision`, `/api/v1/alertas-riesgo`.
- Los errores usan `ResourceNotFoundException` (404), `BusinessRuleException` (409), `DataIntegrityViolationException` (409) y la validacion (400). Ya existen en `GlobalExceptionHandler`.
- Cada CRUD lleva una tabla en una migracion Flyway **nueva** (`V<n+1>__...sql`). Nunca se edita una migracion ya aplicada. Ademas:
  - las reglas de unicidad van como `CONSTRAINT uk_...` y los rangos de fechas como `CHECK`;
  - Hibernate esta en `ddl-auto: validate`, asi que los tipos de la tabla deben coincidir con la entidad.
- Los catalogos se siembran en la misma migracion con `INSERT`. Ejemplos ya hechos: tipos de periodo, tipos de curso, componentes de evaluacion y escala vigesimal.
- Los comentarios del codigo van en espanol sin tildes, igual que el resto del proyecto.

### Solicitudes (patron comun)

Admision y trayectoria tienen varias: reclamo/apelacion, anulacion de inscripcion, constancia/certificado, reincorporacion.
Todas siguen el mismo patron, que ya esta en planificacion (CRUD 28-35), curriculo (46) y evaluacion (10, 13, 15):

- **Campos comunes:**
  - `idSolicitante` sale del JWT (`JwtClaims.idPersona(jwt)`), no del cuerpo del request.
  - `estado` usa el enum `EstadoSolicitud`: PENDIENTE, APROBADA o RECHAZADA.
  - Ademas: `observacionRespuesta`, `idRespondidoPor`, `fechaSolicitud`, `fechaRespuesta`.
- **Responder:** `POST /api/v1/<recurso>/{id}/responder` con `{ "aprobada": true, "observacion": "..." }`. Quien responde tambien sale del JWT.
- **Solo PENDIENTE:** una solicitud solo se edita, elimina o responde si esta PENDIENTE.
- **Aprobacion con efecto:** si aprobar cambia algo, se hace en `aplicarAprobacion(...)`, dentro de la misma transaccion. Ejemplo: la solicitud de cambio de aula llama a `HorarioGrupoService.reprogramar`.
- **Copiar la plantilla:** `JwtClaims` y `RespuestaSolicitudRequest` se copian de `lamb-planificacion-ms` (`config/` y `dto/`) cambiando el paquete.

### Ids de otros microservicios

- Solo se guarda el id (`Long`). **Nunca** se crea un CRUD de algo que pertenece a otro microservicio.
- Encima del campo va un comentario `// FUSION: <de donde viene y que hacer al fusionar>`.
- Si se valida en linea, se usa Feign + `@CircuitBreaker` en un bean aparte, con un fallback que no devuelva 500. El modelo a copiar es `CurriculoConsultaService` de planificacion.

### Vistas calculadas (sin CRUD)

- **Que son:** endpoints solo GET, sin tabla, que calculan su resultado con datos de otros CRUD.
- **Ejemplos del documento:**
  - Admision: Orden de merito / Ranking de ingreso.
  - Trayectoria: Avance curricular, Historial academico, Promedio ponderado, Rendimiento por curso, Historial de estados.
- **Controller:** van en un `VistaController` con `@Tag(name = "Vistas")` y `@RequestMapping("/api/v1/vistas")`.
- **Nombres:** el nombre de la vista no puede repetir uno de otro microservicio, porque el Gateway enruta cada vista por su ruta exacta. Ya estan usadas:
  - planificacion: `horario-estudiante`;
  - evaluacion: `acta-notas`, `historial-acta`, `nota-final`, `estadistica-grupo`, `cuadro-meritos`, `resultado-evaluacion-docente`.

### Seguridad (Keycloak, ya no `lamb-auth-ms`)

- Cada microservicio valida el JWT por su cuenta. Su `SecurityConfig` ya esta y no hay que tocarlo.
- Las reglas por rol viven en el **Gateway**:
  - archivo: `infra/lamb-gateway/src/main/java/.../SecurityConfig.java`;
  - por cada CRUD nuevo se agrega su ruta a un arreglo, con GET para cualquier autenticado y escritura para los roles que correspondan;
  - roles disponibles: ADMIN, COORDINADOR, DOCENTE, ESTUDIANTE.
  - Ya existe una regla para `POST /api/v1/solicitudes-*/*/responder` que solo deja pasar a ADMIN y COORDINADOR. Una solicitud que no empiece con `solicitudes-` necesita su propia linea.
- Las rutas del Gateway van en `infra/lamb-config/config-repo/lamb-gateway-dev.yml` y en `-prod.yml`. Se puede listar varias rutas en un solo predicado, separadas por coma: `- Path=/api/v1/a/**,/api/v1/b/**`.
- `idPersona` es un claim propio del token, separado del `sub`.

---

## 3. Lo que admision tiene que jalar de nosotros

| CRUD de admision | Jala | Endpoint (via Gateway 18080) |
|---|---|---|
| 15 Asignacion postulante-aula | Aula / ambiente (planificacion CRUD 15) | `GET /api/v1/ambientes`, `GET /api/v1/ambientes/{id}` (incluye `aforo`, `activo`, `codigoPabellon`) |
| 3 Vacantes por programa, sede y modalidad | Modalidad (curriculo CRUD 1) | `GET /api/v1/modalidades` |
| Perfil de ingreso (lo consulta admision) | curriculo CRUD 29 | `GET /api/v1/perfiles-ingreso?idPlan=` |
| Universidad de origen | curriculo CRUD 42 | `GET /api/v1/universidades-origen` |
| 30 Solicitud de convalidacion por traslado externo | Reglas en curriculo CRUD 41 y 43 | `GET /api/v1/tablas-convalidacion?idUniversidad=`, `GET /api/v1/cursos-externos-convalidables?idTabla=` |
| Programa, plan vigente | curriculo CRUD 5 | `GET /api/v1/planes-estudio/programa/{idPrograma}/vigente` |

- La "Modalidad de admision" (CRUD 2 de admision: ordinaria, traslado...) es **otra cosa** que la modalidad de estudio de curriculo (presencial, virtual): admision tiene su propia tabla.
- Sede, facultad y escuela/programa son de `lamb-institucional-ms` (`/api/v1/facultades`, `/api/v1/programas`). Sede todavia no existe: solo se guarda `idSede` con `// FUSION:`.
- El evento `postulante.admitido` que ya publica admision no cambio. Trayectoria lo sigue consumiendo igual.

## 4. Lo que trayectoria tiene que jalar de nosotros

| CRUD de trayectoria | Jala | Endpoint |
|---|---|---|
| 6 Cumplimiento de requisito de egreso | Requisitos (curriculo CRUD 33-38) | `/api/v1/requisitos-egreso`, `/requisitos-idioma`, `/requisitos-practicas`, `/requisitos-investigacion`, `/requisitos-extracurriculares`, `/modalidades-titulacion` (todos con `?idPlan=`) |
| 7 Participacion en formacion integral | Actividad (curriculo CRUD 26) | `GET /api/v1/actividades-formacion` |
| 8 Practicas preprofesionales | Requisito de practicas (curriculo CRUD 35) | `GET /api/v1/requisitos-practicas?idPlan=` |
| 2 Cambio de plan / adecuacion | Plan de adecuacion y equivalencias (curriculo CRUD 39-40) | `/api/v1/planes-adecuacion`, `/api/v1/equivalencias-plan` |
| 5 Convalidacion reconocida | Tablas de convalidacion (curriculo CRUD 41-43) | ver seccion 3 |
| 23 Portafolio / competencias | Competencias (curriculo CRUD 31-32) | `/api/v1/competencias-generales`, `/api/v1/competencias-especificas?idPerfilEgreso=` |
| 11 Curso asignado a refuerzo, 15 Repitencia | Curso (curriculo catalogo CRUD 12) | `GET /api/v1/cursos-catalogo`; codigo y creditos tambien llegan en `acta.cerrada` |
| 26 Situacion al cierre del periodo | Periodo (planificacion CRUD 1) | `GET /api/v1/periodos-academicos` |
| 22 Reconocimiento (tercio superior) | Cuadro de meritos (vista de evaluacion) | `GET /api/v1/vistas/cuadro-meritos?periodo=2027-I` (trae `posicion`, `promedio`, `tercioSuperior`) |
| Nota final | Evento `acta.cerrada` (ya lo consume) | o `GET /api/v1/vistas/nota-final?idGrupo=&idPersona=` |
| 17 Tutor | Docente: no existe MS de docentes | guardar `idDocente` (= `idPersona` de personas) con `// FUSION:` |
| Matricula (lamb-matricula-ms) | otro integrante | solo `idMatricula` con `// FUSION:` |

- Avance curricular: se calcula con los cursos del plan vigente (`GET /api/v1/planes-estudio/programa/{idPrograma}/vigente` trae `cursos[]` con `creditos` y `totalCreditos`) frente al historial.
- Si trayectoria necesita guardar datos del curso o del plan, que los copie desde el evento (como hace planificacion con `plan.publicado`). No debe consultar a curriculo cada vez.

## 5. Eventos Kafka que existen hoy (no cambiar sus nombres)

| Topic | Evento | Lo publica | Campos clave |
|---|---|---|---|
| `curriculo-eventos` | `plan.publicado` | curriculo | `planId`, `idPrograma`, `version`, `codigosCursos[]`, `cursos[{codigo,nombre,creditos}]` |
| `planificacion-eventos` | `grupo.abierto` | planificacion | `grupoId`, `codigoGrupo`, `codigoCurso`, `periodo`, `idDocenteTitular`, `creditos` |
| `evaluacion-eventos` | `acta.abierta` / `acta.rechazada` / `acta.cerrada` | evaluacion | `actaId`, `grupoId`, `resultados[]` (solo en cerrada) |
| `admision-eventos` | `postulante.admitido` | admision | (sin cambios) |

- El evento es una clase plana en `event/`, propia de cada microservicio. No hay libreria compartida.
- Se publica tras el commit (`publicarTrasCommit`) y la key es el id de la entidad.
- El consumidor tiene su propio `group-id` y es idempotente en dos capas: una busqueda explicita mas un `UNIQUE` que atrapa `DataIntegrityViolationException`.

## 6. Transaccional: lo que pide la guia a cada integrante

La guia pide que cada integrante tenga al menos un microservicio **transaccional**, es decir, cabecera-detalle mas una
regla de negocio real (ver `docs/PLANIFICACION-TRANSACCIONAL.md`). Candidatos naturales en sus microservicios:

- **Admision:** `Inscripcion` (cabecera) con sus `DocumentoPresentado` (detalle). La regla es que no se aprueba sin los documentos que exige la modalidad (CRUD 10). Otro candidato es `AdjudicacionVacantes`, con la regla de que no se pasa el cupo de la vacante (CRUD 3, con bloqueo como el aforo del ambiente).
- **Trayectoria:** `PlanRefuerzo` (cabecera) con sus `CursoRefuerzo` (detalle, CRUD 10 y 11). Otro candidato es `Egreso` (CRUD 24), que verifica todos los requisitos cumplidos antes de registrarse.

## 7. Como probar

- **Swagger:** cada microservicio expone `/swagger-ui.html`. El token se pega en el boton Authorize.
- **Arranque:** `scripts\demo-start.ps1` ya arranca con Keycloak. Antes hay que detener los contenedores de otros proyectos que usan los puertos 18180 y 15436.
- **Migraciones:** al probar contra la base dev, la migracion se aplica de verdad. Para probar sin riesgo, se clona la base: `pg_dump` a un contenedor temporal en otro puerto, y se lanza el jar con `--spring.datasource.url=...` apuntando a ese contenedor.
