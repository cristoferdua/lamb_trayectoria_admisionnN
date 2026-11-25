# Solución de errores

*Guía única: agregar un microservicio nuevo al proyecto Lamb - Si te sale un error, búscalo aquí - 2026*

## 1. Cómo usar esta página

1. Copia la parte importante del error (por ejemplo `Port 8095 was already in use`).
2. Búscala con `Ctrl+F` en esta página o en el buscador de arriba.
3. Sigue los pasos de **Solución**. Cada error dice **qué archivo abrir** y **qué cambiar**.

!!! note "Dónde está el error de verdad"
    En Spring Boot el error largo tiene muchas líneas. La importante es la **última** que empieza con
    `Caused by:`, o la que está debajo de `APPLICATION FAILED TO START` → `Description:`.
    En Angular, mira la terminal de `npm start` y la consola del navegador (`F12` → *Console*).

En todos los ejemplos cambia `evaluacionn`, `examenes`, `8095` y `15460` por los de tu servicio.

## 2. Puertos ocupados

### 2.1 `Port 8095 was already in use` (tu servicio)

**Te sale:**

```text
Web server failed to start. Port 8095 was already in use.
```

**Por qué:** otro programa (casi siempre **otra copia de tu mismo servicio** que dejaste abierta) usa ese puerto.

**Solución A - cerrar al que lo ocupa (recomendado):**

```powershell
Get-NetTCPConnection -LocalPort 8095 -State Listen | Select-Object OwningProcess
Get-Process -Id 12345           # cambia 12345 por el número que salió: mira qué programa es
Stop-Process -Id 12345 -Force   # ciérralo si es un java viejo tuyo
```

**Solución B - cambiar el puerto de tu servicio:**

1. Abre `infra\lamb-config\config-repo\lamb-evaluacionn-ms-dev.yml`.
2. Cambia `server.port: 8095` por un puerto libre, ej. `server.port: 8097`.
3. Vuelve a arrancar tu servicio (el Config Server lee el cambio solo, no hace falta reiniciarlo).
4. El Gateway **no** se toca (busca tu servicio por nombre en Eureka). Solo cambian los links directos
   (`http://localhost:8097/swagger-ui.html`, etc.).

Para saber si un puerto está libre (si **no imprime nada**, está libre):

```powershell
Get-NetTCPConnection -LocalPort 8097 -State Listen -ErrorAction SilentlyContinue
```

### 2.2 `port is already allocated` (la base de datos en Docker)

**Te sale:**

```text
Error response from daemon: ... Bind for 0.0.0.0:15460 failed: port is already allocated
```

**Por qué:** otro contenedor (de otro servicio o de otro proyecto) ya usa ese puerto.

**Solución A - ver quién lo usa y detenerlo:**

```powershell
docker ps --format "table {{.Names}}\t{{.Ports}}"
docker stop NOMBRE-DEL-CONTENEDOR
```

**Solución B - cambiar el puerto de tu BD** (son **2 archivos**, deben coincidir):

1. `services\lamb-evaluacionn-ms\compose-dev.yml` → `"15460:5432"` por `"15461:5432"`
   (cambia solo el número de la **izquierda**; `5432` no se toca).
2. `infra\lamb-config\config-repo\lamb-evaluacionn-ms-dev.yml` →
   `url: jdbc:postgresql://localhost:15460/...` por `localhost:15461/...`.
3. `docker compose -f services/lamb-evaluacionn-ms/compose-dev.yml up -d` y reinicia tu servicio.

### 2.3 `Port 4200 is already in use` (frontend)

**Te sale:** `Port 4200 is already in use. Would you like to use a different port?`

**Solución:** responde **No** y cierra la otra terminal donde corre `npm start` (o el proceso, como en 2.1).

!!! note "No cambies el puerto del frontend"
    Keycloak solo acepta volver a `http://localhost:4200/*`. Si usas otro puerto, el login falla con
    `Invalid parameter: redirect_uri` (ver 7.1).

### 2.4 Puertos de Lamb ocupados (18080, 18092, 18180, 18761, 18888, 13000...)

**Por qué:** casi siempre es **el mismo componente ya levantado** (por ejemplo, Eureka abierto dos veces)
o un contenedor de otro proyecto.

**Solución:** **no cambies** estos puertos (todos los servicios los usan). Cierra lo que los ocupa:
mira el proceso como en 2.1 o el contenedor como en 2.2.

## 3. Arranque del servicio y configuración

### 3.1 `Could not resolve placeholder 'app.kafka.topic.evaluacionn'`

**Por qué:** tu servicio arrancó **sin su configuración**: o el Config Server está apagado, o el archivo no se llama
igual que el servicio, o falta la clave.

**Solución:**

1. Abre http://localhost:18888/lamb-evaluacionn-ms/dev. Si no carga, arranca el Config Server (S2).
2. Si carga pero `propertySources` está vacío (`[]`): el archivo debe llamarse **exactamente**
   `spring.application.name` + `-dev.yml`. Compara `src\main\resources\application.yml` (`name: lamb-evaluacionn-ms`)
   con el archivo `infra\lamb-config\config-repo\lamb-evaluacionn-ms-dev.yml`.
3. Si el archivo está bien: la clave `app.kafka.topic.evaluacionn` del `-dev.yml` debe ser **igual** a la del
   `@Value("${app.kafka.topic.evaluacionn}")` de `KafkaTopicsConfig.java`.

### 3.2 `Connection refused` a `localhost:18888` al arrancar

**Te sale:** `Could not locate PropertySource ... I/O error on GET request for "http://localhost:18888/..."`

**Solución:** arranca primero el Config Server (`cd infra\lamb-config; .\mvnw.cmd spring-boot:run`) y espera a que
diga `Started`. Orden completo en la [S13](S13_Validacion_End_to_End_Producto.md).

### 3.3 Error de YAML: `mapping values are not allowed here` / `while parsing a block mapping`

**Por qué:** en un `.yml` la **sangría** (espacios) está mal o usaste tabulador.

**Solución:** abre el archivo que dice el error (normalmente el `-dev.yml` o `lamb-gateway-dev.yml`) en la línea
indicada. Usa **solo espacios**, nunca `Tab`, y alinea con las líneas de arriba. En el Gateway, cada `- id:`
va con **12 espacios**.

### 3.4 `JAVA_HOME not found` / `release version 21 not supported`

**Por qué:** no tienes Java 21 o Windows usa otro Java.

**Solución:**

```powershell
java -version        # debe decir 21
```

Si dice otro número, instala el JDK 21 (Temurin) y configura `JAVA_HOME` a su carpeta
(Configuración de Windows → *Variables de entorno*). Cierra y vuelve a abrir la terminal.

### 3.5 `Could not resolve dependencies` / Maven no descarga

**Solución:** revisa tu internet y ejecuta `.\mvnw.cmd -U -DskipTests package` en la carpeta del servicio.

### 3.6 `Unable to rename '...jar'` / `The process cannot access the file`

**Por qué:** el servicio (o el Gateway) sigue corriendo mientras lo recompilas.

**Solución:** detenlo con `Ctrl+C` (o `Stop-Process` como en 2.1) y vuelve a compilar.

## 4. Compilación (código Java)

### 4.1 `package pe.edu.upeu.lamb.evaluacionn... does not exist` / `cannot find symbol`

**Por qué:** al cambiar los nombres, quedó un `package` o un `import` con el nombre viejo, o el archivo está en otra
carpeta que su `package`.

**Solución:** la primera línea de cada `.java` (`package ...`) debe coincidir con su carpeta.
Busca lo que quedó con el nombre viejo:

```powershell
Get-ChildItem services\lamb-tesoreria-ms\src -Recurse -File | Select-String -Pattern "evaluacionn|examen"
```

### 4.2 `cannot find symbol: method getTitulo()` / `builder()`

**Por qué:** Lombok no generó el código: falta su anotación (`@Data`, `@Builder`...) o el `pom.xml` cambió.

**Solución:** revisa que la clase tenga sus anotaciones (como en la S1) y que el `pom.xml` sea el copiado de
Trayectoria. Si usas otro atributo, el método cambia de nombre (`metodoPago` → `getMetodoPago()`).

### 4.3 `No qualifying bean of type '...ExamenMapper'` / `ExamenMapperImpl` no existe

**Por qué:** falta el procesador de MapStruct.

**Solución:** en el `pom.xml` agrega `mapstruct-processor` en **las dos** `<annotationProcessorPaths>`
([S1, paso 6.5](S01_Construccion_Servicio_Base.md#65-mapperexamenmapperjava-mapstruct)) y recompila
con `.\mvnw.cmd clean package -DskipTests`.

### 4.4 `Unmapped target property: "x"` (aviso de MapStruct)

**Por qué:** un atributo de la entidad no existe en el Request (o se llama distinto).

**Solución:** usa el **mismo nombre** en entidad, Request y Response. Si es a propósito (como `id`), agrega
`@Mapping(target = "x", ignore = true)` en `toEntity`.

## 5. Base de datos y Flyway

### 5.1 `Connection to localhost:15460 refused`

**Solución:** la BD no está levantada. Abre **Docker Desktop** y ejecuta:

```powershell
docker compose -f services/lamb-evaluacionn-ms/compose-dev.yml up -d
docker ps     # debe salir lamb-postgres-evaluacionn-dev "Up"
```

### 5.2 `error during connect ... dockerDesktopLinuxEngine` / `docker` no responde

**Solución:** Docker Desktop está cerrado. Ábrelo y espera a que diga *Engine running*.

### 5.3 `password authentication failed for user "lamb"`

**Por qué:** el usuario o contraseña del `-dev.yml` no es igual al del `compose-dev.yml`, o el volumen se creó
antes con otra contraseña.

**Solución:** iguala `username`/`password` del `-dev.yml` con `POSTGRES_USER`/`POSTGRES_PASSWORD` del `compose-dev.yml`.
Si ya son iguales, recrea la BD (**borra los datos de prueba**):

```powershell
docker compose -f services/lamb-evaluacionn-ms/compose-dev.yml down -v
docker compose -f services/lamb-evaluacionn-ms/compose-dev.yml up -d
```

### 5.4 `database "lamb_evaluacionn_db" does not exist`

**Solución:** el nombre de la BD del `-dev.yml` (`.../lamb_evaluacionn_db`) debe ser igual a `POSTGRES_DB` del
`compose-dev.yml`. Si los cambiaste después de crear el contenedor, recréalo con `down -v` (5.3).

### 5.5 `Migration checksum mismatch for migration version 1`

**Por qué:** editaste `V1__create_examenes.sql` después de que Flyway ya lo ejecutó.

**Solución:** devuelve el `V1` como estaba y pon el cambio en un archivo nuevo `V2__...sql`
(ej. `ALTER TABLE examenes ADD COLUMN anio INTEGER;`). En desarrollo también puedes borrar la BD con `down -v` (5.3).

### 5.6 `Found non-empty schema(s) "public" but no schema history table`

**Por qué:** tu servicio se conectó a una BD que ya tenía tablas de **otro** servicio (puerto o volumen repetido).

**Solución:** en `compose-dev.yml` usa un puerto propio y un `name:` de volumen **único**
(`lamb-evaluacionn-data`), y apunta el `-dev.yml` a ese puerto. Luego `down -v` y `up -d`.

### 5.7 `Schema-validation: missing table [examenes]` / `missing column [x]`

**Por qué:** la entidad no coincide con la tabla (`ddl-auto: validate` lo revisa).

**Solución:** compara `@Table(name = "examenes")` y cada `@Column(name = "...")` con el SQL. Si falta una columna,
agrégala con una migración `V2`. Recuerda: `metodo_pago` en SQL ↔ `metodoPago` en Java.

### 5.8 `Schema-validation: wrong column type ... found [numeric], but expecting [float(53)]`

**Solución:** usa el tipo Java de la tabla de la [S1, 6.1](S01_Construccion_Servicio_Base.md#61-traduce-los-atributos-que-te-piden):
`NUMERIC(10,2)` ↔ `BigDecimal`, `DATE` ↔ `LocalDate`, `INTEGER` ↔ `Integer`.

## 6. Eureka y Gateway

### 6.1 Tu servicio no aparece en Eureka

**Solución:**

1. Espera 30 segundos y recarga http://localhost:18761.
2. Si en el log sale `Cannot execute request on any known server`: Eureka está apagado → arráncalo.
3. Revisa que el `-dev.yml` tenga el bloque `eureka:` de la [S3](S03_Registro_Descubrimiento_Ejecucion_Concurrente.md).

### 6.2 El Gateway responde `404 Not Found`

**Solución:** falta la ruta en `infra\lamb-config\config-repo\lamb-gateway-dev.yml` o el `Path` no coincide con el
`@RequestMapping` del controlador ([S4](S04_Punto_Unico_Acceso_Distribucion_Trafico.md)). Reinicia el Gateway.

### 6.3 El Gateway responde `401 Unauthorized`

**Solución:** un `GET` con `401` → falta la regla `permitAll` en el `SecurityConfig` del Gateway
([S7](S07_Seguridad_Distribuida_Control_Acceso.md)). Un `POST`/`PUT`/`DELETE` sin login da `401` **a propósito**:
pruébalo desde el frontend con `admin`.

### 6.4 `403 Forbidden` / "No tienes permiso para esta acción."

**Solución:** el usuario no tiene el rol que pide el Gateway. Asígnalo en Keycloak
([S7b](S07b_Seguridad_Distribuida_Keycloak.md)) y **cierra sesión y vuelve a entrar** (el token viejo no tiene el rol).

### 6.5 `503 Service Unavailable` / `Unable to find instance for lamb-evaluacionn-ms`

**Solución:** el Gateway no encuentra tu servicio en Eureka: arráncalo, espera 30 s y revisa que `uri: lb://...`
sea **igual** a `spring.application.name`.

### 6.6 El Gateway no arranca después de tu cambio

**Solución:** casi siempre es la sangría del `lamb-gateway-dev.yml` (3.3) o un `;`/paréntesis faltante en el
`SecurityConfig.java` del Gateway. Lee el `Caused by:`.

## 7. Keycloak y login

### 7.1 `Invalid parameter: redirect_uri`

**Por qué:** abriste el frontend en un puerto o dirección que Keycloak no conoce.

**Solución:** usa `http://localhost:4200` (no `127.0.0.1` ni otro puerto). Si de verdad necesitas otro: Keycloak →
realm `lamb` → *Clients* → `lamb-frontend` → agrega la URL en *Valid redirect URIs* y *Web origins*.

### 7.2 La página de login no carga / `Keycloak init failed`

**Solución:** Keycloak está apagado o todavía arrancando (tarda ~1 minuto):

```powershell
docker compose -f keycloak/compose-dev.yml up -d
```

Abre http://localhost:18180/realms/lamb: si responde, recarga el frontend.

### 7.3 Tu servicio responde `401` aunque hay login: `The iss claim is not valid`

**Solución:** en el `-dev.yml`, `issuer-uri` debe ser exactamente `http://localhost:18180/realms/lamb`
(mismo host y puerto que usa el frontend).

## 8. Kafka

### 8.1 `Connection to node -1 (localhost/127.0.0.1:18092) could not be established`

**Solución:** Kafka está apagado:

```powershell
docker compose -f kafka/compose-dev.yml up -d
```

### 8.2 `The class '...' is not in the trusted packages` / el consumidor no recibe

**Solución:** en el `-dev.yml`, `spring.json.trusted.packages` y `spring.json.value.default.type` deben tener
**el paquete y nombre real** de tu clase de evento (`pe.edu.upeu.lamb.evaluacionn.event.EvaluacionnEvento`). Revisa
también que el `group-id` sea el de tu servicio.

### 8.3 Se guarda pero no sale `component=producer` en el log

**Solución:** revisa que el servicio llame a `producer.publicarTrasCommit(...)` en `crear`, `actualizar` y `eliminar`
([S8](S08_Mensajeria_Asincrona_Entre_Servicios.md)). Si la operación falló (`400`/`404`), es **normal** que no publique ([S9](S09_Consistencia_Distribuida_Procesos_Negocio.md)).

## 9. Observabilidad

### 9.1 Grafana no muestra logs de tu servicio

**Solución:**

1. Revisa que exista `services\lamb-evaluacionn-ms\logs\evaluacionn.log`. Si no existe, arrancaste el servicio desde
   otra carpeta: arráncalo desde `services\lamb-evaluacionn-ms`.
2. Revisa el job en `obs\promtail\promtail-config-dev.yml` ([S10](S10_Observabilidad_Diagnostico_Sistemas_Distribuidos.md)):
   la ruta debe tener el **nombre exacto de la carpeta** de tu servicio.
3. `docker restart lamb-promtail-dev`.
4. Abre http://localhost:13100/loki/api/v1/label/application/values: debe aparecer `lamb-evaluacionn-ms`.

### 9.2 Prometheus muestra tu servicio `DOWN`

**Solución:** revisa http://localhost:8095/actuator/prometheus. Si da `404`, falta el bloque `management:` del `-dev.yml`
([S2](S02_Configuracion_Centralizada_Ambientes.md)). Si da `401`, el `SecurityConfig` del servicio debe dejar todo con `permitAll` ([S7](S07_Seguridad_Distribuida_Control_Acceso.md)).

### 9.3 No puedo entrar a Grafana

**Solución:** usuario `admin`, contraseña `admin`. Si no abre: `docker compose -f obs/compose-dev.yml up -d`.

## 10. Frontend Angular

### 10.1 `'ng' no se reconoce` / `Cannot find module`

**Solución:** en `clients\lamb-frontend` ejecuta `npm install` (solo la primera vez) y luego `npm start`.

### 10.2 `npm.ps1 no se puede cargar ... la ejecución de scripts está deshabilitada`

**Solución:** una sola vez en PowerShell:

```powershell
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
```

### 10.3 `Can't bind to 'formGroup'` / `'x' is not a known element`

**Solución:** en el `.ts` del componente falta el import en `imports: [...]`: `ReactiveFormsModule` para formularios,
`RouterLink` para links. Copia el bloque `imports` de la [S11](S11_Integracion_Cliente_Frontend_Angular_Keycloak.md).

### 10.4 `blocked by CORS policy` en la consola

**Solución:** el frontend debe llamar al **Gateway** (`environment.apiUrl` = `http://localhost:18080`), nunca directo
al `8095`. Abre el frontend en `http://localhost:4200`.

### 10.5 La página sale en blanco o vuelve al inicio

**Solución:** revisa la ruta en `app.routes.ts` (que el `import('./features/...')` apunte al archivo correcto y que
`.then((m) => m.Saludo)` use el nombre real de la clase) y que esté **antes** de `{ path: '**', redirectTo: '' }`.

### 10.6 Tildes salen raras (`TÃ­tulo`) al probar con PowerShell

**Solución:** agrega `charset=utf-8`: `-ContentType "application/json; charset=utf-8"`.

## 11. La PC se pone lenta o Docker se cae

**Por qué:** Lamb completo usa mucha memoria (Keycloak, Kafka, Grafana, varios Java).

**Solución:** levanta solo lo necesario para lo que pruebas, y detén lo demás:

```powershell
docker compose -f obs/compose-dev.yml stop     # observabilidad, si no la estás usando
```

Cierra las terminales de servicios que no uses (Admisión y Trayectoria no hacen falta para probar el tuyo).

## 12. Si nada funciona

1. Detén todo (`Ctrl+C` en cada terminal).
2. Levanta en orden, esperando el `Started` de cada uno: Docker (Kafka, Keycloak, tu BD) → Config → Eureka →
   tu servicio → Gateway → frontend ([S13](S13_Validacion_End_to_End_Producto.md)).
3. Prueba cada link de [Enlaces del sistema](Enlaces_Sistema.md) de arriba hacia abajo: el **primero** que falle es
   donde está el problema.
