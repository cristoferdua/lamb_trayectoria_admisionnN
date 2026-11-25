# S10 - Observabilidad y diagnóstico de sistemas distribuidos

*Guía única: agregar un microservicio nuevo al proyecto Lamb - Parte 10 de 16 - 2026*

!!! note "Antes de empezar: debe estar levantado"
    Desde la raíz del proyecto (`Lamb-Modulo02-Admision-Trayectoria`). Si ya está corriendo, no lo arranques otra vez.

    **Docker** (Docker Desktop abierto; se pueden pegar en la misma terminal):

    ```powershell
    docker compose -f obs/compose-dev.yml up -d    # Prometheus + Loki + Promtail + Grafana
    docker compose -f services/lamb-evaluacionn-ms/compose-dev.yml up -d    # BD de tu servicio
    ```

    **Servicios** (cada uno en **su propia terminal**, en este orden, esperando que diga `Started ...`):

    ```powershell
    cd infra\lamb-config; .\mvnw.cmd spring-boot:run    # Config Server
    cd infra\lamb-eureka; .\mvnw.cmd spring-boot:run    # Eureka
    cd services\lamb-evaluacionn-ms; .\mvnw.cmd spring-boot:run    # Tu servicio
    ```

    Comprueba que responden:

    - Prometheus + Loki + Promtail + Grafana: http://localhost:13000
    - BD de tu servicio: `docker ps` → `lamb-postgres-evaluacionn-dev` *Up*
    - Config Server: http://localhost:18888/lamb-evaluacionn-ms/dev
    - Eureka: http://localhost:18761
    - Tu servicio: http://localhost:8095/actuator/health

    Tu servicio **debe** arrancarse desde su carpeta (así se crea `logs\evaluacionn.log`).

## 1. Introducción

Tu servicio ya produce todo lo necesario para verlo en **Grafana** (igual en los dos caminos):

| Qué | Lo produce | Lo recoge | Configuración |
|---|---|---|---|
| **Métricas** | Actuator + Micrometer (`/actuator/prometheus`) | Prometheus (descubre vía Eureka) | ninguna, ya funciona |
| **Logs** | `logback-spring.xml` → `logs\evaluacionn.log` | Promtail → Loki | **1 job** (paso 2) |
| **traceId** | `CorrelationIdFilter` (header `X-Trace-ID`) | sale en cada línea de log | ninguna |

## 2. Logs: agrega tu servicio a Promtail

**Abre el archivo:** `promtail-config-dev.yml`  
**En la ruta:** `obs\promtail\`  
**Qué hacer:** Busca (`Ctrl+F`) la línea `- job_name: lamb-gateway` y pega **justo arriba** de ella. Respeta los 2 espacios antes de `- job_name`. No borres lo demás del archivo.


```yaml
  - job_name: lamb-evaluacionn-ms
    static_configs:
      - targets: [localhost]
        labels:
          application: lamb-evaluacionn-ms
          __path__: /var/log/lamb/lamb-evaluacionn-ms/logs/*.log
```

!!! note "Qué cambia"
    Las 3 apariciones de `lamb-evaluacionn-ms` (tu `spring.application.name`, que también es el nombre de la carpeta
    en `services\`). No cambies `/var/log/lamb/` ni `/logs/*.log`: Promtail ve la carpeta `services\` montada ahí.

Levanta (o reinicia) la observabilidad para que lea el job nuevo:

**Dónde abrir la terminal:** En la **raíz del proyecto** (`Lamb-Modulo02-Admision-Trayectoria`). En VS Code: *Terminal → New Terminal*.

```powershell
docker compose -f obs/compose-dev.yml up -d
docker restart lamb-promtail-dev
```

!!! note "Arranca tu servicio desde su carpeta"
    `logs\evaluacionn.log` se crea **donde arrancas el servicio**. Si lo arrancas desde otra carpeta, Promtail no lo encuentra.

## 3. Ver todo en Grafana

Abre http://localhost:13000 (`admin` / `admin`) → *Explore*.

| Quieres ver | Fuente | Consulta |
|---|---|---|
| Todos los logs de tu servicio | **Loki** | `{application="lamb-evaluacionn-ms"}` |
| Solo errores | Loki | `{application="lamb-evaluacionn-ms"} \|= "ERROR"` |
| Eventos de Kafka | Loki | `{application="lamb-evaluacionn-ms"} \|= "component="` |
| Una petición concreta | Loki | `{application="lamb-evaluacionn-ms"} \|= "PEGA-AQUI-EL-TRACE-ID"` |
| ¿Está arriba? | **Prometheus** | `up{application="LAMB-EVALUACIONN-MS"}` |
| Peticiones por segundo | Prometheus | `rate(http_server_requests_seconds_count{application="LAMB-EVALUACIONN-MS"}[1m])` |

!!! note "Mayúsculas en Prometheus"
    En Prometheus el nombre sale en **MAYÚSCULAS** (`LAMB-EVALUACIONN-MS`) porque viene de Eureka. En Loki va en minúsculas.

El dashboard *Lamb → Lamb - Infraestructura* ya incluye tu servicio.

## 4. Diagnóstico con traceId

1. Haz una petición y mira el header de respuesta:

    ```powershell
    (Invoke-WebRequest http://localhost:18080/api/v1/evaluacionn/saludo -UseBasicParsing).Headers["X-Trace-ID"]
    ```

2. Copia ese valor y búscalo en Loki (tabla de arriba). Verás **solo** las líneas de esa petición:

    ```text
    2026-11-24 18:42:13.351 [5b1f0c3e-...] INFO  p.e.u.l.b.c.SaludoController - Saludo solicitado
    ```

Así encuentras qué pasó en una petición que falló, aunque haya miles de líneas.

!!! note "Archivos de esta sesión"
    Modificas:

    - `obs\promtail\promtail-config-dev.yml` - un job nuevo

## 5. Evidencia de cierre de la S10

| Qué | Link | Qué debes ver |
|---|---|---|
| Prometheus | http://localhost:19090/targets | `lamb-evaluacionn-ms` UP |
| Grafana - logs | http://localhost:13000 → Explore → Loki | Tus logs con `traceId` |
| Grafana - métricas | Explore → Prometheus | Gráfico de peticiones por segundo |

Siguiente: [S11 - Frontend Angular con Keycloak](S11_Integracion_Cliente_Frontend_Angular_Keycloak.md).
