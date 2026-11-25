# Fusion: ids externos y cambios de contrato (curriculo, planificacion, evaluacion)

Este archivo es para quien fusione el codigo de los demas integrantes con los tres microservicios de
`curriculo`, `planificacion` y `evaluacion` (CRUD del documento *CRUD_Admision_a_Trayectoria_UPeU_v3*).
Esta escrito para que una persona o un asistente (Claude) sepa exactamente que conectar o corregir.

En el codigo, cada id que viene de un microservicio ajeno lleva un comentario `// FUSION: ...` encima del campo.
Para listarlos todos:

```bash
grep -rn "FUSION:" services/lamb-curriculo-ms services/lamb-planificacion-ms services/lamb-evaluacion-ms
```

Regla que se siguio: **no se creo ningun CRUD de otro microservicio** (ni docente, ni sede, ni persona).
Solo se guarda el id (`Long`) y hoy no se valida contra nadie.

---

## 1. Cambios que ROMPEN codigo de otros integrantes (corregir al fusionar)

### 1.1 La seccion ahora es "grupo" (planificacion)

| Antes | Ahora |
|---|---|
| `GET /api/v1/secciones/{id}` | `GET /api/v1/grupos/{id}` |
| estado `ABIERTA` / `ANULADA` | estado `ABIERTO` / `ANULADO` (tambien `PENDIENTE_VALIDACION`, `PENDIENTE_ACTA`) |
| evento `seccion.abierta` (campo `seccionId`, `docente` texto) | evento `grupo.abierto` (campo `grupoId`, `codigoGrupo`, `idDocenteTitular` Long) |
| eventos `acta.abierta / acta.rechazada / acta.cerrada` con `seccionId` | mismos eventos con `grupoId` |
| tabla `secciones` | tabla `grupos` (migracion `V3__grupos_y_cruds_planificacion.sql`) |
| ruta Gateway `lamb-planificacion-secciones` | ruta `lamb-planificacion` (lista de 35 recursos) |

**lamb-matricula-ms** (Yesmi / Nelson):
- `client/PlanificacionClient.java`: cambiar `@GetMapping("/api/v1/secciones/{id}")` por `@GetMapping("/api/v1/grupos/{id}")`.
- `service/MatriculaService.java` linea ~61: comparar contra `"ABIERTO"` en vez de `"ABIERTA"`.
- `dto/SeccionDisponibleDto.java`: los campos `id`, `estado`, `cupoMaximo` siguen existiendo en `GrupoResponse` (se puede renombrar a `GrupoDisponibleDto`).
- `entity/DetalleMatricula.idSeccion`, `DetalleMatriculaRequest/Response`: renombrar a `idGrupo` es opcional, pero recomendado para coherencia.

**lamb-trayectoria-ms** (Cristofer) - **importante**:
- `event/ActaCerradaEvento.java`: el campo llega como `grupoId`. Hoy trayectoria lee `seccionId`, que llegara `null`,
  y `HistorialCurso.id_seccion` es `NOT NULL`: el consumidor fallara al guardar. Renombrar `seccionId` -> `grupoId`
  (y la columna `id_seccion` -> `id_grupo` con una migracion Flyway, como se hizo en evaluacion `V4__grupos.sql`).
- `TrayectoriaService` (lineas ~87-94) usa `acta.getSeccionId()` y `existsByIdPersonaAndIdSeccion`.
- El evento `acta.cerrada` ahora trae la **nota final ponderada** por los pesos del grupo (CRUD 2 de evaluacion) y
  `aprobado` segun la escala vigente (CRUD 3), ya no un promedio simple con 10.5 fijo.

**scripts/demo-pruebas.ps1**: todavia usa `/api/v1/secciones` y el login de `lamb-auth-ms` (ya retirado por Keycloak).

### 1.2 El curso se define en curriculo (catalogo, CRUD 12)

- curriculo: `PlanCurso` ya no guarda codigo/nombre/creditos; referencia a `CursoCatalogo` (`/api/v1/cursos-catalogo`).
  `POST /api/v1/planes-estudio/{id}/cursos` ahora recibe `{ idCursoCatalogo, ciclo, idTipoCurso, idAreaCurricular? }`.
- El tipo de curso dejo de ser enum: es el CRUD 13 `/api/v1/tipos-curso` (OBLIGATORIO, ELECTIVO, REQUISITO sembrados).
- El evento `plan.publicado` agrega `cursos: [{codigo, nombre, creditos}]`.
- planificacion: `/api/v1/cursos` es **solo lectura**; se llena con `plan.publicado` (copia local para la oferta).
  Ya no se crean cursos en planificacion.

### 1.3 Nota (evaluacion)

- `tipoEvaluacion` (PRACTICA/PARCIAL/FINAL) paso a `idComponente` (CRUD 1, `/api/v1/componentes-evaluacion`).
  Los tres valores antiguos se sembraron como componentes y las notas existentes se migraron (`V5__cruds_evaluacion.sql`).

---

## 2. Ids que se jalan de microservicios de otros integrantes

| Id en nuestras tablas | Debe venir de | Estado hoy | Que hacer al fusionar |
|---|---|---|---|
| `idPrograma` (curriculo: denominaciones, grados, titulos, lineas de investigacion, proyeccion social, comisiones; planificacion: ofertas, talleres, cupos de taller, limites de creditos, parametros de matricula, equivalencias) | `lamb-institucional-ms` `GET /api/v1/programas/{id}` | **existe** | Agregar un Feign + `@CircuitBreaker` como `CurriculoClient` de planificacion y validar el id antes de guardar |
| `idFacultad` (curriculo: departamentos academicos) | `lamb-institucional-ms` `GET /api/v1/facultades/{id}` | **existe** | Igual que arriba |
| `idSede` (planificacion: calendarios academicos, pabellones) | MS de organizacion (Sede/Filial) | **no existe** en el repo | Cuando exista, validar por Feign. Hoy es un numero libre |
| `idDocente` (planificacion: asignaciones docente, cargas lectivas/no lectivas, disponibilidad, preferencias, solicitud de cambio de docente; evaluacion: actas, plazos de apertura, respuestas de encuesta, firmas de acta) | MS de docentes / RR. HH. | **no existe** | Hoy se usa el `idPersona` de `lamb-personas-ms` (claim `idPersona` de Keycloak). Si se crea el MS de docentes, cambiar el Feign; la columna sigue siendo `BIGINT` |
| `idPersona` estudiante (evaluacion: notas, asistencia, calificacion por criterio, inhabilitacion, actas complementarias y de rectificacion) | `lamb-personas-ms` + matricula del grupo en `lamb-matricula-ms` | personas **existe**; matricula **existe** | Validar que el estudiante este matriculado en el grupo (`lamb-matricula-ms`) antes de registrar notas/asistencia |
| `idPersona` miembro de comision (curriculo) | `lamb-personas-ms` `GET /api/v1/personas/{id}` | **existe** | Validar por Feign |
| Grupos del estudiante para la vista `GET /api/v1/vistas/horario-estudiante` | `lamb-matricula-ms` | **existe** | Hoy recibe `?idsGrupo=1,2`. Al fusionar: obtener los grupos de la matricula del `idPersona` del JWT |

## 3. Ids que se jalan entre nuestros propios microservicios (ya resueltos o por conectar)

| Id | Origen | Usado en |
|---|---|---|
| curso | curriculo `cursos-catalogo` -> evento `plan.publicado` | planificacion `cursos` (copia), ofertas, talleres, nivelacion, preferencias, equivalencias |
| `idSilaboBase` | curriculo CRUD 15 `/api/v1/silabos-base` | planificacion `silabos_grupo` (hoy sin Feign) |
| `idUnidadAprendizaje` | curriculo CRUD 16 `/api/v1/unidades-aprendizaje` | planificacion `sesiones_clase` (hoy sin Feign) |
| `idGrupo` | planificacion CRUD 7 `/api/v1/grupos` | evaluacion: pesos, evaluaciones programadas, asistencia, solicitudes y plazos de apertura, encuestas |
| `idSesionClase` | planificacion CRUD 26 `/api/v1/sesiones-clase` | evaluacion `registros_asistencia` |
| `idPeriodo` | planificacion CRUD 1 `/api/v1/periodos-academicos` | curriculo `evaluaciones_curriculares`, evaluacion `encuestas_docente` |

## 4. Lo que otros integrantes pueden jalar de nosotros

- **Admision** (Cristofer): perfil de ingreso `GET /api/v1/perfiles-ingreso?idPlan=`, universidad de origen
  `/api/v1/universidades-origen`, tablas de convalidacion `/api/v1/tablas-convalidacion` y
  `/api/v1/cursos-externos-convalidables` (curriculo); aula / local de examen `/api/v1/ambientes` (planificacion).
- **Trayectoria** (Cristofer): requisitos de egreso `/api/v1/requisitos-egreso`, idioma, practicas, investigacion,
  extracurriculares, modalidades de titulacion, competencias, actividades de formacion integral (curriculo);
  periodo academico (planificacion); notas finales y cuadro de meritos `GET /api/v1/vistas/cuadro-meritos?periodo=` (evaluacion).
- **Matricula**: limite de creditos `/api/v1/limites-creditos`, parametros de matricula `/api/v1/parametros-matricula`,
  grupos habilitados por equivalencia `/api/v1/grupos-habilitados-equivalencia` y solicitudes aprobadas
  `/api/v1/solicitudes-grupo-equivalente` (planificacion); requisitos de matricula `/api/v1/requisitos-matricula` (curriculo).

## 5. Limitaciones conocidas

- El acta de rectificacion (evaluacion CRUD 13) queda registrada y aprobada, pero no publica un evento:
  trayectoria no se entera del cambio de nota. Si se necesita, agregar `acta.rectificada` en `evaluacion-eventos`.
- Los ids externos no se validan (no hay Feign hacia institucional, personas ni matricula desde estos tres MS).
