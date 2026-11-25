# Guía: crear un servicio desde cero (con o sin CRUD)

*Guía de proyecto - un microservicio nuevo y solo, con su propia infraestructura - 2026*

## 1. Introducción

### 1.1 Qué vas a construir

Un microservicio Spring Boot **nuevo y solo**, sin nada que ver con el proyecto Lamb, que trae su propia
infraestructura en un único `docker-compose.yml` y está conectado a todo:

| Conexión | Para qué sirve |
|---|---|
| **PostgreSQL + Flyway** | Su propia base de datos |
| **Kafka + Kafka UI** | Envía y recibe eventos |
| **Keycloak** | Login en la página y roles (`ADMIN` puede modificar, `USUARIO` solo ver) |
| **Prometheus + Grafana** | Métricas, con un dashboard listo |
| **Loki + Promtail + Grafana** | Logs del servicio |
| **Swagger** | Documentación de la API |
| **Página web** | HTML simple, **sin diseño** |

!!! note "¿Y el Gateway, Eureka y Config Server?"
    Con **un solo servicio** no hacen falta: eso es para cuando hay varios microservicios. Para agregar un servicio
    al proyecto Lamb (con Gateway, Eureka y Config Server) usa la guía de las sesiones, desde la
    [S1](../sesiones/S01_Construccion_Servicio_Base.md). Aquí el servicio valida el token de Keycloak por sí mismo
    y sirve su propia página.

### 1.2 Elige tu camino

Elige **uno** al inicio y síguelo hasta el final:

| Camino | Cuándo elegirlo | Qué obtienes al final |
|---|---|---|
| **A - Sin CRUD** | Te piden "un servicio conectado a todo", sin lógica ni tablas | Página "Hola mundo" + botón que manda un evento a Kafka |
| **B - Con CRUD** | Te piden registrar algo (productos, alumnos, pagos...): agregar, editar, eliminar | Lo del camino A + formulario y tabla para agregar, editar y eliminar |

!!! note "Cómo leer esta guía"
    - Lo que está fuera de los caminos es **común**: hazlo siempre.
    - Si una sección dice **Camino A - Sin CRUD**, hazla solo si elegiste A.
    - Si una sección dice **Camino B - Con CRUD**, hazla solo si elegiste B.
    - Debajo de los archivos, el cuadro **Qué cambia** dice qué datos cambias según lo que te pidan.
      Lo que no se menciona, se copia tal cual.

### 1.3 Elige tus nombres

La guía usa estos nombres de ejemplo. Decide los tuyos al inicio (en VS Code: `Ctrl+Shift+H` para reemplazar en todos los archivos):

| En la guía dice | Lo cambias por (ej. te piden "Tienda" con "Alumnos") | Dónde |
|---|---|---|
| `mi-servicio` | `tienda` | carpeta, `pom.xml`, `application.yml`, `docker-compose.yml`, Prometheus, Promtail, dashboard |
| `mi-servicio-eventos` | `tienda-eventos` | topic de Kafka (`application.yml`) |
| `productos` / `Producto` (camino B) | `alumnos` / `Alumno` | tabla, clases Java, ruta `/api/alumnos`, página |
| `nombre`, `precio` (camino B) | los atributos que te pidan | tabla, entidad, controlador, página |
| `8100` | otro puerto libre (solo si está ocupado) | `application.yml`, `realm-demo.json`, `prometheus.yml` |

!!! note "Esto NO se cambia"
    El paquete `com.ejemplo.miservicio` (cambiarlo es opcional, ver sección 11), los puertos de la infraestructura
    (`25432`, `28180`, `28090`, `29090`, `23000`, `23100`) salvo que estén ocupados, y los archivos de
    observabilidad salvo el nombre del servicio.

### 1.4 Estructura final

```text
mi-proyecto\
├── docker-compose.yml          <- toda la infraestructura
├── keycloak\realm-demo.json    <- realm "demo", usuarios y roles
├── observabilidad\
│   ├── prometheus.yml
│   ├── promtail.yml
│   └── grafana\
│       ├── datasources\datasources.yml
│       └── dashboards\dashboards.yml, mi-servicio.json
└── mi-servicio\                <- el servicio Spring Boot
    ├── pom.xml, mvnw, mvnw.cmd, .mvn\
    └── src\main\
        ├── java\com\ejemplo\miservicio\
        │   ├── MiServicioApplication.java
        │   ├── config\      SecurityConfig, KafkaTopicsConfig, OpenApiConfig
        │   ├── controller\  HolaController, ProductoController (B)
        │   ├── entity\      Producto (B)
        │   ├── repository\  ProductoRepository (B)
        │   ├── event\       MiEvento
        │   └── messaging\   EventosProducer, EventosConsumer
        └── resources\
            ├── application.yml
            ├── logback-spring.xml
            ├── db\migration\   V1__create_productos.sql (B; vacía en el camino A)
            └── static\index.html, js\
```

`(B)` = solo en el camino **con CRUD**.

### 1.5 Verifica tu computadora (antes de todo)

Hazlo **una sola vez** antes de empezar: así evitas errores más adelante.

| Herramienta | Versión | Para qué | Si falta |
|---|---|---|---|
| **Java JDK** | **21** | Compilar y correr los servicios | Instala *Eclipse Temurin 21* (en el instalador activa **Set JAVA_HOME variable**) |
| **JAVA_HOME** | apuntando al JDK 21 | `mvnw` la usa para compilar | Ver "Configurar JAVA_HOME" abajo |
| **Docker Desktop** | con Docker Compose v2+ | BD, Kafka, Keycloak, Grafana... | Instala Docker Desktop (te pedirá activar **WSL 2**: acepta y reinicia) |
| **Node.js** | cualquier LTS | Solo para descargar `keycloak.js` (paso 7.1) | Instala *Node.js LTS* (trae npm) |
| **npm** | viene con Node.js | Descargar `keycloak.js` | — |
| **Git** | cualquiera | Clonar y subir el proyecto | Instala *Git for Windows* |
| **Maven** | no hace falta | `mvnw.cmd` lo descarga solo | — |
| **RAM** | 16 GB recomendado (8 GB mínimo) | Todo corre en tu PC | Con 8 GB levanta solo lo necesario |
| **Internet** | la primera vez | Descargar librerías e imágenes de Docker | — |

**1. Pega esto en PowerShell** (revisa todo de una vez):

```powershell
$script:faltan = 0
function Revisar($nombre, [scriptblock]$comando, $esperado, $solucion) {
    try { $salida = (& $comando 2>&1 | Out-String).Trim() } catch { $salida = "" }
    if ($salida -match $esperado) {
        Write-Host ("[OK]    {0,-18} {1}" -f $nombre, (($salida -split "`n")[0].Trim() -replace '^\S+ : ', ''))
    } else {
        Write-Host ("[FALTA] {0,-18} {1}" -f $nombre, $solucion)
        $script:faltan++
    }
}
Revisar "Java 21"          { java -version }  'version "21\.'  "Instala el JDK 21 (Temurin) y vuelve a abrir la terminal"
Revisar "JAVA_HOME (21)"   { & "$env:JAVA_HOME\bin\java.exe" -version } 'version "21\.' "La variable JAVA_HOME debe apuntar a la carpeta del JDK 21"
Revisar "Docker"           { docker --version } 'Docker version' "Instala Docker Desktop"
Revisar "Docker encendido" { docker info --format '{{.ServerVersion}}' } '^\d' "Abre Docker Desktop y espera 'Engine running'"
Revisar "Docker Compose"   { docker compose version } 'version v\d' "Actualiza Docker Desktop"
Revisar "Node.js"          { node -v } '^v(20\.(19|[2-9]\d)|22\.(1[2-9]|[2-9]\d)|2[4-9]|[3-9]\d)\.' "Instala Node.js 22 o 24 (LTS)"
Revisar "npm"              { npm -v } '^\d+\.' "Viene con Node.js: reinstala Node.js"
Revisar "Git"              { git --version } 'git version' "Instala Git para Windows"
Revisar "Permiso scripts"  { Get-ExecutionPolicy } 'RemoteSigned|Unrestricted|Bypass' "Ejecuta: Set-ExecutionPolicy -Scope CurrentUser RemoteSigned"
$ram = [math]::Round((Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory / 1GB)
if ($ram -ge 16) { Write-Host ("[OK]    {0,-18} {1} GB" -f "Memoria RAM", $ram) }
else { Write-Host ("[AVISO] {0,-18} {1} GB: levanta solo lo necesario (ver Solucion de errores, seccion 11)" -f "Memoria RAM", $ram) }
if ($script:faltan -eq 0) { Write-Host "`nTodo listo." } else { Write-Host "`nFaltan $($script:faltan) cosas: arreglalas antes de seguir." }
```

Debe terminar en **`Todo listo.`** Si alguna línea dice **`[FALTA]`**, haz lo que indica, **cierra y vuelve a abrir
PowerShell** y pégalo otra vez. Así se ve cuando todo está bien:

```text
[OK]    Java 21            openjdk version "21.0.12" 2026-07-21 LTS
[OK]    JAVA_HOME (21)     openjdk version "21.0.12" 2026-07-21 LTS
[OK]    Docker             Docker version 29.7.2, build a7dcaa6
[OK]    Docker encendido   29.7.2
[OK]    Docker Compose     Docker Compose version v5.5.0
[OK]    Node.js            v24.15.0
[OK]    npm                11.12.1
[OK]    Git                git version 2.54.0.windows.1
[OK]    Permiso scripts    RemoteSigned
[OK]    Memoria RAM        16 GB

Todo listo.
```

Tus números pueden ser otros: lo importante es que todo diga `[OK]`.

**2. Revisa que los puertos estén libres:**

```powershell
foreach ($p in 8100, 25432, 28090, 28180, 29090, 29092, 23000, 23100) {
    $c = Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($c) { "[OCUPADO] $p  -> lo usa: $((Get-Process -Id $c.OwningProcess).ProcessName)" } else { "[LIBRE]   $p" }
}
```

Todos deben salir `[LIBRE]`. Si alguno sale `[OCUPADO]`: ciérralo, o cambia el número de la **izquierda** en `ports:` del `docker-compose.yml` (y en `application.yml` si es la BD o el `8100`). Más ayuda en la [sección 12](#12-problemas-comunes).

!!! note "Configurar JAVA_HOME (si salió [FALTA])"
    1. Busca la carpeta del JDK 21: normalmente `C:\Program Files\Eclipse Adoptium\jdk-21...`
    2. Menú inicio → escribe **variables de entorno** → *Editar las variables de entorno del sistema* → *Variables de entorno*.
    3. En *Variables de usuario* → *Nueva*: nombre `JAVA_HOME`, valor la carpeta del paso 1 (sin `\bin`).
    4. En la misma lista edita `Path` → *Nuevo* → `%JAVA_HOME%\bin` → súbelo arriba de todo.
    5. Acepta, **cierra todas las terminales** (y VS Code) y vuelve a abrirlas.

!!! note "Si algo sigue fallando"
    Busca el mensaje en [Solución de errores](../sesiones/Solucion_Errores.md).

**Cómo seguir los pasos:** cada paso dice qué archivo **crear**, **modificar** o **reemplazar**, con su ruta completa desde `mi-proyecto\` (la carpeta que abres en VS Code). Para crear un archivo: clic derecho en la carpeta → *New File*. Para abrir una terminal en `mi-proyecto\`: *Terminal → New Terminal*.

## 2. Crear las carpetas

Aquí **no creas archivos a mano**: el comando crea `mi-proyecto\` y todas sus carpetas (vacías).

**Dónde abrir la terminal:** En la carpeta donde quieras crear el proyecto (ej. `Documentos`). Pega todo el bloque. Al terminar, la terminal queda **dentro** de `mi-proyecto\`. Ábrela en VS Code: *File → Open Folder* → `mi-proyecto`.


```powershell
New-Item -ItemType Directory mi-proyecto | Out-Null
cd mi-proyecto
"keycloak","observabilidad\grafana\datasources","observabilidad\grafana\dashboards",
"mi-servicio\src\main\java\com\ejemplo\miservicio\config",
"mi-servicio\src\main\java\com\ejemplo\miservicio\controller",
"mi-servicio\src\main\java\com\ejemplo\miservicio\event",
"mi-servicio\src\main\java\com\ejemplo\miservicio\messaging",
"mi-servicio\src\main\resources\db\migration",
"mi-servicio\src\main\resources\static\js" | ForEach-Object { New-Item -ItemType Directory -Force $_ | Out-Null }
```

**Camino B - Con CRUD:** crea además (en la **misma** terminal, que ya está en `mi-proyecto\`):

```powershell
"mi-servicio\src\main\java\com\ejemplo\miservicio\entity",
"mi-servicio\src\main\java\com\ejemplo\miservicio\repository" | ForEach-Object { New-Item -ItemType Directory -Force $_ | Out-Null }
```

!!! note "Qué cambia"
    `mi-proyecto` y `mi-servicio` por tus nombres. El resto se pega tal cual.

## 3. Infraestructura (Docker) - los dos caminos

### 3.1 `docker-compose.yml` (en `mi-proyecto\`)

Levanta PostgreSQL, Kafka, Kafka UI, Keycloak, Prometheus, Loki, Promtail y Grafana.

**Crea el archivo:** `docker-compose.yml`  
**En la ruta:** `mi-proyecto\`  
**Cómo:** clic derecho en la carpeta `mi-proyecto` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Ojo:** Va **directo** en `mi-proyecto\` (no dentro de `mi-servicio`).  
**Y pega el siguiente código:**

```yaml
# Infraestructura completa para mi-servicio (independiente de Lamb).
# Levantar:  docker compose up -d        Apagar:  docker compose down
name: mi-servicio

services:
  # ---------- Base de datos ----------
  postgres:
    image: postgres:16-alpine
    container_name: mi-servicio-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: mi_servicio_db
      POSTGRES_USER: app
      POSTGRES_PASSWORD: app
    ports:
      - "25432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  # ---------- Kafka (mensajeria) ----------
  kafka:
    image: apache/kafka:4.1.0
    container_name: mi-servicio-kafka
    restart: unless-stopped
    ports:
      - "29092:29092"
    environment:
      CLUSTER_ID: TWlTZXJ2aWNpb0thZmthMQ
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: INTERNAL://:9092,EXTERNAL://:29092,CONTROLLER://:9093
      KAFKA_ADVERTISED_LISTENERS: INTERNAL://kafka:9092,EXTERNAL://localhost:29092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: INTERNAL:PLAINTEXT,EXTERNAL:PLAINTEXT,CONTROLLER:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0
    healthcheck:
      test: ["CMD-SHELL", "/opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092 > /dev/null 2>&1 || exit 1"]
      interval: 10s
      timeout: 10s
      retries: 10
      start_period: 20s

  kafka-ui:
    image: ghcr.io/kafbat/kafka-ui:v1.5.0
    container_name: mi-servicio-kafka-ui
    restart: unless-stopped
    depends_on:
      kafka:
        condition: service_healthy
    ports:
      - "28090:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: mi-servicio
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092

  # ---------- Keycloak (login y roles) ----------
  keycloak:
    image: quay.io/keycloak/keycloak:26.8.0
    container_name: mi-servicio-keycloak
    restart: unless-stopped
    command: start-dev --import-realm
    environment:
      KC_BOOTSTRAP_ADMIN_USERNAME: admin
      KC_BOOTSTRAP_ADMIN_PASSWORD: admin123
      KC_HEALTH_ENABLED: "true"
      KC_METRICS_ENABLED: "true"
    ports:
      - "28180:8080"
    volumes:
      - ./keycloak:/opt/keycloak/data/import

  # ---------- Observabilidad: Prometheus (metricas) + Loki/Promtail (logs) + Grafana ----------
  prometheus:
    image: prom/prometheus:v3.14.0
    container_name: mi-servicio-prometheus
    restart: unless-stopped
    ports:
      - "29090:9090"
    volumes:
      - ./observabilidad/prometheus.yml:/etc/prometheus/prometheus.yml:ro
    extra_hosts:
      - "host.docker.internal:host-gateway"

  loki:
    image: grafana/loki:3.7.6
    container_name: mi-servicio-loki
    restart: unless-stopped
    ports:
      - "23100:3100"

  promtail:
    image: grafana/promtail:3.6.8
    container_name: mi-servicio-promtail
    restart: unless-stopped
    command: -config.file=/etc/promtail/config.yml
    volumes:
      - ./observabilidad/promtail.yml:/etc/promtail/config.yml:ro
      - ./mi-servicio/logs:/var/log/mi-servicio:ro
    depends_on:
      - loki

  grafana:
    image: grafana/grafana:11.4.0
    container_name: mi-servicio-grafana
    restart: unless-stopped
    ports:
      - "23000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
    volumes:
      - ./observabilidad/grafana:/etc/grafana/provisioning:ro
    depends_on:
      - prometheus
      - loki

volumes:
  postgres_data:
```

**Qué cambiar en este código** (ejemplo: si tu servicio es **tienda** y tu CRUD es **Alumnos** con `dni` y `fechaNacimiento`):

- `mi-servicio` (aparece 13 veces) → el nombre de **tu servicio**. Ej.: `tienda`.
- `mi_servicio` (aparece 1 vez) → el nombre de **tu servicio** con guion bajo (base de datos). Ej.: `tienda`.
- Todo lo demás se copia **igual**. Tip: `Ctrl+H` en VS Code, con **Aa** activado (respeta mayúsculas).

!!! note "Más detalle"
    - `name:` y los `container_name` → tu nombre de servicio.
    - `./mi-servicio/logs` (en Promtail) → la carpeta de tu servicio.
    - Si un puerto está ocupado: cambia solo el número de la **izquierda** en `ports:` (ej. `"25433:5432"`) y
      actualízalo donde se use (`application.yml`).

### 3.2 `keycloak\realm-demo.json` (usuarios y roles)

Keycloak lo importa solo al arrancar. Crea el realm **demo**, los roles `ADMIN` y `USUARIO`,
el cliente `mi-servicio-web` y dos usuarios.

**Crea el archivo:** `realm-demo.json`  
**En la ruta:** `keycloak\`  
**Cómo:** clic derecho en la carpeta `keycloak` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

```json
{
  "realm": "demo",
  "enabled": true,
  "sslRequired": "none",
  "roles": {
    "realm": [
      { "name": "ADMIN", "description": "Puede crear, editar y eliminar" },
      { "name": "USUARIO", "description": "Solo puede ver" }
    ]
  },
  "clients": [
    {
      "clientId": "mi-servicio-web",
      "enabled": true,
      "publicClient": true,
      "protocol": "openid-connect",
      "standardFlowEnabled": true,
      "directAccessGrantsEnabled": true,
      "redirectUris": ["http://localhost:8100/*"],
      "webOrigins": ["http://localhost:8100"],
      "attributes": { "pkce.code.challenge.method": "S256", "post.logout.redirect.uris": "http://localhost:8100/*" }
    }
  ],
  "users": [
    {
      "username": "admin",
      "enabled": true,
      "email": "admin@demo.local",
      "emailVerified": true,
      "firstName": "Admin",
      "lastName": "Demo",
      "credentials": [{ "type": "password", "value": "admin123", "temporary": false }],
      "realmRoles": ["ADMIN", "USUARIO"]
    },
    {
      "username": "usuario",
      "enabled": true,
      "email": "usuario@demo.local",
      "emailVerified": true,
      "firstName": "Usuario",
      "lastName": "Demo",
      "credentials": [{ "type": "password", "value": "usuario123", "temporary": false }],
      "realmRoles": ["USUARIO"]
    }
  ]
}
```

**Qué cambiar en este código** (ejemplo: si tu servicio es **tienda** y tu CRUD es **Alumnos** con `dni` y `fechaNacimiento`):

- `mi-servicio` (aparece 1 vez) → el nombre de **tu servicio**. Ej.: `tienda`.
- `8100` (aparece 3 veces) → el puerto de **tu servicio** (solo si está ocupado).
- Todo lo demás se copia **igual**. Tip: `Ctrl+H` en VS Code, con **Aa** activado (respeta mayúsculas).

| Usuario | Clave | Roles | Puede |
|---|---|---|---|
| `admin` | `admin123` | ADMIN, USUARIO | ver y modificar |
| `usuario` | `usuario123` | USUARIO | solo ver |

!!! note "Qué cambia: si te piden otro rol (ej. DOCENTE)"
    Agrégalo en `"roles" → "realm"` y en el `"realmRoles"` del usuario. Este Keycloak **no guarda datos**
    entre reinicios: para aplicar cambios del JSON haz `docker compose down` y `docker compose up -d`.

### 3.3 `observabilidad\prometheus.yml` (de dónde saca métricas)

**Crea el archivo:** `prometheus.yml`  
**En la ruta:** `observabilidad\`  
**Cómo:** clic derecho en la carpeta `observabilidad` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  # Metricas del servicio Spring Boot (corre en tu PC, puerto 8100)
  - job_name: "mi-servicio"
    metrics_path: "/actuator/prometheus"
    static_configs:
      - targets: ["host.docker.internal:8100"]
        labels:
          application: mi-servicio
```

**Qué cambiar en este código** (ejemplo: si tu servicio es **tienda** y tu CRUD es **Alumnos** con `dni` y `fechaNacimiento`):

- `mi-servicio` (aparece 2 veces) → el nombre de **tu servicio**. Ej.: `tienda`.
- `8100` (aparece 2 veces) → el puerto de **tu servicio** (solo si está ocupado).
- Todo lo demás se copia **igual**. Tip: `Ctrl+H` en VS Code, con **Aa** activado (respeta mayúsculas).

### 3.4 `observabilidad\promtail.yml` (de dónde saca logs)

**Crea el archivo:** `promtail.yml`  
**En la ruta:** `observabilidad\`  
**Cómo:** clic derecho en la carpeta `observabilidad` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

```yaml
server:
  http_listen_port: 9080

positions:
  filename: /tmp/positions.yaml

clients:
  - url: http://loki:3100/loki/api/v1/push

scrape_configs:
  # Lee los logs que escribe el servicio en mi-servicio/logs/*.log
  - job_name: mi-servicio
    static_configs:
      - targets: [localhost]
        labels:
          application: mi-servicio
          __path__: /var/log/mi-servicio/*.log
```

**Qué cambiar en este código** (ejemplo: si tu servicio es **tienda** y tu CRUD es **Alumnos** con `dni` y `fechaNacimiento`):

- `mi-servicio` (aparece 4 veces) → el nombre de **tu servicio**. Ej.: `tienda`.
- Todo lo demás se copia **igual**. Tip: `Ctrl+H` en VS Code, con **Aa** activado (respeta mayúsculas).

!!! note "Qué cambia en 3.3 y 3.4"
    Solo `job_name` y `application` → tu nombre de servicio (y el puerto `8100` si lo cambiaste).

### 3.5 `observabilidad\grafana\datasources\datasources.yml` - copia tal cual

**Crea el archivo:** `datasources.yml`  
**En la ruta:** `observabilidad\grafana\datasources\`  
**Cómo:** clic derecho en la carpeta `datasources` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    uid: prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true

  - name: Loki
    uid: loki
    type: loki
    access: proxy
    url: http://loki:3100
```

**Qué cambiar en este código:** nada, se pega tal cual.

### 3.6 `observabilidad\grafana\dashboards\dashboards.yml` - copia tal cual

**Crea el archivo:** `dashboards.yml`  
**En la ruta:** `observabilidad\grafana\dashboards\`  
**Cómo:** clic derecho en la carpeta `dashboards` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

```yaml
apiVersion: 1

providers:
  - name: mi-servicio
    folder: mi-servicio
    type: file
    options:
      path: /etc/grafana/provisioning/dashboards
```

**Qué cambiar en este código** (ejemplo: si tu servicio es **tienda** y tu CRUD es **Alumnos** con `dni` y `fechaNacimiento`):

- `mi-servicio` (aparece 2 veces) → el nombre de **tu servicio**. Ej.: `tienda`.
- Todo lo demás se copia **igual**. Tip: `Ctrl+H` en VS Code, con **Aa** activado (respeta mayúsculas).

### 3.7 `observabilidad\grafana\dashboards\mi-servicio.json` (el dashboard)

Muestra: si el servicio está arriba, peticiones por segundo y los logs en vivo.

**Crea el archivo:** `mi-servicio.json`  
**En la ruta:** `observabilidad\grafana\dashboards\`  
**Cómo:** clic derecho en la carpeta `dashboards` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

```json
{
  "title": "mi-servicio",
  "uid": "mi-servicio",
  "schemaVersion": 39,
  "time": { "from": "now-30m", "to": "now" },
  "refresh": "10s",
  "panels": [
    {
      "type": "stat",
      "title": "Servicio arriba (1 = si)",
      "gridPos": { "x": 0, "y": 0, "w": 6, "h": 6 },
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [{ "expr": "up{job=\"mi-servicio\"}", "refId": "A" }]
    },
    {
      "type": "timeseries",
      "title": "Peticiones HTTP por segundo",
      "gridPos": { "x": 6, "y": 0, "w": 18, "h": 6 },
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [{ "expr": "sum by (uri, method) (rate(http_server_requests_seconds_count{job=\"mi-servicio\"}[1m]))", "legendFormat": "{{method}} {{uri}}", "refId": "A" }]
    },
    {
      "type": "logs",
      "title": "Logs del servicio",
      "gridPos": { "x": 0, "y": 6, "w": 24, "h": 12 },
      "datasource": { "type": "loki", "uid": "loki" },
      "targets": [{ "expr": "{application=\"mi-servicio\"}", "refId": "A" }]
    }
  ]
}
```

**Qué cambiar en este código** (ejemplo: si tu servicio es **tienda** y tu CRUD es **Alumnos** con `dni` y `fechaNacimiento`):

- `mi-servicio` (aparece 5 veces) → el nombre de **tu servicio**. Ej.: `tienda`.
- Todo lo demás se copia **igual**. Tip: `Ctrl+H` en VS Code, con **Aa** activado (respeta mayúsculas).

!!! note "Más detalle"
    El nombre del archivo y cada `job="mi-servicio"` y `application="mi-servicio"` → tu nombre de servicio.

## 4. Proyecto Spring Boot - los dos caminos

### 4.1 Maven Wrapper (`mvnw`)

Copia `mvnw`, `mvnw.cmd` y la carpeta `.mvn` de **cualquier** proyecto Spring Boot a `mi-servicio\`.
Por ejemplo, desde el proyecto Lamb:

**Dónde abrir la terminal:** En la carpeta **`mi-proyecto\`** (la que abriste en VS Code). Cambia la primera línea por la ruta real de Lamb en tu PC.

```powershell
$lamb = "C:\ruta\a\Lamb-Modulo02-Admision-Trayectoria\services\lamb-trayectoria-ms"   # cambia por tu ruta
Copy-Item -Recurse "$lamb\.mvn" mi-servicio
Copy-Item "$lamb\mvnw", "$lamb\mvnw.cmd" mi-servicio
```

(Si tienes Maven instalado también vale: `cd mi-servicio; mvn -N wrapper:wrapper`).

### 4.2 `mi-servicio\pom.xml`

Trae: web, JPA, PostgreSQL, Flyway, Kafka, seguridad JWT (Keycloak), Swagger, Actuator y Prometheus.
Es **el mismo** para los dos caminos.

**Crea el archivo:** `pom.xml`  
**En la ruta:** `mi-servicio\`  
**Cómo:** clic derecho en la carpeta `mi-servicio` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Ojo:** Va **directo** en `mi-servicio\`, al lado de `mvnw.cmd`.  
**Y pega el siguiente código:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>4.0.8</version>
		<relativePath/>
	</parent>
	<groupId>com.ejemplo</groupId>
	<artifactId>mi-servicio</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name>mi-servicio</name>
	<description>Servicio independiente conectado a Postgres, Kafka, Keycloak y Grafana</description>
	<properties>
		<java.version>21</java.version>
	</properties>
	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-actuator</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-flyway</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-validation</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webmvc</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-kafka</artifactId>
		</dependency>
		<dependency>
			<groupId>org.flywaydb</groupId>
			<artifactId>flyway-database-postgresql</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springdoc</groupId>
			<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
			<version>3.0.2</version>
		</dependency>
		<dependency>
			<groupId>io.micrometer</groupId>
			<artifactId>micrometer-registry-prometheus</artifactId>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>org.postgresql</groupId>
			<artifactId>postgresql</artifactId>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-actuator-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-flyway-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-validation-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webmvc-test</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>
	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>
				<executions>
					<execution>
						<id>default-compile</id>
						<phase>compile</phase>
						<goals><goal>compile</goal></goals>
						<configuration>
							<annotationProcessorPaths>
								<path>
									<groupId>org.projectlombok</groupId>
									<artifactId>lombok</artifactId>
								</path>
							</annotationProcessorPaths>
						</configuration>
					</execution>
					<execution>
						<id>default-testCompile</id>
						<phase>test-compile</phase>
						<goals><goal>testCompile</goal></goals>
						<configuration>
							<annotationProcessorPaths>
								<path>
									<groupId>org.projectlombok</groupId>
									<artifactId>lombok</artifactId>
								</path>
							</annotationProcessorPaths>
						</configuration>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>
</project>
```

**Qué cambiar en este código** (ejemplo: si tu servicio es **tienda** y tu CRUD es **Alumnos** con `dni` y `fechaNacimiento`):

- `mi-servicio` (aparece 2 veces) → el nombre de **tu servicio**. Ej.: `tienda`.
- Todo lo demás se copia **igual**. Tip: `Ctrl+H` en VS Code, con **Aa** activado (respeta mayúsculas).

!!! note "Más detalle"
    Solo `<artifactId>` y `<name>` del proyecto (no los de las dependencias).

### 4.3 `mi-servicio\.gitignore` - copia tal cual

**Crea el archivo:** `.gitignore`  
**En la ruta:** `mi-servicio\`  
**Cómo:** clic derecho en la carpeta `mi-servicio` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

```text
HELP.md
target/
.mvn/wrapper/maven-wrapper.jar
!**/src/main/**/target/
!**/src/test/**/target/

### STS ###
.apt_generated
.classpath
.factorypath
.project
.settings
.springBeans
.sts4-cache

### IntelliJ IDEA ###
.idea
*.iws
*.iml
*.ipr

### NetBeans ###
/nbproject/private/
/nbbuild/
/dist/
/nbdist/
/.nb-gradle/
build/
!**/src/main/**/build/
!**/src/test/**/build/

### VS Code ###
.vscode/
```

**Qué cambiar en este código:** nada, se pega tal cual.

### 4.4 `src\main\resources\application.yml`

**Crea el archivo:** `application.yml`  
**En la ruta:** `mi-servicio\src\main\resources\`  
**Cómo:** clic derecho en la carpeta `resources` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

```yaml
server:
  port: 8100

spring:
  application:
    name: mi-servicio

  # ---------- Base de datos (Postgres del docker-compose) ----------
  datasource:
    url: jdbc:postgresql://localhost:25432/mi_servicio_db
    username: app
    password: app
  flyway:
    enabled: true
    locations: classpath:db/migration
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false

  # ---------- Keycloak: el servicio valida el token JWT ----------
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:28180/realms/demo

  # ---------- Kafka ----------
  kafka:
    bootstrap-servers: localhost:29092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JacksonJsonSerializer
      properties:
        spring.json.add.type.headers: false
    consumer:
      group-id: mi-servicio
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
      properties:
        spring.deserializer.value.delegate.class: org.springframework.kafka.support.serializer.JacksonJsonDeserializer
        spring.json.value.default.type: com.ejemplo.miservicio.event.MiEvento
        spring.json.trusted.packages: "*"
        spring.json.use.type.headers: false

app:
  kafka:
    topic: mi-servicio-eventos
  keycloak:
    url: http://localhost:28180
    realm: demo
    client-id: mi-servicio-web

# ---------- Swagger ----------
springdoc:
  swagger-ui:
    path: /swagger-ui.html

# ---------- Metricas para Prometheus/Grafana ----------
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
```

**Qué cambiar en este código** (ejemplo: si tu servicio es **tienda** y tu CRUD es **Alumnos** con `dni` y `fechaNacimiento`):

- `mi-servicio` (aparece 4 veces) → el nombre de **tu servicio**. Ej.: `tienda`.
- `mi_servicio` (aparece 1 vez) → el nombre de **tu servicio** con guion bajo (base de datos). Ej.: `tienda`.
- `8100` (aparece 1 vez) → el puerto de **tu servicio** (solo si está ocupado).
- `com.ejemplo.miservicio` y `MiServicio` **no hace falta cambiarlos** (es opcional, ver sección 11).
- Todo lo demás se copia **igual**. Tip: `Ctrl+H` en VS Code, con **Aa** activado (respeta mayúsculas).

!!! note "Más detalle"
    `spring.application.name`, `app.kafka.topic` y, si los cambiaste en el `docker-compose.yml`, el puerto,
    la BD, usuario y contraseña de `datasource`. `server.port` solo si `8100` está ocupado.

### 4.5 `src\main\resources\logback-spring.xml` (logs a archivo, para Loki)

Escribe en `mi-servicio\logs\mi-servicio.log`, carpeta que Promtail lee.

**Crea el archivo:** `logback-spring.xml`  
**En la ruta:** `mi-servicio\src\main\resources\`  
**Cómo:** clic derecho en la carpeta `resources` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- Escribe los logs en consola y en logs/mi-servicio.log (Promtail los manda a Loki/Grafana) -->
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <property name="LOG_PATTERN" value="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level %logger{36} - %msg%n"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder><pattern>${LOG_PATTERN}</pattern></encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/mi-servicio.log</file>
        <encoder><pattern>${LOG_PATTERN}</pattern></encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/mi-servicio-%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>7</maxHistory>
        </rollingPolicy>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

**Qué cambiar en este código** (ejemplo: si tu servicio es **tienda** y tu CRUD es **Alumnos** con `dni` y `fechaNacimiento`):

- `mi-servicio` (aparece 3 veces) → el nombre de **tu servicio**. Ej.: `tienda`.
- Todo lo demás se copia **igual**. Tip: `Ctrl+H` en VS Code, con **Aa** activado (respeta mayúsculas).

!!! note "Más detalle"
    Solo el nombre del archivo de log (`mi-servicio.log`).

## 5. Código Java común - los dos caminos

Todos van dentro de `mi-servicio\src\main\java\com\ejemplo\miservicio\`.

### 5.1 `MiServicioApplication.java`

**Crea el archivo:** `MiServicioApplication.java`  
**En la ruta:** `mi-servicio\src\main\java\com\ejemplo\miservicio\`  
**Cómo:** clic derecho en la carpeta `miservicio` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

```java
package com.ejemplo.miservicio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MiServicioApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiServicioApplication.class, args);
    }
}
```

**Qué cambiar en este código:** nada, se pega tal cual.

### 5.2 `config\SecurityConfig.java` (Keycloak y roles)

Deja pasar la página y Swagger; para `/api/**`: **ver** = cualquier usuario logueado,
**modificar** = solo `ADMIN`. Convierte los roles del token de Keycloak (`realm_access.roles`) en roles de Spring.
Ya sirve para el CRUD del camino B: no hay que tocarlo.

**Crea el archivo:** `SecurityConfig.java`  
**En la ruta:** `mi-servicio\src\main\java\com\ejemplo\miservicio\config\`  
**Cómo:** clic derecho en la carpeta `config` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

```java
package com.ejemplo.miservicio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Publico: la pagina web, la config de login, Swagger y metricas
                        .requestMatchers("/", "/index.html", "/favicon.ico", "/js/**", "/api/config",
                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                                "/actuator/**", "/error").permitAll()
                        // Leer: cualquier usuario logueado en Keycloak
                        .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                        // Crear / editar / eliminar / enviar eventos: solo rol ADMIN
                        .requestMatchers("/api/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(rolesDeKeycloak())));
        return http.build();
    }

    // Keycloak manda los roles en realm_access.roles -> los convertimos a ROLE_ADMIN, ROLE_USUARIO...
    @Bean
    public JwtAuthenticationConverter rolesDeKeycloak() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            Object roles = realmAccess == null ? null : realmAccess.get("roles");
            if (!(roles instanceof Collection<?> lista)) {
                return List.<GrantedAuthority>of();
            }
            return lista.stream()
                    .map(rol -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + rol))
                    .toList();
        });
        return converter;
    }
}
```

**Qué cambiar en este código:** nada, se pega tal cual.

!!! note "Qué cambia: quién puede hacer qué"
    Ej. "los docentes también pueden modificar": `.requestMatchers("/api/**").hasAnyRole("ADMIN", "DOCENTE")`
    y crea el rol `DOCENTE` en `realm-demo.json`.

### 5.3 `config\OpenApiConfig.java` (Swagger con botón *Authorize*) - copia tal cual

**Crea el archivo:** `OpenApiConfig.java`  
**En la ruta:** `mi-servicio\src\main\java\com\ejemplo\miservicio\config\`  
**Cómo:** clic derecho en la carpeta `config` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

```java
package com.ejemplo.miservicio.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    // Boton "Authorize" en Swagger para pegar el token de Keycloak
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
```

**Qué cambiar en este código:** nada, se pega tal cual.

### 5.4 `config\KafkaTopicsConfig.java` (crea el topic) - copia tal cual

**Crea el archivo:** `KafkaTopicsConfig.java`  
**En la ruta:** `mi-servicio\src\main\java\com\ejemplo\miservicio\config\`  
**Cómo:** clic derecho en la carpeta `config` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

```java
package com.ejemplo.miservicio.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

// Crea el topic en Kafka al arrancar (si ya existe no hace nada)
@Configuration
public class KafkaTopicsConfig {

    @Bean
    public NewTopic miTopic(@Value("${app.kafka.topic}") String nombre) {
        return TopicBuilder.name(nombre).partitions(3).replicas(1).build();
    }
}
```

**Qué cambiar en este código:** nada, se pega tal cual.

### 5.5 `event\MiEvento.java` (el mensaje que viaja por Kafka)

**Crea el archivo:** `MiEvento.java`  
**En la ruta:** `mi-servicio\src\main\java\com\ejemplo\miservicio\event\`  
**Cómo:** clic derecho en la carpeta `event` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

```java
package com.ejemplo.miservicio.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Mensaje que viaja por Kafka
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiEvento {
    private String tipoEvento;   // ej: "saludo.enviado", "producto.creado"
    private Long id;
    private String mensaje;
    private Long timestamp;
}
```

**Qué cambiar en este código** (ejemplo: si tu servicio es **tienda** y tu CRUD es **Alumnos** con `dni` y `fechaNacimiento`):

- `producto` / `productos` (aparece 1 vez) → el nombre de **tu CRUD**, en minúscula (singular / plural). Ej.: `alumno` / `alumnos`.
- `com.ejemplo.miservicio` y `MiServicio` **no hace falta cambiarlos** (es opcional, ver sección 11).
- Todo lo demás se copia **igual**. Tip: `Ctrl+H` en VS Code, con **Aa** activado (respeta mayúsculas).

!!! note "Más detalle"
    Si te piden mandar más datos, agrega campos (ej. `private String dni;`).

### 5.6 `messaging\EventosProducer.java` (ENVÍA a Kafka) - copia tal cual

**Crea el archivo:** `EventosProducer.java`  
**En la ruta:** `mi-servicio\src\main\java\com\ejemplo\miservicio\messaging\`  
**Cómo:** clic derecho en la carpeta `messaging` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

```java
package com.ejemplo.miservicio.messaging;

import com.ejemplo.miservicio.event.MiEvento;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

// PRODUCTOR: envia mensajes al topic
@Slf4j
@Component
@RequiredArgsConstructor
public class EventosProducer {

    private final KafkaTemplate<String, MiEvento> kafkaTemplate;

    @Value("${app.kafka.topic}")
    private String topic;

    public void publicar(MiEvento evento) {
        kafkaTemplate.send(topic, String.valueOf(evento.getId()), evento)
                .whenComplete((r, ex) -> {
                    if (ex != null) {
                        log.error("kafka producer topic={} evento={} ERROR {}", topic, evento.getTipoEvento(), ex.getMessage());
                    } else {
                        log.info("kafka producer topic={} offset={} evento={} enviado",
                                topic, r.getRecordMetadata().offset(), evento.getTipoEvento());
                    }
                });
    }
}
```

**Qué cambiar en este código:** nada, se pega tal cual.

### 5.7 `messaging\EventosConsumer.java` (RECIBE de Kafka)

**Crea el archivo:** `EventosConsumer.java`  
**En la ruta:** `mi-servicio\src\main\java\com\ejemplo\miservicio\messaging\`  
**Cómo:** clic derecho en la carpeta `messaging` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

```java
package com.ejemplo.miservicio.messaging;

import com.ejemplo.miservicio.event.MiEvento;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// CONSUMIDOR: escucha el topic y reacciona a cada mensaje
@Slf4j
@Component
public class EventosConsumer {

    @KafkaListener(topics = "${app.kafka.topic}")
    public void alRecibir(MiEvento evento) {
        log.info("kafka consumer evento={} id={} mensaje=\"{}\" recibido",
                evento.getTipoEvento(), evento.getId(), evento.getMensaje());
        // Aqui pones lo que deba pasar cuando llega el mensaje
    }
}
```

**Qué cambiar en este código:** nada, se pega tal cual.

!!! note "Qué cambia: reaccionar a un evento"
    Escribe dentro de `alRecibir(...)` lo que debe pasar cuando llega el mensaje (guardar algo, avisar, etc.).

### 5.8 `controller\HolaController.java` (los endpoints del Hola mundo) - copia tal cual

`/api/config` (público, le dice a la página dónde está Keycloak), `/api/hola` y `/api/hola/evento`.
Va en **los dos caminos**.

**Crea el archivo:** `HolaController.java`  
**En la ruta:** `mi-servicio\src\main\java\com\ejemplo\miservicio\controller\`  
**Cómo:** clic derecho en la carpeta `controller` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

```java
package com.ejemplo.miservicio.controller;

import com.ejemplo.miservicio.event.MiEvento;
import com.ejemplo.miservicio.messaging.EventosProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HolaController {

    private final EventosProducer producer;

    @Value("${app.keycloak.url}") private String keycloakUrl;
    @Value("${app.keycloak.realm}") private String realm;
    @Value("${app.keycloak.client-id}") private String clientId;

    // Publico: la pagina web pide aqui los datos para conectarse a Keycloak
    @GetMapping("/config")
    public Map<String, String> config() {
        return Map.of("url", keycloakUrl, "realm", realm, "clientId", clientId);
    }

    // Requiere login: GET http://localhost:8100/api/hola
    @GetMapping("/hola")
    public Map<String, Object> hola(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
                "mensaje", "Hola mundo",
                "usuario", jwt.getClaimAsString("preferred_username"),
                "hora", Instant.now().toString());
    }

    // Solo ADMIN: POST http://localhost:8100/api/hola/evento -> manda un mensaje a Kafka
    @PostMapping("/hola/evento")
    public MiEvento enviarEvento() {
        MiEvento evento = MiEvento.builder()
                .tipoEvento("saludo.enviado")
                .id(System.currentTimeMillis())
                .mensaje("Hola mundo por Kafka")
                .timestamp(Instant.now().toEpochMilli())
                .build();
        producer.publicar(evento);
        return evento;
    }
}
```

**Qué cambiar en este código** (ejemplo: si tu servicio es **tienda** y tu CRUD es **Alumnos** con `dni` y `fechaNacimiento`):

- `8100` (aparece 2 veces) → el puerto de **tu servicio** (solo si está ocupado).
- `com.ejemplo.miservicio` y `MiServicio` **no hace falta cambiarlos** (es opcional, ver sección 11).
- Todo lo demás se copia **igual**. Tip: `Ctrl+H` en VS Code, con **Aa** activado (respeta mayúsculas).

**Si elegiste el camino A, salta a la sección 7.**

## 6. Camino B - Con CRUD: tabla, entidad, repositorio y controlador

**Si quieres agregar el servicio con CRUD (agregar, editar, eliminar), sigue estos pasos.**

### 6.1 Traduce los atributos que te piden

El ejemplo tiene `nombre` (texto) y `precio` (dinero). **Depende del caso que te den, cambia estos atributos**
en la tabla (6.2), la entidad (6.3), el controlador (6.5) y la página (7.3). Usa esta tabla:

| Te piden... | SQL | Java | Input HTML |
|---|---|---|---|
| texto | `VARCHAR(150)` | `String` | `<input id="x">` |
| entero | `INTEGER` | `Integer` | `<input id="x" type="number">` |
| decimal / dinero | `NUMERIC(10,2)` | `BigDecimal` | `<input id="x" type="number" step="0.01">` |
| fecha | `DATE` | `LocalDate` | `<input id="x" type="date">` |
| sí/no | `BOOLEAN` | `Boolean` | `<input id="x" type="checkbox">` (usa `.checked` en vez de `.value`) |

En SQL los nombres de varias palabras van con guion bajo (`fecha_nacimiento`) y en Java en camelCase
(`fechaNacimiento`): Spring los une solo.

### 6.2 `src\main\resources\db\migration\V1__create_productos.sql` (la tabla)

Flyway la ejecuta sola al arrancar el servicio.

**Crea el archivo:** `V1__create_productos.sql`  
**En la ruta:** `mi-servicio\src\main\resources\db\migration\`  
**Cómo:** clic derecho en la carpeta `migration` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Ojo:** El nombre lleva **dos** guiones bajos (`V1__`).  
**Y pega el siguiente código:**

```sql
CREATE TABLE productos (
    id      BIGSERIAL PRIMARY KEY,
    nombre  VARCHAR(150)  NOT NULL,
    precio  NUMERIC(10,2) NOT NULL
);
```

**Qué cambiar en este código** (ejemplo: si tu servicio es **tienda** y tu CRUD es **Alumnos** con `dni` y `fechaNacimiento`):

- `producto` / `productos` (aparece 1 vez) → el nombre de **tu CRUD**, en minúscula (singular / plural). Ej.: `alumno` / `alumnos`.
- `nombre`, `precio` (aparece 2 veces) → **tus atributos** (uno por cada atributo que te pidan). Ej.: `dni`, `fechaNacimiento`. El tipo de cada uno está en la [tabla 6.1](#61-traduce-los-atributos-que-te-piden).
- Todo lo demás se copia **igual**. Tip: `Ctrl+H` en VS Code, con **Aa** activado (respeta mayúsculas).

!!! note "Qué cambia: ejemplo, te piden “Alumnos” con nombre, dni y fecha de nacimiento"
    Archivo `V1__create_alumnos.sql`:
    ```sql
    CREATE TABLE alumnos (
        id               BIGSERIAL PRIMARY KEY,
        nombre           VARCHAR(150) NOT NULL,
        dni              VARCHAR(8)   NOT NULL,
        fecha_nacimiento DATE
    );
    ```
    No cambies: `id BIGSERIAL PRIMARY KEY`.

!!! note "Nunca edites una migración ya aplicada"
    Si ya corriste el servicio y quieres cambiar la tabla, crea un archivo nuevo `V2__...sql`
    (ej. `ALTER TABLE productos ADD COLUMN stock INTEGER;`). Si editas el `V1`, Flyway falla con
    `Migration checksum mismatch`.

### 6.3 `entity\Producto.java` (la tabla en Java)

Un atributo por cada columna de la tabla.

**Crea el archivo:** `Producto.java`  
**En la ruta:** `mi-servicio\src\main\java\com\ejemplo\miservicio\entity\`  
**Cómo:** clic derecho en la carpeta `entity` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

```java
package com.ejemplo.miservicio.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;
}
```

**Qué cambiar en este código** (ejemplo: si tu servicio es **tienda** y tu CRUD es **Alumnos** con `dni` y `fechaNacimiento`):

- `producto` / `productos` (aparece 1 vez) → el nombre de **tu CRUD**, en minúscula (singular / plural). Ej.: `alumno` / `alumnos`.
- `Producto` (aparece 1 vez) → el nombre de **tu CRUD**, con mayúscula. Ej.: `Alumno`.
- `nombre`, `precio` (aparece 2 veces) → **tus atributos** (uno por cada atributo que te pidan). Ej.: `dni`, `fechaNacimiento`. El tipo de cada uno está en la [tabla 6.1](#61-traduce-los-atributos-que-te-piden).
- `com.ejemplo.miservicio` y `MiServicio` **no hace falta cambiarlos** (es opcional, ver sección 11).
- Todo lo demás se copia **igual**. Tip: `Ctrl+H` en VS Code, con **Aa** activado (respeta mayúsculas).

!!! note "Qué cambia: ejemplo para “Alumnos”"
    Archivo `Alumno.java`:
    ```java
    @Entity
    @Table(name = "alumnos")
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public class Alumno {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        private String nombre;
        private String dni;
        private LocalDate fechaNacimiento;   // columna fecha_nacimiento
    }
    ```
    No cambies: las anotaciones de la clase y el bloque del `id`.

### 6.4 `repository\ProductoRepository.java`

Con solo esto ya tienes `findAll`, `findById`, `save` y `delete`.

**Crea el archivo:** `ProductoRepository.java`  
**En la ruta:** `mi-servicio\src\main\java\com\ejemplo\miservicio\repository\`  
**Cómo:** clic derecho en la carpeta `repository` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

```java
package com.ejemplo.miservicio.repository;

import com.ejemplo.miservicio.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
}
```

**Qué cambiar en este código** (ejemplo: si tu servicio es **tienda** y tu CRUD es **Alumnos** con `dni` y `fechaNacimiento`):

- `Producto` (aparece 3 veces) → el nombre de **tu CRUD**, con mayúscula. Ej.: `Alumno`.
- `com.ejemplo.miservicio` y `MiServicio` **no hace falta cambiarlos** (es opcional, ver sección 11).
- Todo lo demás se copia **igual**. Tip: `Ctrl+H` en VS Code, con **Aa** activado (respeta mayúsculas).

!!! note "Más detalle"
    Solo el nombre: `public interface AlumnoRepository extends JpaRepository<Alumno, Long> {}`

### 6.5 `controller\ProductoController.java` (el CRUD)

| Método | Ruta | Hace |
|---|---|---|
| `GET` | `/api/productos` | Lista todos |
| `POST` | `/api/productos` | Agrega (responde `201`) |
| `PUT` | `/api/productos/{id}` | Edita (`404` si no existe) |
| `DELETE` | `/api/productos/{id}` | Elimina (`204`; `404` si no existe) |

Cada cambio se avisa por Kafka (`producto.creado`, `producto.editado`, `producto.eliminado`).

**Crea el archivo:** `ProductoController.java`  
**En la ruta:** `mi-servicio\src\main\java\com\ejemplo\miservicio\controller\`  
**Cómo:** clic derecho en la carpeta `controller` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Ojo:** Va al lado de `HolaController.java`.  
**Y pega el siguiente código:**

```java
package com.ejemplo.miservicio.controller;

import com.ejemplo.miservicio.entity.Producto;
import com.ejemplo.miservicio.event.MiEvento;
import com.ejemplo.miservicio.messaging.EventosProducer;
import com.ejemplo.miservicio.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

// CRUD simple: listar, agregar, editar, eliminar
@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoRepository repo;
    private final EventosProducer producer;

    @GetMapping
    public List<Producto> listar() {
        return repo.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Producto agregar(@RequestBody Producto p) {
        p.setId(null);
        Producto guardado = repo.save(p);
        avisar("producto.creado", guardado);
        return guardado;
    }

    @PutMapping("/{id}")
    public Producto editar(@PathVariable Long id, @RequestBody Producto p) {
        Producto actual = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el producto " + id));
        actual.setNombre(p.getNombre());
        actual.setPrecio(p.getPrecio());
        Producto guardado = repo.save(actual);
        avisar("producto.editado", guardado);
        return guardado;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        Producto actual = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el producto " + id));
        repo.delete(actual);
        avisar("producto.eliminado", actual);
    }

    // Cada cambio se avisa por Kafka
    private void avisar(String tipo, Producto p) {
        producer.publicar(MiEvento.builder()
                .tipoEvento(tipo)
                .id(p.getId())
                .mensaje(p.getNombre())
                .timestamp(Instant.now().toEpochMilli())
                .build());
    }
}
```

**Qué cambiar en este código** (ejemplo: si tu servicio es **tienda** y tu CRUD es **Alumnos** con `dni` y `fechaNacimiento`):

- `producto` / `productos` (aparece 6 veces) → el nombre de **tu CRUD**, en minúscula (singular / plural). Ej.: `alumno` / `alumnos`.
- `Producto` (aparece 14 veces) → el nombre de **tu CRUD**, con mayúscula. Ej.: `Alumno`.
- `com.ejemplo.miservicio` y `MiServicio` **no hace falta cambiarlos** (es opcional, ver sección 11).
- Todo lo demás se copia **igual**. Tip: `Ctrl+H` en VS Code, con **Aa** activado (respeta mayúsculas).

!!! note "Más detalle"
    `Producto` → tu entidad, la ruta `/api/productos` → `/api/alumnos`, los textos de los eventos
    (`producto.creado` → `alumno.creado`) y, en `editar(...)`, **un `actual.setX(p.getX());` por cada atributo**:
    ```java
    actual.setNombre(p.getNombre());
    actual.setDni(p.getDni());
    actual.setFechaNacimiento(p.getFechaNacimiento());
    ```

## 7. Página web (sin diseño)

### 7.1 Descargar `keycloak.js` - los dos caminos

La página usa la librería oficial de Keycloak. En PowerShell, desde `mi-proyecto\`:

**Dónde abrir la terminal:** En la carpeta **`mi-proyecto\`** (la que abriste en VS Code). El bloque entra a `static`, descarga la librería y vuelve a `mi-proyecto\`.

```powershell
cd mi-servicio\src\main\resources\static
npm pack keycloak-js@26.2.5
tar -xzf keycloak-js-26.2.5.tgz
Copy-Item package\lib\keycloak.js, package\lib\deprecations.js js\
Remove-Item -Recurse package, keycloak-js-26.2.5.tgz
cd ..\..\..\..\..
```

Deben quedar `static\js\keycloak.js` y `static\js\deprecations.js`.

### 7.2 Camino A - Sin CRUD: `src\main\resources\static\index.html`

**Si elegiste el servicio sin CRUD (solo Hola mundo), usa esta página.** Pide login a Keycloak, muestra
"Hola mundo" y tiene un botón para mandar un evento a Kafka.

**Crea el archivo:** `index.html`  
**En la ruta:** `mi-servicio\src\main\resources\static\`  
**Cómo:** clic derecho en la carpeta `static` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Ojo:** Camino A. Va en `static` (no dentro de `js`).  
**Y pega el siguiente código:**

```html
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <title>mi-servicio</title>
</head>
<body>
  <h1 id="mensaje">Cargando...</h1>
  <p id="info"></p>
  <button id="evento">Enviar evento a Kafka</button>
  <button id="salir">Cerrar sesion</button>
  <p id="resultado"></p>

  <script type="module">
    import Keycloak from './js/keycloak.js';

    // 1. Pedir al backend los datos de Keycloak y hacer login
    const cfg = await (await fetch('/api/config')).json();
    const keycloak = new Keycloak({ url: cfg.url, realm: cfg.realm, clientId: cfg.clientId });
    await keycloak.init({ onLoad: 'login-required', pkceMethod: 'S256' });

    // 2. Funcion para llamar al backend con el token
    async function api(metodo, url) {
      await keycloak.updateToken(30);
      const r = await fetch(url, { method: metodo, headers: { Authorization: 'Bearer ' + keycloak.token } });
      if (!r.ok) throw new Error('HTTP ' + r.status);
      return r.json();
    }

    // 3. Mostrar el Hola mundo
    const hola = await api('GET', '/api/hola');
    document.getElementById('mensaje').textContent = hola.mensaje;
    document.getElementById('info').textContent = 'Usuario: ' + hola.usuario + ' - Hora del servidor: ' + hola.hora;

    document.getElementById('evento').onclick = async () => {
      try {
        const ev = await api('POST', '/api/hola/evento');
        document.getElementById('resultado').textContent = 'Evento enviado a Kafka: ' + ev.tipoEvento;
      } catch (e) {
        document.getElementById('resultado').textContent = 'Error: ' + e.message + ' (solo ADMIN puede enviar)';
      }
    };
    document.getElementById('salir').onclick = () => keycloak.logout();
  </script>
</body>
</html>
```

**Qué cambiar en este código** (ejemplo: si tu servicio es **tienda** y tu CRUD es **Alumnos** con `dni` y `fechaNacimiento`):

- `mi-servicio` (aparece 1 vez) → el nombre de **tu servicio**. Ej.: `tienda`.
- Todo lo demás se copia **igual**. Tip: `Ctrl+H` en VS Code, con **Aa** activado (respeta mayúsculas).

### 7.3 Camino B - Con CRUD: `src\main\resources\static\index.html`

**Si elegiste el servicio con CRUD, usa esta página en lugar de la 7.2.** "Hola mundo", un formulario y una
tabla con botones *Editar* / *Eliminar*.

**Crea el archivo:** `index.html`  
**En la ruta:** `mi-servicio\src\main\resources\static\`  
**Cómo:** clic derecho en la carpeta `static` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Ojo:** Camino B (si ya creaste la del camino A, **reemplaza** todo su contenido).  
**Y pega el siguiente código:**

```html
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <title>mi-servicio</title>
</head>
<body>
  <h1 id="mensaje">Cargando...</h1>
  <p id="info"></p>
  <button id="salir">Cerrar sesion</button>

  <h2>Productos</h2>
  <p>
    Nombre: <input id="nombre">
    Precio: <input id="precio" type="number" step="0.01">
    <button id="guardar">Agregar</button>
    <button id="cancelar" hidden>Cancelar</button>
  </p>
  <p id="error" style="color:red"></p>

  <table border="1" cellpadding="4">
    <thead><tr><th>ID</th><th>Nombre</th><th>Precio</th><th></th></tr></thead>
    <tbody id="filas"></tbody>
  </table>

  <script type="module">
    import Keycloak from './js/keycloak.js';

    // 1. Login con Keycloak
    const cfg = await (await fetch('/api/config')).json();
    const keycloak = new Keycloak({ url: cfg.url, realm: cfg.realm, clientId: cfg.clientId });
    await keycloak.init({ onLoad: 'login-required', pkceMethod: 'S256' });

    // 2. Llamar al backend con el token
    async function api(metodo, url, body) {
      await keycloak.updateToken(30);
      const r = await fetch(url, {
        method: metodo,
        headers: { Authorization: 'Bearer ' + keycloak.token, 'Content-Type': 'application/json' },
        body: body ? JSON.stringify(body) : undefined
      });
      if (!r.ok) throw new Error('HTTP ' + r.status + (r.status === 403 ? ' (solo ADMIN puede modificar)' : ''));
      return r.status === 204 ? null : r.json();
    }

    const $ = id => document.getElementById(id);
    let editandoId = null;

    // 3. Hola mundo
    const hola = await api('GET', '/api/hola');
    $('mensaje').textContent = hola.mensaje;
    $('info').textContent = 'Usuario: ' + hola.usuario;
    $('salir').onclick = () => keycloak.logout();

    // 4. CRUD
    async function cargar() {
      const productos = await api('GET', '/api/productos');
      $('filas').innerHTML = '';
      for (const p of productos) {
        const tr = document.createElement('tr');
        tr.innerHTML = `<td>${p.id}</td><td></td><td>${p.precio}</td>
          <td><button class="editar">Editar</button> <button class="eliminar">Eliminar</button></td>`;
        tr.children[1].textContent = p.nombre;
        tr.querySelector('.editar').onclick = () => {
          editandoId = p.id;
          $('nombre').value = p.nombre;
          $('precio').value = p.precio;
          $('guardar').textContent = 'Actualizar';
          $('cancelar').hidden = false;
        };
        tr.querySelector('.eliminar').onclick = () => accion(() => api('DELETE', '/api/productos/' + p.id));
        $('filas').appendChild(tr);
      }
    }

    function limpiar() {
      editandoId = null;
      $('nombre').value = '';
      $('precio').value = '';
      $('guardar').textContent = 'Agregar';
      $('cancelar').hidden = true;
    }

    async function accion(fn) {
      try { $('error').textContent = ''; await fn(); limpiar(); await cargar(); }
      catch (e) { $('error').textContent = 'Error: ' + e.message; }
    }

    $('guardar').onclick = () => accion(() => {
      const datos = { nombre: $('nombre').value, precio: $('precio').value };
      return editandoId
        ? api('PUT', '/api/productos/' + editandoId, datos)
        : api('POST', '/api/productos', datos);
    });
    $('cancelar').onclick = limpiar;

    cargar();
  </script>
</body>
</html>
```

**Qué cambiar en este código** (ejemplo: si tu servicio es **tienda** y tu CRUD es **Alumnos** con `dni` y `fechaNacimiento`):

- `mi-servicio` (aparece 1 vez) → el nombre de **tu servicio**. Ej.: `tienda`.
- `producto` / `productos` (aparece 6 veces) → el nombre de **tu CRUD**, en minúscula (singular / plural). Ej.: `alumno` / `alumnos`.
- `Producto` (aparece 1 vez) → el nombre de **tu CRUD**, con mayúscula. Ej.: `Alumno`.
- `nombre`, `precio` (aparece 18 veces) → **tus atributos** (uno por cada atributo que te pidan). Ej.: `dni`, `fechaNacimiento`. El tipo de cada uno está en la [tabla 6.1](#61-traduce-los-atributos-que-te-piden).
- Todo lo demás se copia **igual**. Tip: `Ctrl+H` en VS Code, con **Aa** activado (respeta mayúsculas).

!!! note "Más detalle"
    `/api/productos` → tu ruta, un `<input>` por atributo (tipo según la tabla 6.1), las columnas `<th>`,
    el `tr.innerHTML`, lo que se copia al formulario al editar y el objeto `datos`:
    ```js
    const datos = { nombre: $('nombre').value, dni: $('dni').value, fechaNacimiento: $('fechaNacimiento').value };
    ```

## 8. Ejecutar - los dos caminos

**Dónde abrir la terminal:** En la carpeta **`mi-proyecto\`** (la que abriste en VS Code). La línea `cd mi-servicio` entra al servicio. Deja la terminal abierta: el servicio se queda corriendo ahí.

```powershell
# 1. Infraestructura (desde mi-proyecto\; la primera vez descarga imágenes)
docker compose up -d

# 2. Espera ~30 s a que Keycloak arranque y corre el servicio DESDE su carpeta
cd mi-servicio
.\mvnw.cmd spring-boot:run
```

Cuando veas `Started MiServicioApplication`, abre http://localhost:8100 y entra con `admin` / `admin123`.

- **Camino A:** ves "Hola mundo" y el botón *Enviar evento*.
- **Camino B:** además, el formulario y la tabla: agrega, edita y elimina.

**Para apagar:** `Ctrl+C` en el servicio y, en `mi-proyecto\`: `docker compose down`.

## 9. Comprobar que todo está conectado

| Qué | Link | Qué debes ver |
|---|---|---|
| **Página web** | http://localhost:8100 | Login de Keycloak y luego "Hola mundo" (y en B, el CRUD) |
| Swagger | http://localhost:8100/swagger-ui.html | La API (botón *Authorize*: pega un token) |
| Salud | http://localhost:8100/actuator/health | `"status":"UP"` |
| Métricas crudas | http://localhost:8100/actuator/prometheus | Texto con métricas |
| Keycloak | http://localhost:28180 | `admin` / `admin123` → realm **demo** |
| Kafka UI | http://localhost:28090 | Topic `mi-servicio-eventos` → *Messages* |
| Prometheus | http://localhost:29090/targets | `mi-servicio` en verde (UP) |
| **Grafana** | http://localhost:23000 | `admin` / `admin` → Dashboards → *mi-servicio* |
| Loki | http://localhost:23100/ready | `ready` |
| PostgreSQL | `localhost:25432` | BD `mi_servicio_db`, `app` / `app` |

**Probar con PowerShell** (primero saca un token de Keycloak):

```powershell
$t = (Invoke-RestMethod -Method Post http://localhost:28180/realms/demo/protocol/openid-connect/token `
  -Body @{ grant_type="password"; client_id="mi-servicio-web"; username="admin"; password="admin123" }).access_token
$h = @{ Authorization = "Bearer $t" }
```

**Camino A - Sin CRUD:**

```powershell
Invoke-RestMethod http://localhost:8100/api/hola -Headers $h                       # Hola mundo
Invoke-RestMethod -Method Post http://localhost:8100/api/hola/evento -Headers $h   # evento a Kafka
```

**Camino B - Con CRUD:**

```powershell
$p = "http://localhost:8100/api/productos"
Invoke-RestMethod -Method Post $p -Headers $h -ContentType "application/json; charset=utf-8" -Body '{"nombre":"Lapicero","precio":2.50}'
Invoke-RestMethod $p -Headers $h                 # mira el id (abajo se usa 1)
Invoke-RestMethod -Method Put "$p/1" -Headers $h -ContentType "application/json; charset=utf-8" -Body '{"nombre":"Lapicero azul","precio":3}'
Invoke-RestMethod -Method Delete "$p/1" -Headers $h
```

- Sin token → `401`. Con el usuario `usuario` en un `POST`/`PUT`/`DELETE` → `403`.
- Cada operación deja dos líneas en la consola del servicio: `kafka producer ... enviado` y `kafka consumer ... recibido`.

## 10. Ver la base de datos en VS Code

Con la extensión **Database Client** (ícono de base de datos en la barra izquierda de VS Code) ves tu BD,
sus tablas y sus datos sin usar comandos.

**1. Instalar la extensión (solo la primera vez):** `Ctrl+Shift+X` → busca **Database Client** (autor *Weijan Chen*) → *Install*.

**2. Crear la conexión:** clic en el ícono de base de datos → botón **+** (*Create Connection*) → en *Server Type*
elige **PostgreSQL** y llena:

| Campo | Valor |
|---|---|
| Host | `127.0.0.1` |
| Port | `25432` |
| Username | `app` |
| Password | `app` |
| Database | `mi_servicio_db` |

Clic en **Connect** (o *Save*). La conexión queda guardada para la próxima vez.

!!! note "Qué cambia"
    Port, Database, Username y Password son **los de tu `docker-compose.yml`** (`ports:` el número de la izquierda,
    `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`). Host siempre es `127.0.0.1`.

**3. Ver las tablas:** despliega la conexión → `mi_servicio_db` → `public` → *Tables*. Aparecen cuando el servicio arrancó una vez (Flyway las crea al arrancar). En el **camino A** solo verás `flyway_schema_history`.

| Tabla | Qué es |
|---|---|
| `productos` | **Camino B:** tu tabla. Clic en ella para ver los registros (se llenan al agregar desde la página o la API) |
| `flyway_schema_history` | La crea Flyway: lista de migraciones aplicadas (`V1`, `V2`...). **No la modifiques** |

**4. Hacer una consulta:** clic derecho en la conexión → *New Query* (o el ícono de consulta), escribe y ejecuta (`Ctrl+Enter`):

```sql
SELECT * FROM productos;
SELECT * FROM flyway_schema_history;
```

Si agregas registros desde la página, clic derecho en la tabla → *Refresh* para ver los cambios.

!!! note "No cambies la tabla desde aquí"
    Ver datos y hacer `SELECT` está bien, pero los cambios a la **estructura** (agregar o quitar columnas) se hacen
    con una migración `V2__...sql`. Si los haces a mano, la entidad y Flyway dejan de coincidir y el servicio no arranca.

!!! note "Si no conecta"
    `Connection refused`: la BD no está levantada → `docker ps` y levántala. `password authentication failed`:
    revisa usuario y contraseña con los del `docker-compose.yml`.

## 11. Cambiar el nombre del servicio

Ej. `mi-servicio` → `tienda`:

| Archivo | Qué cambiar |
|---|---|
| `pom.xml` | `<artifactId>` y `<name>` |
| `application.yml` | `spring.application.name`, `app.kafka.topic` |
| `docker-compose.yml` | `name:`, los `container_name` y la carpeta `./mi-servicio/logs` |
| `prometheus.yml` y `promtail.yml` | `job_name` y `application` |
| `mi-servicio.json` | `job="mi-servicio"` y `application="mi-servicio"` |
| `logback-spring.xml` | el nombre del archivo de log |

Cambiar el paquete `com.ejemplo.miservicio` es opcional; si lo cambias, mueve las carpetas, cambia la línea
`package` de cada `.java` y también `spring.json.value.default.type` en `application.yml`.

## 12. Problemas comunes

| Síntoma | Solución |
|---|---|
| La página se queda en "Cargando..." o la API da `401`/`500` al principio | Keycloak aún no arranca: espera 30 s y recarga |
| `Port 8100 was already in use` | Cambia `server.port` en `application.yml`, y `8100` en `realm-demo.json` y `prometheus.yml` |
| `port is already allocated` al hacer `docker compose up` | Cambia el número de la izquierda en `ports:` del `docker-compose.yml` (y en `application.yml` si es la BD) |
| `container name ... already in use` | Otra copia sigue levantada: `docker compose down` en su carpeta |
| No salen logs en Grafana | Arranca el servicio **desde la carpeta `mi-servicio`** (escribe en `mi-servicio\logs`) |
| La página da error al importar `keycloak.js` | Falta el paso 7.1 (`static\js\keycloak.js` y `deprecations.js`) |
| **B:** `Schema-validation: missing table [productos]` | Falta `V1__create_productos.sql` o está en otra carpeta |
| **B:** `Schema-validation: wrong column type` | El tipo Java no coincide con el SQL (tabla 6.1) |
| **B:** `Migration checksum mismatch` | Editaste una migración ya aplicada: crea `V2__...sql` (o borra la BD de prueba con `docker compose down -v`) |
| **B:** `Error: HTTP 403` en la página | Entraste con `usuario`: solo `admin` puede modificar |
| **B:** La tabla no se actualiza | Abre `F12` → *Console* y revisa el error |
