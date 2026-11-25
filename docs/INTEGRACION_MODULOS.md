# Integración del Módulo 02 (Admisión + Trayectoria) con otros módulos

Documento guía para cuando se junten los microservicios de los compañeros. Lista todo lo que mi módulo **consume** (necesita que exista en otro ms) y lo que **publica** (eventos y endpoints que otros pueden usar).

> **Base del sistema**: Pagatu — 22 dominios del Sistema Universitario Integrado.
> https://262dist.github.io/pagatu/proyecto-sello/acad/

---

## 1. Resumen rápido

- **Mi módulo cubre**: Dominio 3 (Admisión) + Dominio 8 (Trayectoria Académica).
- **Patrón de integración**:
  - **REST síncrono** vía Gateway → para **leer datos** de otros módulos (Personas, Institucional, Currículo).
  - **Kafka asíncrono** → para **notificar** a otros módulos cuando algo pasa en el mío (postulación creada, adjudicación publicada, estudiante egresado).
- **Nunca acceso directo a BD de otro ms** (regla de microservicios).

---

## 2. Lo que CONSUMO de otros módulos

### 2.1. Dominio 2 — Personas (consume `lamb-personas-ms`)

**Uso**: cuando creo un Postulante necesito un `idPersona`. En vez de que el usuario escriba el ID a ciegas, lo busco/creo por DNI.

**Endpoints que necesito del compañero**:

| Método | Ruta | Para qué |
|---|---|---|
| `GET` | `/api/v1/personas/dni/{dni}` | Buscar persona existente por DNI. Si existe devuelve su `id` y datos; si no, 404. |
| `POST` | `/api/v1/personas` | Crear persona nueva. Body con DNI, nombres, apellidos (vienen de RENIEC). |

**JSON esperado de respuesta (al crear o al buscar)**:
```json
{
  "id": 42,
  "dni": "76903779",
  "nombres": "CRISTOFER DUA",
  "apellidoPaterno": "ORDOÑEZ",
  "apellidoMaterno": "QUISPE",
  "correo": "cristoferdua@upeu.edu.pe",
  "telefono": "993350757"
}
```

**Cómo lo llamo**: desde `lamb-admision-ms`, con `RestClient`, a través del Gateway (`http://localhost:18080/api/v1/personas/...`) — no directo al ms del compañero, porque el Gateway maneja CORS, routing y futura autorización.

**Mientras no exista** → mi backend actualmente solicita `idPersona` manualmente en el form. Cuando el ms Personas esté arriba:
1. Agrego `PersonasClient` en admision-ms (interface Feign o `RestClient` wrapper)
2. En `PostulanteService.crear()`: si viene `dni` en vez de `idPersona`, primero `personasClient.buscarPorDni(dni)` → si 404, `personasClient.crear(dni, nombres...)` → uso el id devuelto
3. Quito el campo `idPersona` del form, dejo solo DNI + RENIEC

---

### 2.2. Dominio 1 — Institucional (consume `lamb-institucional-ms`)

**Uso**: catálogos que necesito mostrar en selects del form de Postulante y Convocatoria.

**Endpoints**:

| Método | Ruta | Para qué |
|---|---|---|
| `GET` | `/api/v1/programas` | Listar programas (Ing. Sistemas, Contabilidad, etc.) → select `idPrograma` |
| `GET` | `/api/v1/facultades` | Listar facultades → select en Trayectoria |
| `GET` | `/api/v1/escuelas` | Listar escuelas → select en Trayectoria |
| `GET` | `/api/v1/sedes` | Listar sedes (Juliaca, Lima, Tarapoto) → select `idSede` |
| `GET` | `/api/v1/periodos-academicos/activo` | Periodo activo (`2026-2`) → autorrellena todos los forms |
| `GET` | `/api/v1/modalidades` | Modalidades académicas (Presencial, Semipresencial, Virtual) |

**JSON genérico esperado**:
```json
[
  { "id": 1, "codigo": "ING-SIS", "nombre": "Ingeniería de Sistemas", "facultadId": 2 },
  { "id": 2, "codigo": "CONTAB",  "nombre": "Contabilidad",            "facultadId": 1 }
]
```

**Mientras no existan** → en el frontend tengo estos como `number` (el usuario escribe el ID). Cuando el ms esté arriba, cambio los campos a tipo `ref` que dispara un select.

---

### 2.3. Dominio 4 — Currículo (consume `lamb-curriculo-ms`)

**Uso**: en Trayectoria necesito saber el plan de estudios de cada estudiante (créditos totales, ciclos, cursos).

**Endpoints**:

| Método | Ruta | Para qué |
|---|---|---|
| `GET` | `/api/v1/planes-estudio/{idPlan}` | Plan de estudios con sus ciclos y cursos |
| `GET` | `/api/v1/planes-estudio?programaId={id}` | Planes vigentes de un programa |
| `GET` | `/api/v1/requisitos-egreso?planId={id}` | Requisitos para egresar de ese plan |

**Uso específico**: al verificar egreso, yo pido a Currículo los `requisitos-egreso` del plan del estudiante, y comparo contra mis `cumplimientos-requisito` locales para decidir si puede egresar.

---

### 2.4. Dominio 7 — Evaluación (consume `lamb-evaluacion-ms`)

**Uso**: Trayectoria calcula promedio, créditos aprobados — pero **no almacena notas** (regla de Pagatu: "historial académico no es segunda fuente de verdad, se deriva").

**Endpoints**:

| Método | Ruta | Para qué |
|---|---|---|
| `GET` | `/api/v1/vistas/historial-acta/{idPersona}` | Historial de todas las notas por acta |
| `GET` | `/api/v1/vistas/nota-final/{idPersona}?periodo={p}` | Notas del periodo |
| `GET` | `/api/v1/vistas/cuadro-meritos?programa={id}&periodo={p}` | Orden de mérito para Reconocimientos |

---

### 2.5. Dominio 6 — Matrícula (consume `lamb-matricula-ms`)

**Uso**: saber qué cursos matriculó el estudiante para calcular avance en Trayectoria.

**Endpoints**:

| Método | Ruta | Para qué |
|---|---|---|
| `GET` | `/api/v1/matriculas?idPersona={id}` | Todas las matrículas del estudiante |
| `GET` | `/api/v1/matriculas?idPersona={id}&periodo={p}` | Matrículas de un periodo |

---

## 3. Lo que PUBLICO para que otros módulos consuman

### 3.1. Endpoints REST expuestos por mi módulo

Ya están publicados vía Gateway en `http://localhost:18080`.

**Admisión** (`lamb-admision-ms`):
- `GET /api/v1/convocatorias` — lista de procesos de admisión abiertos
- `GET /api/v1/postulantes?idConvocatoria={id}` — postulantes de una convocatoria
- `GET /api/v1/adjudicaciones?periodo={p}` — ingresantes publicados del periodo (⬅️ **esto lo consume Matrícula para saber quién puede matricularse**)
- `GET /api/v1/examenes-admision?idConvocatoria={id}`
- `GET /api/v1/admision/reniec/{dni}` — consulta RENIEC (servicio externo wrapeado)

**Trayectoria** (`lamb-trayectoria-ms`):
- `GET /api/v1/trayectorias/{idPersona}/resumen` — estado académico actual (ciclo, créditos aprobados, promedio, % avance)
- `GET /api/v1/trayectorias/me` — mi propia trayectoria (lee del token JWT)
- `GET /api/v1/vistas/avance-curricular?idPersona={id}` — avance por ciclo/curso
- `GET /api/v1/egresos?periodo={p}` — lista de egresados del periodo

### 3.2. Eventos Kafka que publico

**Topic**: usar convención `admision.{entidad}.{accion}` / `trayectoria.{entidad}.{accion}`.

| Topic | Cuándo se publica | Payload |
|---|---|---|
| `admision.postulante.creado` | Al registrar un postulante nuevo | `{ idPostulante, idPersona, dni, idConvocatoria, idPrograma, periodo, timestamp }` |
| `admision.inscripcion.aprobada` | Al aprobar una inscripción | `{ idInscripcion, idPersona, idConvocatoria, periodo, timestamp }` |
| `admision.examen.calificado` | Al registrar calificación | `{ idCalificacion, idPostulante, puntajeTotal, ordenMerito, timestamp }` |
| `admision.adjudicacion.publicada` | Al publicar ingresante (el evento más importante — dispara Matrícula + Trayectoria + Finanzas + LMS) | `{ idAdjudicacion, idPersona, idPrograma, periodo, numeroResolucion, timestamp }` |
| `trayectoria.alerta.generada` | Al detectar alumno en riesgo | `{ idAlerta, idPersona, nivelRiesgo, tipoAlerta, timestamp }` |
| `trayectoria.egreso.confirmado` | Al confirmar egreso | `{ idEgreso, idPersona, idPrograma, periodoEgreso, promedioFinal, timestamp }` |

**Quién escucha qué** (acordar con compañeros):

- `admision.adjudicacion.publicada` →
  - **Trayectoria (yo)**: creo la entrada inicial de Trayectoria con estado=ACTIVO
  - **Finanzas del estudiante**: genera el primer cargo de matrícula
  - **LMS**: habilita acceso al aula virtual
  - **Matrícula**: habilita al estudiante para matricularse

- `trayectoria.egreso.confirmado` →
  - **Egresados (dominio 18)**: lo agrega al registro de alumni
  - **Biblioteca (dominio 20)**: cierra préstamos pendientes
  - **Finanzas**: verifica que no haya cargos abiertos

### 3.3. Formato Kafka (JSON envelope estándar)

Propongo este envelope común para TODOS los eventos del sistema, que deberíamos acordar entre todos:

```json
{
  "eventId": "uuid-v4",
  "eventType": "admision.adjudicacion.publicada",
  "eventTime": "2026-11-05T12:30:00Z",
  "sourceService": "lamb-admision-ms",
  "correlationId": "uuid-de-la-postulacion-origen",
  "payload": {
    "idAdjudicacion": 42,
    "idPersona": 101,
    "idPrograma": 5,
    "periodo": "2026-2",
    "numeroResolucion": "R-2026-00042"
  }
}
```

---

## 4. Comunicación con servicios externos (fuera del equipo)

Estos NO son microservicios de compañeros, son servicios de terceros que SÍ uso.

| Servicio | Para qué | Endpoint | Credencial |
|---|---|---|---|
| **RENIEC** | Autocompletar datos del postulante al escribir DNI | `GET https://api.apis.net.pe/v2/reniec/dni?numero={dni}` | Bearer token en env var `LAMB_RENIEC_TOKEN` |
| **Resend** ✅ | Correo de bienvenida tras postulación (template HTML). Envío after-commit (nunca enviar si rollback). Modo mock si no hay key. | `POST https://api.resend.com/emails` | API key en env var `LAMB_RESEND_KEY`, from en `LAMB_RESEND_FROM` (default: `onboarding@resend.dev` sandbox) |
| **Keycloak** | Autenticación (ya configurado) | `http://localhost:18180/realms/lamb/...` | Realm `lamb`, client `lamb-frontend` (PKCE) |

Si mi compañero también necesita RENIEC/SendGrid, propongo **centralizarlo** en un ms compartido (`lamb-externos-ms`) o que cada uno llame directo. Hasta que no haya segundo consumidor, está en mi `lamb-admision-ms`.

---

## 4b. Campos autogenerados por el backend (nunca los envía el frontend)

Lista de campos que **mi backend genera solo** — el usuario no los ve ni los escribe. Si vienen vacíos en el request, el backend los pone; si vienen rellenados, respeta lo enviado (útil para pruebas/seed).

| Entidad | Campo | Formato | Cuándo se genera |
|---|---|---|---|
| Postulante | `codigoPostulante` | `POS-{periodo}-{correlativo4}`, ej. `POS-2026-2-0042` | Al crear postulante, si no viene |
| Postulante | `estado` | `REGISTRADO` | Default al crear |
| Postulante | `fechaRegistro` | `now()` | Siempre en crear |
| Convocatoria | `codigo` | lo asigna la secretaría al abrir convocatoria (ej. `CONV-2026-2`) | Manual |
| Adjudicación | `numeroResolucion` | `R-{año}-{correlativo5}`, ej. `R-2026-00042` | Al publicar adjudicación (pendiente) |
| Adjudicación | `ordenMerito` | calculado en base a `puntajeFinal` desc | Al publicar adjudicación |
| Examen | `codigo` | manual (lo pone el área académica) | — |
| Calificación | `ordenMerito` | calculado al finalizar examen | Automático |
| Trayectoria | `creditosAprobados` | calculado desde Evaluación (vista) | Automático |
| Trayectoria | `promedioPonderado` | calculado desde Evaluación (vista) | Automático |
| Egreso | `numeroResolucion` | `RE-{año}-{correlativo5}` | Al confirmar egreso (pendiente) |

**Regla general**: cualquier campo llamado `codigo*`, `numero*` o `fecha{Accion}` lo pone backend. Cualquier campo calculado (promedios, créditos, orden mérito) lo deriva backend de otros módulos.

---

## 5. Convenciones acordadas (checklist antes de juntar)

Antes de integrar, confirmar con el equipo:

- [ ] ¿Todos usamos `idPersona: Long`? ¿O UUID? → **Mi módulo: Long**.
- [ ] ¿Formato de `periodo`? → **Mi módulo: `"2026-2"` (string)**.
- [ ] ¿Topic Kafka con nombre dominio o microservicio? → **Propongo: dominio** (`admision.*`, no `admision-ms.*`).
- [ ] ¿Serialización Kafka? → **Propongo: JSON con Jackson**, no Avro (más simple para proyecto).
- [ ] ¿Gateway único o cada ms con su gateway? → **Único** (`lamb-gateway` en 18080).
- [ ] ¿Config centralizada? → **Sí**, via `lamb-config` (Spring Cloud Config).
- [ ] ¿Discovery? → **Sí**, via Eureka (`lamb-eureka` en 18761).
- [ ] ¿JWT por el Gateway o por cada ms? → **Ambos**: Gateway filtra roles, cada ms valida token con `oauth2-resource-server`.

---

## 6. Qué hacer cuando uno de ustedes suba su microservicio

1. **Me avisas** en qué puerto corre y qué endpoints expone.
2. Agrego su ruta al Gateway (`infra/lamb-config/config-repo/lamb-gateway-dev.yml`).
3. Creo el `*Client` en mi admision-ms o trayectoria-ms para consumirlo.
4. Quito los campos `idPersona`/`idPrograma`/etc. que el usuario digita manualmente, los convierto en selects.
5. Pruebo end-to-end y confirmo que todo sigue funcionando.

**En el peor caso** (no está el ms de alguien) → mantengo mock temporal con datos hardcoded y documento el `TODO: reemplazar por llamada a lamb-{compañero}-ms`.

---

## 6b. 🚨 CHECKLIST DEL DÍA DE INTEGRACIÓN (leer antes de juntar todo)

Esta sección es para cuando llegue el día de juntar los microservicios de todo el equipo. Léela completa antes de hacer cualquier merge.

### Paso 1 — Antes de empezar (1 hora)

- [ ] Todos hicieron `git pull` de main en sus repos y están en una rama limpia.
- [ ] Nadie tiene cambios sin commitear que puedan perderse.
- [ ] Levantar **solo** la infra base en orden: Docker (Kafka + Keycloak + DBs) → config → eureka → gateway. Confirmar que los 4 están UP antes de arrancar microservicios.
- [ ] Hacer un snapshot de estado inicial: `git tag pre-integracion-{fecha}` en cada repo, por si hay que volver.

### Paso 2 — Convenciones críticas a confirmar EN VIVO (30 min, todos juntos)

Antes de arrancar los ms, confirmar palabra por palabra:

- [ ] **Tipo de ID**: todos usamos `Long` (no UUID). Mi módulo: `Long`.
- [ ] **Formato de periodo**: `"2026-2"` como string. Nunca `"2026-II"`, nunca separado en año/semestre.
- [ ] **Serialización Kafka**: JSON con Jackson. Sin Avro.
- [ ] **Naming de topics Kafka**: `{dominio}.{entidad}.{accion}` (ej. `admision.postulante.creado`). Yo uso el topic único `admision-eventos` con `tipoEvento` adentro del payload — a acordar si lo mantenemos o separamos.
- [ ] **Envelope JSON** común: `{eventId, eventType, eventTime, sourceService, correlationId, payload}`. Si lo aprobamos, yo adapto mis producers/consumers.
- [ ] **Puerto de cada ms**: cada uno dice el suyo, se anota en un Google Sheet / canvas común.
- [ ] **Base de datos**: cada ms tiene su propia BD PostgreSQL separada. Nadie comparte esquema.

### Paso 3 — Orden de arranque de los ms (1 hora)

**Hay un orden obligatorio** porque unos dependen de otros:

1. **Dominio 2 — Personas** ← arranca primero (base universal, todos dependen).
2. **Dominio 1 — Institucional** ← arranca segundo (catálogos de programas, sedes).
3. **Dominio 4 — Currículo** ← tercero (planes de estudio).
4. **Mi Admisión + Trayectoria** ← cuarto.
5. **Dominio 5 — Planificación** ← quinto.
6. **Dominio 6 — Matrícula** ← sexto (consume Admisión + Planificación).
7. **Dominio 7 — Evaluación** ← séptimo.
8. **Dominio 9 — Finanzas** ← al final (consume todos).

Esperar a que cada uno esté UP (ver en Eureka `http://localhost:18761`) antes de arrancar el siguiente.

### Paso 4 — Integrar Personas con Admisión (lo más importante para mí)

1. [ ] **Agregar ruta al Gateway** en `lamb-gateway-dev.yml`:
   ```yaml
   - id: lamb-personas
     uri: lb://lamb-personas-ms
     predicates:
       - Path=/api/v1/personas/**
   ```
2. [ ] **Agregar `PersonasClient`** en `lamb-admision-ms` (nuevo archivo `client/PersonasClient.java`):
   ```java
   @Component
   public class PersonasClient {
     private final RestClient rc;
     public PersonasClient(@Value("${lamb.personas.url:http://lamb-gateway:18080}") String url) {
       this.rc = RestClient.builder().baseUrl(url).build();
     }
     public Optional<PersonaDto> buscarPorDni(String dni) { ... }
     public PersonaDto crear(CrearPersonaRequest req) { ... }
   }
   ```
3. [ ] **Modificar `PostulacionWizardService.resolverIdPersonaDesdeDni()`**: reemplazar el mock (`(dni % 9000000) + 1000000`) por:
   ```java
   return personasClient.buscarPorDni(dni)
       .map(PersonaDto::getId)
       .orElseGet(() -> personasClient.crear(
           new CrearPersonaRequest(dni, reniec.getNombres(), reniec.getApellidoPaterno(), reniec.getApellidoMaterno())
       ).getId());
   ```
4. [ ] **Pasar RENIEC al wizard service** para que al crear persona pueda enviar los nombres oficiales (hoy los tiene el controller pero el service no, cambio menor).
5. [ ] **Quitar el campo `idPersona` del DTO** `PostulacionWizardRequest` — ya no se envía, se resuelve solo.
6. [ ] **Test**: crear postulante nuevo → verificar en BD de Personas que se creó una Persona con DNI correcto y mismo `idPersona` que quedó en Postulante.

### Paso 5 — Integrar catálogos con Admisión y Trayectoria

1. [ ] **Institucional / Programas** (consumido por Postulante, Trayectoria):
   - Reemplazar `idPrograma: number` en `cruds.catalog.ts` por `ref` que apunta a `/api/v1/programas`
   - Mi backend hoy no valida que `idPrograma` exista — no cambiar, Institucional tampoco pediría una FK estricta entre BDs distintas
2. [ ] **Institucional / Sedes, Facultades, Escuelas** (consumido por Trayectoria):
   - Mismo patrón que arriba
3. [ ] **Institucional / Periodo activo**:
   - En vez de hardcodear `2026-2` en el topbar, llamar `GET /api/v1/periodos-academicos/activo` al iniciar la app Angular
4. [ ] **Currículo / Planes de estudio** (consumido por Trayectoria):
   - El componente `plan-academico` hoy tiene datos mockeados → cambiar por llamada real

### Paso 6 — Validar eventos Kafka en vivo

1. [ ] Mi Admisión publica `postulante.creado` → verificar que Trayectoria lo recibe (listener Kafka del compañero debería crear una Trayectoria).
2. [ ] Simular Adjudicación → verificar que Finanzas crea el primer cargo de matrícula.
3. [ ] Abrir **Kafka UI** (`http://localhost:18090`) y revisar que los mensajes aparecen en `admision-eventos`.
4. [ ] Si alguien no consume → revisar grupo de consumidor en Kafka UI. Si está "stuck", reiniciar su ms.

### Paso 7 — Gateway: consolidar rutas

El Gateway hoy tiene TODAS las rutas de todos los módulos. Confirmar:

- [ ] Cada ms está registrado en Eureka con el nombre exacto que el gateway espera (`lb://lamb-personas-ms`, etc.).
- [ ] Nadie duplicó un path. Si dos ms exponen `/api/v1/algo-genérico`, hay colisión.
- [ ] CORS: Angular corre en `4200`, ya está permitido. Si alguien levanta el front en `3000` u otro puerto, agregar a `allowed-origins`.
- [ ] `permitAll` solo en rutas de DEV. En el merge final, acordar qué rutas son públicas (ej. `/api/v1/convocatorias GET` para landing page).

### Paso 8 — Seguridad / Keycloak

- [ ] Todos apuntamos al mismo realm `lamb` en `http://localhost:18180`.
- [ ] Roles acordados: `ADMIN`, `COORDINADOR`, `DOCENTE`, `ESTUDIANTE`. Mi gateway ya los filtra con `hasAnyRole`.
- [ ] Mi microservicio tiene `oauth2-resource-server` pero las rutas están en `permitAll` para DEV. En producción habría que cerrar.
- [ ] El frontend Angular usa `keycloak-js` con PKCE. Si cambian el client_id, avisar.

### Paso 9 — Base de datos

- [ ] **Nunca** acceder a la BD de otro ms directamente. Si necesitas un dato, llamar a su API.
- [ ] Cada ms tiene su migración Flyway independiente. Verificar que ningún ms intente modificar tabla de otro.
- [ ] Si hay seeds compartidos (ej. sedes, programas), vienen de Institucional y los demás los **leen por API**, no los duplican.

### Paso 10 — Post-integración (smoke test completo)

Después de arrancar todo, ejecutar este flujo end-to-end:

1. [ ] Login en Angular con usuario `admin@upeu.edu.pe` → entra al dashboard
2. [ ] Ir a **Admisión → Nueva postulación** → DNI real → RENIEC rellena → selecciona Convocatoria + Programa + Modalidad → Registrar
3. [ ] Verificar:
   - [ ] En BD Personas se creó el registro
   - [ ] En BD Admisión quedó el Postulante con el mismo `idPersona`
   - [ ] En Kafka UI aparece el mensaje en `admision-eventos`
   - [ ] En BD Trayectoria NO debe crearse todavía (solo con adjudicación)
4. [ ] Simular adjudicación (POST a `/api/v1/adjudicaciones`) → verificar que:
   - [ ] Kafka publica `adjudicacion.publicada`
   - [ ] Trayectoria crea la entrada inicial
   - [ ] Finanzas crea el primer cargo
5. [ ] Ir a Trayectoria → ver el estudiante listado con estado ACTIVO

Si TODOS los puntos pasan ✅ → la integración está lista.

### Paso 11 — Si algo falla, cómo debugear

| Síntoma | Causa probable | Qué revisar |
|---|---|---|
| 404 en el Gateway | Ruta no agregada | `lamb-gateway-dev.yml` → agregar la ruta |
| 503 Service Unavailable | ms no registrado en Eureka | abrir `http://localhost:18761`, verificar que aparezca |
| 401 Unauthorized | JWT expirado o mal firmado | logout + login en Angular, verificar realm correcto |
| 500 en POST | FK inválida o validación | revisar logs del ms destino |
| Evento Kafka no llega | consumer group stuck | Kafka UI → reset offset o reiniciar el consumer ms |
| CORS error en navegador | nuevo origen no permitido | agregar a `allowed-origins` del gateway |
| ms arranca pero muere | puerto ocupado o BD caída | `netstat -ano \| findstr {puerto}` y `docker ps` |

### Paso 12 — Reglas de convivencia (para que nadie rompa a nadie)

1. **Nunca commitear secrets** (tokens RENIEC, API keys SendGrid). Usar env vars.
2. **Nunca borrar topic Kafka** del compañero. Los topics son compartidos.
3. **Nunca cambiar un endpoint ya documentado** sin avisar. Si hay que cambiarlo, versionar (`/api/v2/...`).
4. **Nunca hacer `git push --force`** a main. Siempre PR.
5. **Si tu ms produce un evento mal formado** y rompe el consumer de otro, arreglarlo YA no después.
6. **Documentar** en este MD cualquier cambio de contrato antes del merge.

---

---

## 7. Diagrama resumen

```
                    ┌─────────────────────────┐
                    │  Frontend Angular (:4200)│
                    └────────────┬────────────┘
                                 │
                          HTTP + JWT Keycloak
                                 ↓
                    ┌─────────────────────────┐
                    │  Gateway (:18080)        │
                    └────────────┬────────────┘
                                 │
         ┌───────────┬───────────┼───────────┬─────────────┐
         ↓           ↓           ↓           ↓             ↓
    ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌──────────┐
    │Personas │ │Institu- │ │Admisión │ │Trayecto-│ │Currículo │
    │  (otro) │ │ cional  │ │  (MÍO)  │ │ ria(MÍO)│ │  (otro)  │
    └─────────┘ └─────────┘ └────┬────┘ └────┬────┘ └──────────┘
                                 │           │
                                 └──┬────────┘
                                    ↓
                             ┌─────────────┐
                             │   Kafka     │
                             │  (:19092)   │
                             └──────┬──────┘
                                    │
             ┌──────────────────────┼──────────────────────┐
             ↓                      ↓                      ↓
        ┌─────────┐          ┌──────────┐          ┌──────────┐
        │Finanzas │          │Matrícula │          │   LMS    │
        │  (otro) │          │  (otro)  │          │  (otro)  │
        └─────────┘          └──────────┘          └──────────┘

                             ╔═══════════════╗
                             ║  Keycloak     ║  (externo)
                             ║  (:18180)     ║
                             ╚═══════════════╝
                             ╔═══════════════╗
                             ║  RENIEC       ║  (externo)
                             ║ apis.net.pe   ║
                             ╚═══════════════╝
```

---

**Última actualización**: 2026-11-05
**Mantenedor**: Cristofer Dua
