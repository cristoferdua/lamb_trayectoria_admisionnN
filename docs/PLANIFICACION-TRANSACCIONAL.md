# lamb-planificacion-ms: el microservicio transaccional

La guia (`pagatu/docs/proyecto-sello/brief.md`, seccion 3) pide que cada integrante tenga al menos un
microservicio **transaccional**: "una operacion con **cabecera-detalle real** y al menos **una regla de negocio
verdadera**, equivalente a `orden-ms` o `pago-ms`". En este modulo ese microservicio es planificacion.

## Cabecera-detalle

| pagatu (`orden-ms`) | Lamb (`lamb-planificacion-ms`) |
|---|---|
| `Orden` (cabecera) | `Grupo` (CRUD 7): oferta (curso + periodo + programa), codigo, cupo, estado, `@Version` |
| `DetalleOrden` = Orden x Producto | `HorarioGrupo` (CRUD 17) = **Grupo x Ambiente**, con dia y rango de horas |
| | `AsignacionDocente` (CRUD 20) = **Grupo x Docente**, con rol y horas semanales |

Las dos relaciones son **muchos a muchos con atributos**: un ambiente recibe muchos grupos (en horas distintas)
y un grupo usa varios ambientes; un docente dicta en muchos grupos y un grupo tiene varios docentes.

## La operacion: `POST /api/v1/grupos`

Un solo request trae la cabecera y los detalles. `GrupoService.crear` es un unico `@Transactional`:
si cualquier regla falla se lanza `BusinessRuleException`, la transaccion hace **rollback** y no queda nada
(ni el grupo ni ningun horario ni docente).

```json
{
  "idOferta": 2, "codigo": "A", "cupoMaximo": 35,
  "horarios": [
    {"idAmbiente": 1, "diaSemana": "LUNES", "horaInicio": "08:00", "horaFin": "10:00"},
    {"idAmbiente": 1, "diaSemana": "MIERCOLES", "horaInicio": "08:00", "horaFin": "10:00"}
  ],
  "docentes": [
    {"idDocente": 2, "rol": "TITULAR", "horasSemanales": 4},
    {"idDocente": 5, "rol": "PRACTICA", "horasSemanales": 2}
  ]
}
```

### Reglas de negocio (todas en `ReglasGrupoService`)

1. La oferta no esta CERRADA y el codigo de grupo no se repite en la oferta.
2. El curso esta en un plan VIGENTE de curriculo (Feign + Circuit Breaker, S6). Si curriculo no responde, el grupo
   queda `PENDIENTE_VALIDACION` en vez de fallar.
3. El ambiente esta activo y su **aforo alcanza** para el cupo del grupo.
4. El ambiente **no esta ocupado** ese dia en un rango que se cruce (mismo periodo).
5. Los horarios del mismo grupo no se pisan entre si.
6. Ningun docente **dicta en otro grupo** a esa hora.
7. Ningun docente **pasa su carga lectiva maxima** del periodo (CRUD 21).
8. A lo mas un docente `TITULAR` y sin docentes repetidos.

Las mismas reglas se reutilizan en `PUT/POST /horarios-grupo`, `/asignaciones-docente` y al aprobar las
solicitudes de cambio de horario (CRUD 31), de aula (CRUD 32) y de docente (CRUD 30).

### Concurrencia: dos coordinadores reservan la misma aula a la misma hora

| Capa | Mecanismo | Donde |
|---|---|---|
| 1 | `SELECT ... FOR UPDATE` sobre la fila del ambiente (`@Lock(PESSIMISTIC_WRITE)`), en orden de id para no provocar deadlocks | `AmbienteRepository.findByIdParaReserva` |
| 1 | Candado transaccional por docente (`pg_advisory_xact_lock`), porque no hay fila de docente que bloquear | `AsignacionDocenteRepository.bloquearDocente` |
| 2 | Constraint `EXCLUDE USING gist` en PostgreSQL: dos horarios activos del mismo ambiente, periodo y dia no pueden solaparse | `V3__grupos_y_cruds_planificacion.sql` (`ex_horarios_grupo_cruce_ambiente`) |
| 3 | Bloqueo optimista `@Version` en `Grupo`: dos ediciones simultaneas de la cabecera, la segunda recibe 409 | `Grupo.version` |

`ReglasGrupoService` es `@Transactional(propagation = MANDATORY)`: solo se puede llamar dentro de una transaccion
abierta, porque los candados duran hasta el commit o rollback de quien llama. La llamada Feign a curriculo se hace
**antes** de tomar los candados, para no retener filas bloqueadas mientras se espera una respuesta remota.

### Despues del commit (S8 / S9)

Solo si la transaccion confirma, se publica `grupo.abierto` (afterCommit). Evaluacion abre el acta
(`acta.abierta` -> el grupo pasa a `ABIERTO`) o la rechaza si no hay docente titular (`acta.rechazada` ->
compensacion: el grupo pasa a `ANULADO` y sus horarios quedan inactivos, liberando el aula).

## Como demostrarlo (resultados obtenidos en la prueba del 2026-10-29)

| Caso | Resultado |
|---|---|
| Apertura con 2 horarios y 2 docentes | 201, todo guardado junto |
| 2do horario cruza un aula ocupada | 409 "El ambiente A-101 ya esta ocupado el LUNES de 08:00 a 10:00..." y `GET /grupos?idOferta=` vacio (rollback) |
| Aula de 20 para cupo 30 | 409 por aforo |
| Docente ya dicta a esa hora | 409 por cruce de docente |
| Docente con 4 h de 6 pide 3 mas | 409 por carga lectiva |
| **Dos POST simultaneos** al mismo aula y hora | uno 201, el otro 409 |
| INSERT directo en la base que se solapa | `ERROR: conflicting key value violates exclusion constraint "ex_horarios_grupo_cruce_ambiente"` |
| PUT con `version` vieja | 409 |
| Grupo sin titular | evaluacion rechaza el acta, el grupo queda ANULADO y otro grupo puede usar ese horario |
