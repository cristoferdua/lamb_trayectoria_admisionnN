# S9 - Consistencia distribuida en procesos de negocio

*Guía única: agregar un microservicio nuevo al proyecto Lamb - Parte 9 de 16 - 2026*

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
    cd services\lamb-evaluacionn-ms; .\mvnw.cmd spring-boot:run    # Tu servicio
    ```

    Comprueba que responden:

    - Kafka + Kafka UI: http://localhost:18090
    - BD de tu servicio: `docker ps` → `lamb-postgres-evaluacionn-dev` *Up*
    - Config Server: http://localhost:18888/lamb-evaluacionn-ms/dev
    - Eureka: http://localhost:18761
    - Tu servicio: http://localhost:8095/actuator/health

## 1. Introducción

Cuando un servicio **guarda en su base de datos** y **avisa por Kafka**, hay un riesgo:
avisar algo que al final **no se guardó** (por ejemplo, si la base de datos falla justo después).
Tu servicio ya está protegido: no tienes que escribir código nuevo, pero sí **entender y demostrar** por qué.

## 2. Lo que ya hace tu código

### 2.1 `@Transactional` en el servicio (camino B)

En `ExamenService`, `crear`, `actualizar` y `eliminar` tienen `@Transactional`: o se guarda **todo**, o **nada**.

### 2.2 `publicarTrasCommit` en el productor (los dos caminos)

```java
public void publicarTrasCommit(EvaluacionnEvento evento) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
        // hay una transacción abierta: espera a que se confirme (commit)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                enviar(evento);
            }
        });
    } else {
        // no hay transacción (ej. el botón de Hola mundo): envía de inmediato
        enviar(evento);
    }
}
```

| Situación | Qué pasa |
|---|---|
| Se guarda bien | Después del commit se publica `examen.creado` |
| Falla al guardar (error, validación, BD caída) | Se hace *rollback* y **no** se publica nada |
| Camino A (sin base de datos) | Se publica de inmediato |

Es el mismo mecanismo que usa `PagoEventosProducer` en Pagatu.

## 3. Si te piden más: idempotencia

Kafka puede entregar **el mismo mensaje dos veces**. Si te piden que tu consumidor **haga algo** al recibir un
evento (ej. guardar un registro), debe ignorar los repetidos. Patrón de `pagatu-pago-ms` (`PagoServiceImpl.procesar`):

```java
@Transactional
public void procesar(EvaluacionnEvento evento) {
    if (historialRepository.existsByEventoId(evento.getId())) {      // tu repositorio y campo
        log.warn("component=processor id={} status=ignored motivo=\"evento duplicado\"", evento.getId());
        return;
    }
    // ... guardar lo que te pidan ...
}
```

Para la compensación (deshacer un paso cuando otro falla, como `pago.fallido` → orden `CANCELADA`), sigue la
[S9 original de Pagatu](https://262dist.github.io/pagatu/sesiones/S09_Consistencia_Distribuida_Procesos_Negocio/).

!!! note "Esta sección 3 no está en el ejemplo probado"
    Solo hazla si te la piden: los caminos A y B funcionan sin ella.

## 4. Probar (camino B)

Envía un examen **inválido** (título vacío) y revisa que **no** salga ningún evento:

```powershell
Invoke-RestMethod -Method Post http://localhost:8095/api/v1/examenes -ContentType "application/json" -Body '{"titulo":""}'
```

Responde `400` ("Error de validación en los datos enviados") y en el log **no** aparece `component=producer`.
Con un examen válido sí aparece `examen.creado`.

!!! note "Archivos de esta sesión"
    En esta sesión **no creas ni cambias archivos**: el código de la S8 ya lo hace.

## 5. Evidencia de cierre de la S9

- Captura del `400` sin evento y del `201` con su evento `examen.creado` (camino B).
- Explicación en tus palabras de `publicarTrasCommit`.

Siguiente: [S10 - Observabilidad](S10_Observabilidad_Diagnostico_Sistemas_Distribuidos.md).
