# S2 - Gestión centralizada de configuración y ambientes

*Guía única: agregar un microservicio nuevo al proyecto Lamb - Parte 2 de 16 - 2026*

!!! note "Antes de empezar: debe estar levantado"
    Desde la raíz del proyecto (`Lamb-Modulo02-Admision-Trayectoria`). Si ya está corriendo, no lo arranques otra vez.

    **Docker** (Docker Desktop abierto; se pueden pegar en la misma terminal):

    ```powershell
    docker compose -f services/lamb-evaluacionn-ms/compose-dev.yml up -d    # BD de tu servicio
    ```

    Comprueba que responden:

    - BD de tu servicio: `docker ps` → `lamb-postgres-evaluacionn-dev` *Up*

    El Config Server y tu servicio se arrancan **en esta sesión** (paso 4).

## 1. Introducción

En Lamb, ningún microservicio guarda su configuración (puerto, base de datos, Kafka, Keycloak...) dentro
de su código: la pide al **Config Server** (`http://localhost:18888`) al arrancar. En esta sesión tu servicio
hace lo mismo. **Es igual para los dos caminos (A y B).**

| Archivo | Dónde | Qué tiene |
|---|---|---|
| `application.yml` | dentro de tu servicio | solo el nombre y dónde está el Config Server |
| `lamb-evaluacionn-ms-dev.yml` | `infra\lamb-config\config-repo\` | toda la configuración del ambiente DEV |

## 2. `src\main\resources\application.yml` (en tu servicio)

**Crea el archivo:** `application.yml`  
**En la ruta:** `services\lamb-evaluacionn-ms\src\main\resources\`  
**Cómo:** clic derecho en la carpeta `resources` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Ojo:** Va en `resources`, al lado de `logback-spring.xml`.  
**Y pega el siguiente código:**

```yaml
spring:
  application:
    name: lamb-evaluacionn-ms
  profiles:
    active: dev
  config:
    import: "optional:configserver:${CONFIG_SERVER_URL:http://localhost:18888}"
```

**Qué cambiar en este código** (ejemplo: si tu servicio es **Tesorería** y tu CRUD es **Pagos** con `monto` y `fecha`):

- `evaluacionn` (aparece 1 vez) → el nombre de **tu servicio**, en minúscula. Ej.: `tesoreria`.
- Todo lo demás se copia **igual**. Tip: `Ctrl+H` en VS Code, con **Aa** activado (respeta mayúsculas).

!!! note "Más detalle"
    Solo `name: lamb-evaluacionn-ms`. No cambies `profiles.active: dev` ni la línea `config.import`.

## 3. `infra\lamb-config\config-repo\lamb-evaluacionn-ms-dev.yml`

Crea este archivo en el Config Server. Ya trae todo lo que se usará en las siguientes sesiones
(Eureka en S3, Keycloak en S7, Kafka en S8, métricas en S10), así no vuelves a tocarlo.

**Crea el archivo:** `lamb-evaluacionn-ms-dev.yml`  
**En la ruta:** `infra\lamb-config\config-repo\`  
**Cómo:** clic derecho en la carpeta `config-repo` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Ojo:** Esta carpeta está en `infra` (**fuera** de tu servicio), junto a los `-dev.yml` de los otros servicios.  
**Y pega el siguiente código:**

```yaml
server:
  port: 8095

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:18180/realms/lamb

  datasource:
    url: jdbc:postgresql://localhost:15460/lamb_evaluacionn_db
    username: lamb
    password: lamb
    driver-class-name: org.postgresql.Driver
  flyway:
    enabled: true
    locations: classpath:db/migration
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  devtools:
    restart:
      enabled: true
      quiet-period: 2s
      poll-interval: 3s
    livereload:
      enabled: true
  kafka:
    bootstrap-servers: localhost:18092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JacksonJsonSerializer
      properties:
        spring.json.add.type.headers: false
    consumer:
      group-id: lamb-evaluacionn-ms
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
      properties:
        spring.deserializer.value.delegate.class: org.springframework.kafka.support.serializer.JacksonJsonDeserializer
        spring.json.value.default.type: pe.edu.upeu.lamb.evaluacionn.event.EvaluacionnEvento
        spring.json.trusted.packages: pe.edu.upeu.lamb.evaluacionn.event
        spring.json.use.type.headers: false

app:
  kafka:
    topic:
      evaluacionn: evaluacionn-eventos

springdoc:
  swagger-ui:
    path: /swagger-ui.html

logging:
  level:
    pe.edu.upeu.lamb.evaluacionn: DEBUG

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
      group:
        liveness:
          include: livenessState
        readiness:
          include: readinessState, db

eureka:
  instance:
    hostname: localhost
    instance-id: ${spring.application.name}:${server.port}
  client:
    service-url:
      defaultZone: http://localhost:18761/eureka
```

**Qué cambiar en este código** (ejemplo: si tu servicio es **Tesorería** y tu CRUD es **Pagos** con `monto` y `fecha`):

- `evaluacionn` (aparece 7 veces) → el nombre de **tu servicio**, en minúscula. Ej.: `tesoreria`.
- `Evaluacionn` (aparece 1 vez) → el nombre de **tu servicio**, con mayúscula. Ej.: `Tesoreria`.
- `8095` (aparece 1 vez) → el puerto de **tu servicio**.
- `15460` (aparece 1 vez) → el puerto de la **base de datos** de tu servicio.
- Todo lo demás se copia **igual**. Tip: `Ctrl+H` en VS Code, con **Aa** activado (respeta mayúsculas).

**Qué cambiar según tu caso y qué no:**

| Línea | Cambia | No cambia |
|---|---|---|
| `server.port: 8095` | tu puerto | |
| `issuer-uri: .../realms/lamb` | | el realm de Lamb es siempre `lamb` |
| `datasource.url: ...localhost:15460/lamb_evaluacionn_db` | puerto y nombre de BD (los del `compose-dev.yml`) | `jdbc:postgresql://localhost:` |
| `username` / `password` | solo si los cambiaste en el `compose-dev.yml` | |
| `flyway`, `jpa`, `devtools` | | todo |
| `kafka.bootstrap-servers: localhost:18092` | | el Kafka de Lamb |
| `group-id: lamb-evaluacionn-ms` | tu nombre de servicio | |
| `spring.json.value.default.type` | `pe.edu.upeu.lamb.TUSERVICIO.event.TuEvento` | |
| `spring.json.trusted.packages` | `pe.edu.upeu.lamb.TUSERVICIO.event` | |
| `app.kafka.topic.evaluacionn: evaluacionn-eventos` | la clave y el nombre del topic | |
| `logging.level.pe.edu.upeu.lamb.evaluacionn` | tu paquete | |
| `management`, `eureka` | | todo |

!!! note "El nombre del archivo importa"
    Debe ser exactamente `spring.application.name` + `-dev.yml` → `lamb-evaluacionn-ms-dev.yml`.
    Si no coincide, el servicio arranca sin configuración y falla.

## 4. Probar

1. Arranca el Config Server (si no está corriendo). Terminal **nueva** en la raíz del proyecto:

    ```powershell
    cd infra\lamb-config
    .\mvnw.cmd spring-boot:run
    ```

2. Abre http://localhost:18888/lamb-evaluacionn-ms/dev → debe mostrar un JSON con tu configuración
   (busca `"server.port": 8095`).

3. Arranca tu servicio desde **su** carpeta (siempre desde ahí, para que los logs queden en `logs\`). Otra terminal **nueva** en la raíz del proyecto (el Config Server debe seguir corriendo en la suya):

    ```powershell
    cd services\lamb-evaluacionn-ms
    .\mvnw.cmd spring-boot:run
    ```

En el log debe salir:

```text
Fetching config from server at : http://localhost:18888
Located environment: name=lamb-evaluacionn-ms, profiles=[dev]
...
Started LambEvaluacionnMsApplication in ... seconds
```

**Camino B:** además debe salir `Successfully applied 1 migration to schema "public", now at version v1` (Flyway creó la tabla).

## 5. Ver la base de datos en VS Code

Con la extensión **Database Client** (ícono de base de datos en la barra izquierda de VS Code) ves tu BD,
sus tablas y sus datos sin usar comandos.

**1. Instalar la extensión (solo la primera vez):** `Ctrl+Shift+X` → busca **Database Client** (curso *Weijan Chen*) → *Install*.

**2. Crear la conexión:** clic en el ícono de base de datos → botón **+** (*Create Connection*) → en *Server Type*
elige **PostgreSQL** y llena:

| Campo | Valor |
|---|---|
| Host | `127.0.0.1` |
| Port | `15460` |
| Username | `lamb` |
| Password | `lamb` |
| Database | `lamb_evaluacionn_db` |

Clic en **Connect** (o *Save*). La conexión queda guardada para la próxima vez.

!!! note "Qué cambia"
    Port, Database, Username y Password son **los de tu `compose-dev.yml`** (`ports:` el número de la izquierda,
    `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`). Host siempre es `127.0.0.1`.

**3. Ver las tablas:** despliega la conexión → `lamb_evaluacionn_db` → `public` → *Tables*. Aparecen cuando tu servicio arrancó una vez (Flyway las crea al arrancar).

| Tabla | Qué es |
|---|---|
| `examenes` | **Camino B:** tu tabla. Clic en ella para ver los registros (se llenan al agregar desde la página o la API) |
| `flyway_schema_history` | La crea Flyway: lista de migraciones aplicadas (`V1`, `V2`...). **No la modifiques** |

**4. Hacer una consulta:** clic derecho en la conexión → *New Query* (o el ícono de consulta), escribe y ejecuta (`Ctrl+Enter`):

```sql
SELECT * FROM examenes;
SELECT * FROM flyway_schema_history;
```

Si agregas registros desde la página, clic derecho en la tabla → *Refresh* para ver los cambios.

!!! note "No cambies la tabla desde aquí"
    Ver datos y hacer `SELECT` está bien, pero los cambios a la **estructura** (agregar o quitar columnas) se hacen
    con una migración `V2__...sql`. Si los haces a mano, la entidad y Flyway dejan de coincidir y el servicio no arranca.

!!! note "Si no conecta"
    `Connection refused`: la BD no está levantada → `docker ps` y levántala. `password authentication failed`:
    revisa usuario y contraseña con los del `compose-dev.yml`.

!!! note "Archivos de esta sesión"
    Creas:

    - `services\lamb-evaluacionn-ms\src\main\resources\application.yml`
    - `infra\lamb-config\config-repo\lamb-evaluacionn-ms-dev.yml`

## 6. Evidencia de cierre de la S2

| Qué | Link | Qué debes ver |
|---|---|---|
| Config Server | http://localhost:18888/lamb-evaluacionn-ms/dev | Tu configuración |
| Salud | http://localhost:8095/actuator/health | `"status":"UP"` (y `db` UP) |
| Hola mundo directo | http://localhost:8095/api/v1/evaluacionn/saludo | `Hola mundo desde lamb-evaluacionn-ms` |
| Swagger | http://localhost:8095/swagger-ui.html | Tus endpoints |
| **Camino B:** CRUD directo | http://localhost:8095/api/v1/examenes | `[]` |
| BD en VS Code | Database Client → `lamb_evaluacionn_db` | `flyway_schema_history` (y en B, `examenes`) |

Siguiente: [S3 - Registro y descubrimiento](S03_Registro_Descubrimiento_Ejecucion_Concurrente.md).
