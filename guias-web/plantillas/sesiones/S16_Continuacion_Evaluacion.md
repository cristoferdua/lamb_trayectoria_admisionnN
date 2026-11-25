# S16 - Continuación de la evaluación de la Unidad III

*Guía única: agregar un microservicio nuevo al proyecto Lamb - Parte 16 de 16 - 2026*

## 1. Preguntas técnicas para preparar la sustentación

| Tema | Pregunta | Respuesta corta |
|---|---|---|
| Servicio base | ¿Qué hace cada carpeta (`controller`, `service`, `repository`, `dto`, `entity`, `mapper`)? | Controlador recibe HTTP; servicio tiene la lógica; repositorio habla con la BD; DTO es lo que entra/sale; entidad es la tabla; mapper copia entre DTO y entidad. |
| Flyway | ¿Cómo se crea la tabla? | Flyway ejecuta `V1__...sql` al arrancar y anota que ya lo aplicó. |
| Config | ¿Qué pasa si el Config Server está caído al arrancar? | El servicio no arranca: no tiene puerto, BD ni Kafka. |
| Eureka | ¿Qué es `lb://`? | "Busca este nombre en Eureka y reparte entre sus instancias". |
| Gateway | ¿Por qué el frontend no llama al 8095? | Todo entra por un solo punto que aplica seguridad y balanceo. |
| Seguridad | ¿Cómo llega el rol al Gateway? | Dentro del token de Keycloak (`realm_access.roles`). |
| Kafka | ¿Qué es un topic y una partición? | El topic es el "canal" (`evaluacionn-eventos`); se divide en 3 particiones para repartir carga. |
| Kafka | ¿Qué pasa si el consumidor está apagado? | Los mensajes quedan en Kafka; al volver, los lee desde donde se quedó (su `group-id`). |
| Consistencia | ¿Qué pasa si falla la BD después de publicar? | No pasa: se publica **después** del commit (`publicarTrasCommit`). |
| Observabilidad | ¿Cómo encuentras el error de una petición? | Tomo su `X-Trace-ID` y lo busco en Loki. |
| Escalabilidad | ¿Cómo atiendes más usuarios? | Levanto otra instancia en otro puerto; Eureka y el Gateway la usan solas. |

## 2. Si te piden un cambio en vivo

| Te piden | Qué haces | Sesión |
|---|---|---|
| "Agrega un atributo" | Migración `V2__...sql` + entidad + Request/Response + `actualizar` + formulario | S1 (6.1), S11 |
| "Solo el docente puede crear" | `.hasAnyRole("ADMIN", "DOCENTE")` en el Gateway | S7 |
| "Agrega otra validación" | Anotación en el Request (`@Size`, `@Min`...) + `Validators` en el formulario | S1 (6.1), S11 |
| "Muestra el evento en la consola" | Ya está: `alRecibirEvento` lo registra | S8 |
| "Levanta otra instancia" | `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8096"` | S3 |

## 3. Cierre

Con esto terminas la guía: tienes un microservicio nuevo integrado al proyecto Lamb, con o sin CRUD,
siguiendo el estilo de código de Pagatu. Para empezar otro servicio, vuelve a la
[S1](S01_Construccion_Servicio_Base.md) y usa la tabla de nombres.

Todos los links (Eureka, Kafka, Grafana, Keycloak...): [Enlaces del sistema](Enlaces_Sistema.md).
