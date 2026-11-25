# S12 - Evaluación de la Unidad II

*Guía única: agregar un microservicio nuevo al proyecto Lamb - Parte 12 de 16 - 2026*

!!! note "Antes de empezar: debe estar levantado"
    Desde la raíz del proyecto (`Lamb-Modulo02-Admision-Trayectoria`). Si ya está corriendo, no lo arranques otra vez.

    **Docker** (Docker Desktop abierto; se pueden pegar en la misma terminal):

    ```powershell
    docker compose -f kafka/compose-dev.yml up -d    # Kafka + Kafka UI
    docker compose -f keycloak/compose-dev.yml up -d    # Keycloak (tarda ~1 min)
    docker compose -f obs/compose-dev.yml up -d    # Prometheus + Loki + Promtail + Grafana
    docker compose -f services/lamb-evaluacionn-ms/compose-dev.yml up -d    # BD de tu servicio
    ```

    **Servicios** (cada uno en **su propia terminal**, en este orden, esperando que diga `Started ...`):

    ```powershell
    cd infra\lamb-config; .\mvnw.cmd spring-boot:run    # Config Server
    cd infra\lamb-eureka; .\mvnw.cmd spring-boot:run    # Eureka
    cd services\lamb-evaluacionn-ms; .\mvnw.cmd spring-boot:run    # Tu servicio
    cd infra\lamb-gateway; .\mvnw.cmd spring-boot:run    # Gateway (al final)
    cd clients\lamb-frontend; npm start    # Frontend
    ```

    Comprueba que responden:

    - Kafka + Kafka UI: http://localhost:18090
    - Keycloak: http://localhost:18180/realms/lamb
    - Prometheus + Loki + Promtail + Grafana: http://localhost:13000
    - BD de tu servicio: `docker ps` → `lamb-postgres-evaluacionn-dev` *Up*
    - Config Server: http://localhost:18888/lamb-evaluacionn-ms/dev
    - Eureka: http://localhost:18761
    - Tu servicio: http://localhost:8095/actuator/health
    - Gateway: http://localhost:18080/actuator/health
    - Frontend: http://localhost:4200

## 1. Qué debes demostrar

Al terminar la Unidad II tu servicio es un **sistema distribuido robusto**: seguro con Keycloak, avisa por Kafka
solo lo que se guardó, se puede diagnosticar en Grafana y tiene su frontend.

## 2. Lista de evidencias

Toma una captura de cada una (sirven para el [Producto U2](../proyecto-sello/u2-producto.md)):

| N. | Evidencia | Cómo obtenerla | Sesión |
|---|---|---|---|
| 1 | `GET` libre, `POST` con `401` sin token | PowerShell (S7, paso 4) | S7 |
| 2 | Roles en Keycloak | http://localhost:18180 → realm `lamb` → *Realm roles* | S7b |
| 3 | Eventos publicados y consumidos | log: `status=published` / `status=consumed` | S8 |
| 4 | Topic en Kafka UI | http://localhost:18090 → `evaluacionn-eventos` | S8 |
| 5 | **Camino B:** dato inválido → `400` sin evento | PowerShell (S9, paso 4) | S9 |
| 6 | Logs en Grafana | http://localhost:13000 → Explore → Loki | S10 |
| 7 | Seguimiento por traceId | buscar un `X-Trace-ID` en Loki | S10 |
| 8 | Métricas | http://localhost:19090/targets → UP | S10 |
| 9 | Frontend Hola mundo + evento | http://localhost:4200/evaluacionn | S11 |
| 10 | **Camino B:** CRUD en el frontend | `/evaluacionn/examenes`: nuevo, editar, eliminar | S11 |
| 11 | `403` para `estudiante1` | "No tienes permiso para esta acción." | S11 |

## 3. Preguntas que te pueden hacer

| Pregunta | Respuesta corta |
|---|---|
| ¿Dónde se revisan los roles, en el Gateway o en el servicio? | En el Gateway (`hasRole`). El servicio solo valida que el token sea auténtico. |
| ¿Qué diferencia hay entre `401` y `403`? | `401`: no mandaste token. `403`: tu token es válido pero no tienes el rol. |
| ¿Por qué Kafka y no una llamada directa? | El que publica no espera ni depende de quién escucha; si el consumidor se cae, lee los mensajes después. |
| ¿Qué hace `publicarTrasCommit`? | Publica el evento solo después de que la base de datos confirmó el guardado. |
| ¿Para qué sirve el traceId? | Para filtrar en Loki todas las líneas de una sola petición. |
| ¿Cómo encuentra Prometheus a tu servicio? | Le pregunta a Eureka qué servicios hay. |

Siguiente: [S13 - Validación end-to-end](S13_Validacion_End_to_End_Producto.md).
