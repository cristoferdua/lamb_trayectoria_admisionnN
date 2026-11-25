# S3 - Registro, descubrimiento y ejecución concurrente de servicios

*Guía única: agregar un microservicio nuevo al proyecto Lamb - Parte 3 de 16 - 2026*

!!! note "Antes de empezar: debe estar levantado"
    Desde la raíz del proyecto (`Lamb-Modulo02-Admision-Trayectoria`). Si ya está corriendo, no lo arranques otra vez.

    **Docker** (Docker Desktop abierto; se pueden pegar en la misma terminal):

    ```powershell
    docker compose -f services/lamb-evaluacionn-ms/compose-dev.yml up -d    # BD de tu servicio
    ```

    **Servicios** (cada uno en **su propia terminal**, en este orden, esperando que diga `Started ...`):

    ```powershell
    cd infra\lamb-config; .\mvnw.cmd spring-boot:run    # Config Server
    ```

    Comprueba que responden:

    - BD de tu servicio: `docker ps` → `lamb-postgres-evaluacionn-dev` *Up*
    - Config Server: http://localhost:18888/lamb-evaluacionn-ms/dev

    Eureka y tu servicio se arrancan **en esta sesión** (paso 3).

## 1. Introducción

**Eureka** (`http://localhost:18761`) es la "guía telefónica" de Lamb: cada servicio se registra al arrancar
y el Gateway lo busca por su nombre (`lb://lamb-evaluacionn-ms`), sin saber su puerto.
**Es igual para los dos caminos (A y B)** y no tienes que escribir código: ya quedó configurado en la S2.

## 2. Lo que ya tienes

Tu `pom.xml` (copiado de Trayectoria) ya trae `spring-cloud-starter-netflix-eureka-client`, y tu
`lamb-evaluacionn-ms-dev.yml` ya trae este bloque:

```yaml
eureka:
  instance:
    hostname: localhost
    instance-id: ${spring.application.name}:${server.port}
  client:
    service-url:
      defaultZone: http://localhost:18761/eureka
```

!!! note "Esto NO se cambia"
    Es el mismo bloque para todos los servicios de Lamb. `instance-id` usa el puerto para que cada instancia
    tenga un nombre distinto.

## 3. Probar

Orden de arranque: **una terminal nueva por línea**, todas abiertas en la raíz del proyecto. Copia **una** línea en cada terminal y espera `Started ...` antes de la siguiente:

```powershell
cd infra\lamb-config;            .\mvnw.cmd spring-boot:run    # 1. Config
cd infra\lamb-eureka;            .\mvnw.cmd spring-boot:run    # 2. Eureka
cd services\lamb-evaluacionn-ms;  .\mvnw.cmd spring-boot:run    # 3. Tu servicio
```

Abre http://localhost:18761 → en *Instances currently registered with Eureka* debe aparecer
**`LAMB-EVALUACIONN-MS`** con `localhost:lamb-evaluacionn-ms:8095`.

!!! note "Tarda ~30 segundos"
    Un servicio recién arrancado tarda hasta medio minuto en aparecer en Eureka.

## 4. Ejecución concurrente: dos instancias

Para demostrar que el sistema escala, levanta una **segunda instancia** de tu servicio en otro puerto
(otra terminal, en la carpeta del servicio):

**Dónde abrir la terminal:** Terminal **nueva** dentro de `services\lamb-evaluacionn-ms` (primero `cd services\lamb-evaluacionn-ms` desde la raíz). La primera instancia sigue corriendo en su terminal.

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8096"
```

Usa un puerto libre distinto al de la primera instancia. En Eureka verás **2 instancias** de `LAMB-EVALUACIONN-MS`
(`...:8095` y `...:8096`). En la S4 el Gateway repartirá las peticiones entre las dos.

## 5. Bonus: Prometheus ya te encontró

Prometheus de Lamb descubre los servicios **a través de Eureka**, así que tus métricas ya se están recolectando:
http://localhost:19090/targets → `lamb-evaluacionn-ms` en verde (si el stack `obs` está levantado; ver S10).

!!! note "Archivos de esta sesión"
    En esta sesión **no creas ni cambias archivos**: solo arrancas y compruebas.

## 6. Evidencia de cierre de la S3

| Qué | Link | Qué debes ver |
|---|---|---|
| Eureka | http://localhost:18761 | `LAMB-EVALUACIONN-MS` (1 o 2 instancias) |
| Instancia 1 | http://localhost:8095/actuator/health | `UP` |
| Instancia 2 | http://localhost:8096/actuator/health | `UP` |

Siguiente: [S4 - Punto único de acceso](S04_Punto_Unico_Acceso_Distribucion_Trafico.md).
