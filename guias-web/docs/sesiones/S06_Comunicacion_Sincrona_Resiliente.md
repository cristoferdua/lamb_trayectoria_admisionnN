# S6 - Comunicación síncrona resiliente entre servicios

*Guía única: agregar un microservicio nuevo al proyecto Lamb - Parte 6 de 16 - 2026*

## 1. ¿Lo necesitas?

**Para los caminos A y B de esta guía: no.** Tu servicio no necesita llamar a otro microservicio para funcionar,
así que puedes pasar directo a la [S7](S07_Seguridad_Distribuida_Control_Acceso.md).

Haz esta sesión **solo si te piden** que tu servicio **consulte a otro servicio antes de guardar**
(ej. "antes de registrar un préstamo, verifica que el estudiante exista en Admisión").

## 2. Cómo se hace (si te lo piden)

Es el mismo patrón de la S6 de Pagatu (`orden-ms` consulta a `catalogo-ms`):
**OpenFeign** para llamar al otro servicio por su nombre en Eureka + **Circuit Breaker** (Resilience4j)
para responder algo controlado si el otro servicio está caído.

| En Pagatu (S6 original) | En tu servicio |
|---|---|
| `pagatu-orden-ms` (el que llama) | `lamb-evaluacionn-ms` |
| `pagatu-catalogo-ms` (el llamado) | el servicio que te pidan consultar (ej. `lamb-admision-ms`) |
| `ProductoClient` (`@FeignClient(name = "pagatu-catalogo-ms")`) | `TuClient` con `@FeignClient(name = "lamb-admision-ms")` |
| `ProductoDto` | un DTO con solo los campos que necesitas del otro servicio |
| `ProductoConsultaService` (con `@CircuitBreaker`) | `TuConsultaService` |

Sigue los pasos de la sesión original del profe:
[S6 - Comunicación síncrona resiliente entre servicios (Pagatu)](https://262dist.github.io/pagatu/sesiones/S06_Comunicacion_Sincrona_Resiliente/),
reemplazando los nombres con la tabla de arriba.

!!! note "Esta parte no está en el ejemplo probado"
    Los caminos A y B de esta guía se probaron completos sin esta sesión. Si la agregas, prueba también
    qué responde tu servicio cuando el otro está apagado (debe responder un error controlado, no colgarse).

Siguiente: [S7 - Seguridad distribuida](S07_Seguridad_Distribuida_Control_Acceso.md).
