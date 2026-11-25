# S4 - Punto único de acceso y distribución de tráfico

*Guía única: agregar un microservicio nuevo al proyecto Lamb - Parte 4 de 16 - 2026*

!!! note "Antes de empezar: debe estar levantado"
    Desde la raíz del proyecto (`Lamb-Modulo02-Admision-Trayectoria`). Si ya está corriendo, no lo arranques otra vez.

    **Docker** (Docker Desktop abierto; se pueden pegar en la misma terminal):

    ```powershell
    docker compose -f services/lamb-evaluacionn-ms/compose-dev.yml up -d    # BD de tu servicio
    ```

    **Servicios** (cada uno en **su propia terminal**, en este orden, esperando que diga `Started ...`):

    ```powershell
    cd infra\lamb-config; .\mvnw.cmd spring-boot:run    # Config Server
    cd infra\lamb-eureka; .\mvnw.cmd spring-boot:run    # Eureka
    cd services\lamb-evaluacionn-ms; .\mvnw.cmd spring-boot:run    # Tu servicio
    cd services\lamb-evaluacionn-ms; .\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8096"    # 2da instancia
    ```

    Comprueba que responden:

    - BD de tu servicio: `docker ps` → `lamb-postgres-evaluacionn-dev` *Up*
    - Config Server: http://localhost:18888/lamb-evaluacionn-ms/dev
    - Eureka: http://localhost:18761
    - Tu servicio: http://localhost:8095/actuator/health
    - 2da instancia: http://localhost:8096/actuator/health

    El Gateway se (re)arranca **en esta sesión** (paso 4), cuando tu servicio ya salga en Eureka.

## 1. Introducción

El frontend **nunca** llama directo a tu servicio (`8095`): todo entra por el **Gateway** (`http://localhost:18080`),
que busca el servicio en Eureka y reparte las peticiones entre sus instancias.
Tocas 2 archivos del Gateway: **la ruta** y **el permiso**.

## 2. La ruta: `infra\lamb-config\config-repo\lamb-gateway-dev.yml`

Una ruta **por recurso**.

**Abre el archivo:** `lamb-gateway-dev.yml`  
**En la ruta:** `infra\lamb-config\config-repo\`  
**Qué hacer:** Busca (`Ctrl+F`) la línea `- id: lamb-finanzas` y pega **justo arriba** de ella el bloque de tu camino (A **o** B, no los dos). No borres lo demás del archivo.


### Camino A - Sin CRUD

```yaml
            - id: lamb-evaluacionn-saludo
              uri: lb://lamb-evaluacionn-ms
              predicates:
                - Path=/api/v1/evaluacionn/**
```

### Camino B - Con CRUD

Las dos rutas (la del Hola mundo y la del CRUD):

```yaml
            - id: lamb-evaluacionn-saludo
              uri: lb://lamb-evaluacionn-ms
              predicates:
                - Path=/api/v1/evaluacionn/**
            - id: lamb-evaluacionn-examenes
              uri: lb://lamb-evaluacionn-ms
              predicates:
                - Path=/api/v1/examenes/**
```

!!! note "Qué cambia"
    - `id`: un nombre único (`lamb-` + servicio + recurso).
    - `uri: lb://lamb-evaluacionn-ms`: el `spring.application.name` de tu servicio (`lb://` = buscarlo en Eureka y balancear).
    - `Path`: la ruta de tu controlador (`@RequestMapping`). Si el profe pide otro CRUD, agrega otra ruta igual con su `Path`.

    No cambies la indentación (12 espacios antes de `- id:`): si está mal, el Gateway no arranca.

## 3. El permiso (fase 1, todo abierto)

El Gateway de Lamb **rechaza** (`401`) toda ruta que no tenga permiso.

**Abre el archivo:** `SecurityConfig.java`  
**En la ruta:** `infra\lamb-gateway\src\main\java\pe\edu\upeu\lamb\gateway\config\`  
**Qué hacer:** Busca (`Ctrl+F`) la línea `.requestMatchers(TRAYECTORIA).permitAll()` y pega **en la línea de abajo** la línea de tu camino (A **o** B). No borres lo demás del archivo.


### Camino A - Sin CRUD

```java
                        .requestMatchers("/api/v1/evaluacionn/**").permitAll()
```

### Camino B - Con CRUD

```java
                        .requestMatchers("/api/v1/evaluacionn/**", "/api/v1/examenes/**").permitAll()
```

!!! note "Es temporal"
    Igual que Admisión y Trayectoria en "DEV Fase 1". En la **S7** se reemplaza por reglas con roles de Keycloak.

## 4. Reiniciar el Gateway

Cambiaste código Java del Gateway: detenlo (Ctrl+C) y vuelve a arrancarlo **al final**, cuando tu servicio
ya esté en Eureka:

**Dónde abrir la terminal:** En la terminal donde corre el Gateway: pulsa `Ctrl+C` para detenerlo. Si no lo tenías corriendo, abre una terminal nueva en la raíz del proyecto. Arráncalo **al final**, cuando tu servicio ya salga en Eureka.

```powershell
cd infra\lamb-gateway
.\mvnw.cmd spring-boot:run
```

!!! note "Windows: el .jar bloqueado"
    Si el Gateway lo corres con `java -jar`, detenlo **antes** de recompilar; si no, sale `Unable to rename ... .jar`.

## 5. Probar

| Qué | Link | Qué debes ver |
|---|---|---|
| Hola mundo por el Gateway | http://localhost:18080/api/v1/evaluacionn/saludo | `Hola mundo desde lamb-evaluacionn-ms` |
| **Camino B:** CRUD por el Gateway | http://localhost:18080/api/v1/examenes | `[]` o la lista |

**Camino B:** prueba el CRUD completo por el Gateway con PowerShell:

**Dónde abrir la terminal:** Cualquier PowerShell (no importa la carpeta). Pega **una línea a la vez** para ver qué responde cada una.

```powershell
$api = "http://localhost:18080/api/v1/examenes"                       # cambia por tu ruta
Invoke-RestMethod -Method Post $api -ContentType "application/json; charset=utf-8" -Body '{"titulo":"Parcial 1","curso":"Matematica"}'
Invoke-RestMethod $api                                              # mira el id
Invoke-RestMethod -Method Put "$api/1" -ContentType "application/json; charset=utf-8" -Body '{"titulo":"Parcial 1 (recuperacion)","curso":"Matematica"}'
Invoke-RestMethod -Method Delete "$api/1"
```

El `-Body` lleva **tus** atributos.

## 6. Distribución de tráfico entre instancias

Con las 2 instancias de la S3 levantadas, llama varias veces al Gateway:

```powershell
1..6 | ForEach-Object { Invoke-RestMethod http://localhost:18080/api/v1/evaluacionn/saludo }
```

En las consolas de **las dos** instancias aparece `Saludo solicitado`: el Gateway reparte las peticiones (balanceo de carga).

!!! note "Archivos de esta sesión"
    Modificas:

    - `infra\lamb-config\config-repo\lamb-gateway-dev.yml` - las rutas
    - `infra\lamb-gateway\src\main\java\pe\edu\upeu\lamb\gateway\config\SecurityConfig.java` - la línea `permitAll()` (temporal, **se reemplaza en la S7**)

## 7. Evidencia de cierre de la S4

- Hola mundo (y en el camino B, el CRUD) responde por `http://localhost:18080`.
- Las peticiones se reparten entre las 2 instancias.

!!! note "Si el Gateway responde..."
    - `404`: falta la ruta del paso 2 o no reiniciaste el Gateway.
    - `401`: falta el permiso del paso 3.
    - `503` / `500 Unable to find instance`: tu servicio aún no está en Eureka; espera 30 s.

Siguiente: [S5 - Evaluación de la Unidad I](S05_Evaluacion_Unidad_1.md).
