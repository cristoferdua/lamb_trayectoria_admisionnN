# S15 - Evaluación de la Unidad III

*Guía única: agregar un microservicio nuevo al proyecto Lamb - Parte 15 de 16 - 2026*

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

El producto final: tu servicio integrado a Lamb, funcionando de punta a punta, y que **sepas explicarlo**.
Lo que entregas va en el [Producto U3 / Producto Final](../proyecto-sello/u3-producto.md).

## 2. Guion de demostración (10 minutos)

| Min. | Qué muestras | Dónde |
|---|---|---|
| 1 | Arquitectura: diagrama de la S1 (sección 1.4) | esta guía |
| 2 | Configuración centralizada y registro | http://localhost:18888/lamb-evaluacionn-ms/dev, http://localhost:18761 |
| 3 | Hola mundo por el Gateway | http://localhost:18080/api/v1/evaluacionn/saludo |
| 4 | Login con `admin` y uso del frontend (camino B: crear, editar, eliminar) | http://localhost:4200/evaluacionn |
| 5 | Eventos en Kafka | http://localhost:18090 |
| 6 | `estudiante1` sin permiso (`403`) | frontend |
| 7 | Logs con traceId y métricas | http://localhost:13000 |
| 8 | Código: entidad → DTO → mapper → servicio → controlador (B) o controlador + productor (A) | IDE |
| 9–10 | Preguntas | |

## 3. Lista final de verificación

- [ ] Compila sin errores (`BUILD SUCCESS`).
- [ ] Configuración solo en el Config Server.
- [ ] Aparece en Eureka.
- [ ] Responde por el Gateway.
- [ ] `GET` libre; modificar solo con `ADMIN`.
- [ ] Publica y consume eventos en Kafka.
- [ ] **Camino B:** CRUD completo con validaciones (`400`) y `404`.
- [ ] Logs en Loki con traceId; métricas en Prometheus.
- [ ] Frontend sin diseño funcionando con login de Keycloak.
- [ ] Todas las pruebas de la S13 en OK.

Siguiente: [S16 - Continuación de la evaluación](S16_Continuacion_Evaluacion.md).
