# S8 - Mensajería asíncrona entre servicios

*Guía única: agregar un microservicio nuevo al proyecto Lamb - Parte 8 de 16 - 2026*

!!! note "Antes de empezar: debe estar levantado"
    Desde la raíz del proyecto (`Lamb-Modulo02-Admision-Trayectoria`). Si ya está corriendo, no lo arranques otra vez.

    **Docker** (Docker Desktop abierto; se pueden pegar en la misma terminal):

    ```powershell
    docker compose -f kafka/compose-dev.yml up -d    # Kafka + Kafka UI
    docker compose -f services/lamb-evaluacionn-ms/compose-dev.yml up -d    # BD de tu servicio
    ```

    **Servicios** (cada uno en **su propia terminal**, en este orden, esperando que diga `Started ...`):

    ```powershell
    cd infra\lamb-config; .\mvnw.cmd spring-boot:run    # Config Server
    cd infra\lamb-eureka; .\mvnw.cmd spring-boot:run    # Eureka
    ```

    Comprueba que responden:

    - Kafka + Kafka UI: http://localhost:18090
    - BD de tu servicio: `docker ps` → `lamb-postgres-evaluacionn-dev` *Up*
    - Config Server: http://localhost:18888/lamb-evaluacionn-ms/dev
    - Eureka: http://localhost:18761

    Tu servicio se arranca **en esta sesión**, después de agregar el código de Kafka.

## 1. Introducción

Tu servicio va a **publicar eventos** en Kafka (`localhost:18092`) y también **escucharlos**, igual que
`orden-ms` y `pago-ms` en Pagatu:

| Camino | Cuándo publica | Tipo de evento |
|---|---|---|
| A - Sin CRUD | al pulsar "Enviar evento" | `saludo.enviado` |
| B - Con CRUD | cada vez que se agrega, edita o elimina | `examen.creado`, `examen.actualizado`, `examen.eliminado` (y también `saludo.enviado`) |

La configuración de Kafka ya quedó lista en la S2 (`spring.kafka...` y `app.kafka.topic.evaluacionn`).

Crea las carpetas `event` y `messaging` dentro de `services\lamb-evaluacionn-ms\src\main\java\pe\edu\upeu\lamb\evaluacionn\`:

**Dónde abrir la terminal:** En la **raíz del proyecto** (`Lamb-Modulo02-Admision-Trayectoria`). En VS Code: *Terminal → New Terminal*.

```powershell
$java = "services\lamb-evaluacionn-ms\src\main\java\pe\edu\upeu\lamb\evaluacionn"   # cambia por tu nombre
"event","messaging" | ForEach-Object { New-Item -ItemType Directory -Force "$java\$_" | Out-Null }
```

## 2. Código común (los dos caminos)

### 2.1 `config\KafkaTopicsConfig.java` (crea el topic al arrancar)

**Crea el archivo:** `KafkaTopicsConfig.java`  
**En la ruta:** `services\lamb-evaluacionn-ms\src\main\java\pe\edu\upeu\lamb\evaluacionn\config\`  
**Cómo:** clic derecho en la carpeta `config` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Ojo:** Va en `config`, al lado de `SecurityConfig.java`.  
**Y pega el siguiente código:**

{{lamb:services/lamb-biblioteca-ms/src/main/java/pe/edu/upeu/lamb/biblioteca/config/KafkaTopicsConfig.java}}

!!! note "Qué cambia"
    El nombre del método y la clave `app.kafka.topic.evaluacionn` (debe ser la misma del `-dev.yml` de la S2).

### 2.2 `event\EvaluacionnEvento.java` (el mensaje)

Mismo formato que `OrdenCreadaEvento` / `PagoValidadoEvento` de Pagatu.

**Crea el archivo:** `EvaluacionnEvento.java`  
**En la ruta:** `services\lamb-evaluacionn-ms\src\main\java\pe\edu\upeu\lamb\evaluacionn\event\`  
**Cómo:** clic derecho en la carpeta `event` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

{{lamb:services/lamb-biblioteca-ms/src/main/java/pe/edu/upeu/lamb/biblioteca/event/BibliotecaEvento.java}}

!!! note "Qué cambia"
    El nombre de la clase (`TuServicioEvento`). Si te piden mandar más datos, agrega atributos
    (ej. `private String dni;`). Deja `tipoEvento`, `id`, `origen` y `timestamp`.

!!! note "Si cambias el nombre o el paquete de la clase"
    Actualiza `spring.json.value.default.type` y `spring.json.trusted.packages` en el `-dev.yml` (S2),
    o el consumidor no podrá leer los mensajes.

### 2.3 `messaging\EvaluacionnEventosProducer.java` (ENVÍA) - copia tal cual

Igual que `PagoEventosProducer`. `publicarTrasCommit` se explica en la [S9](S09_Consistencia_Distribuida_Procesos_Negocio.md).

**Crea el archivo:** `EvaluacionnEventosProducer.java`  
**En la ruta:** `services\lamb-evaluacionn-ms\src\main\java\pe\edu\upeu\lamb\evaluacionn\messaging\`  
**Cómo:** clic derecho en la carpeta `messaging` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

{{lamb:services/lamb-biblioteca-ms/src/main/java/pe/edu/upeu/lamb/biblioteca/messaging/BibliotecaEventosProducer.java}}

### 2.4 `messaging\EvaluacionnEventosConsumer.java` (RECIBE)

**Crea el archivo:** `EvaluacionnEventosConsumer.java`  
**En la ruta:** `services\lamb-evaluacionn-ms\src\main\java\pe\edu\upeu\lamb\evaluacionn\messaging\`  
**Cómo:** clic derecho en la carpeta `messaging` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

{{lamb:services/lamb-biblioteca-ms/src/main/java/pe/edu/upeu/lamb/biblioteca/messaging/BibliotecaEventosConsumer.java}}

!!! note "Qué cambia"
    Solo los nombres. Si te piden **hacer algo** al recibir el evento (guardar un historial, avisar...), va
    dentro de `alRecibirEvento(...)`. Para escuchar el topic de **otro** servicio (ej. `admision-eventos`), mira
    `AdmisionEventosConsumer` de Trayectoria o `OrdenEventosConsumer` de `pagatu-pago-ms`.

## 3. Camino A - Sin CRUD: el botón de evento

**Si elegiste el servicio sin CRUD (solo Hola mundo), reemplaza `controller\SaludoController.java` por esta versión**
(agrega `POST /api/v1/evaluacionn/saludo/evento`):

**Abre el archivo:** `SaludoController.java`  
**En la ruta:** `services\lamb-evaluacionn-ms\src\main\java\pe\edu\upeu\lamb\evaluacionn\controller\`  
**Borra todo su contenido y pega el siguiente código:**

{{lamb:services/lamb-biblioteca-ms/src/main/java/pe/edu/upeu/lamb/biblioteca/controller/SaludoController.java}}

## 4. Camino B - Con CRUD: cada cambio avisa a Kafka

**Si elegiste el servicio con CRUD:**

1. Reemplaza `controller\SaludoController.java` por la versión de la sección 3 (también va en el camino B).
2. Reemplaza `service\ExamenService.java` por esta versión, que publica un evento en cada `crear`, `actualizar` y `eliminar`:

**Abre el archivo:** `ExamenService.java`  
**En la ruta:** `services\lamb-evaluacionn-ms\src\main\java\pe\edu\upeu\lamb\evaluacionn\service\`  
**Borra todo su contenido y pega el siguiente código:**

{{lamb:services/lamb-biblioteca-ms/src/main/java/pe/edu/upeu/lamb/biblioteca/service/LibroService.java}}

!!! note "Qué cambia respecto a la S1"
    Se agregaron: `producer`, `nombreServicio`, el método `publicar(...)` y una llamada `publicar("examen.xxx", examen)`
    en cada método que modifica. Cambia `"examen.creado"` etc. por tu recurso (`"pago.creado"`) y en
    `.mensaje(examen.getTitulo())` usa un atributo que identifique tu registro.

## 5. Probar

Reinicia tu servicio (en su terminal `Ctrl+C` y otra vez `.\mvnw.cmd spring-boot:run`). En el log debe salir el topic creado y, al probar
(en **otra** PowerShell, cualquier carpeta):

```powershell
# Camino A y B (directo al servicio, sin token)
Invoke-RestMethod -Method Post http://localhost:8095/api/v1/evaluacionn/saludo/evento
```

En la consola del servicio salen **dos** líneas (envió y recibió):

```text
... component=producer topic=evaluacionn-eventos partition=1 offset=1 eventType=saludo.enviado ... status=published
... component=consumer eventType=saludo.enviado ... mensaje="Hola mundo por Kafka" status=consumed
```

**Camino B:** crea, edita y elimina un examen (S4, paso 5, pero contra `http://localhost:8095/api/v1/examenes`)
y verás `examen.creado`, `examen.actualizado` y `examen.eliminado`.

Mira los mensajes en **Kafka UI**: http://localhost:18090 → *Topics* → `evaluacionn-eventos` → *Messages*.

!!! note "¿Por qué directo al 8095?"
    Por el Gateway el `POST` exige rol `ADMIN` (S7). Con usuario `admin` lo probarás desde el frontend en la S11.

!!! note "Archivos de esta sesión"
    **Creas:**

    - `services\lamb-evaluacionn-ms\src\main\java\pe\edu\upeu\lamb\evaluacionn\config\KafkaTopicsConfig.java`
    - `services\lamb-evaluacionn-ms\src\main\java\pe\edu\upeu\lamb\evaluacionn\event\EvaluacionnEvento.java`
    - `services\lamb-evaluacionn-ms\src\main\java\pe\edu\upeu\lamb\evaluacionn\messaging\EvaluacionnEventosProducer.java`
    - `services\lamb-evaluacionn-ms\src\main\java\pe\edu\upeu\lamb\evaluacionn\messaging\EvaluacionnEventosConsumer.java`

    **Reemplazas:**

    - `services\lamb-evaluacionn-ms\src\main\java\pe\edu\upeu\lamb\evaluacionn\controller\SaludoController.java` - los dos caminos
    - `services\lamb-evaluacionn-ms\src\main\java\pe\edu\upeu\lamb\evaluacionn\service\ExamenService.java` - solo camino B

## 6. Evidencia de cierre de la S8

- Líneas `component=producer ... status=published` y `component=consumer ... status=consumed` en el log.
- Captura del topic `evaluacionn-eventos` con sus mensajes en Kafka UI.

Siguiente: [S9 - Consistencia distribuida](S09_Consistencia_Distribuida_Procesos_Negocio.md).
