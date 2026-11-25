# S11 - Integración del cliente (frontend Angular con Keycloak)

*Guía única: agregar un microservicio nuevo al proyecto Lamb - Parte 11 de 16 - 2026*

!!! note "Antes de empezar: debe estar levantado"
    Desde la raíz del proyecto (`Lamb-Modulo02-Admision-Trayectoria`). Si ya está corriendo, no lo arranques otra vez.

    **Docker** (Docker Desktop abierto; se pueden pegar en la misma terminal):

    ```powershell
    docker compose -f kafka/compose-dev.yml up -d    # Kafka + Kafka UI
    docker compose -f keycloak/compose-dev.yml up -d    # Keycloak (tarda ~1 min)
    docker compose -f services/lamb-evaluacionn-ms/compose-dev.yml up -d    # BD de tu servicio
    ```

    **Servicios** (cada uno en **su propia terminal**, en este orden, esperando que diga `Started ...`):

    ```powershell
    cd infra\lamb-config; .\mvnw.cmd spring-boot:run    # Config Server
    cd infra\lamb-eureka; .\mvnw.cmd spring-boot:run    # Eureka
    cd services\lamb-evaluacionn-ms; .\mvnw.cmd spring-boot:run    # Tu servicio
    cd infra\lamb-gateway; .\mvnw.cmd spring-boot:run    # Gateway (al final)
    ```

    Comprueba que responden:

    - Kafka + Kafka UI: http://localhost:18090
    - Keycloak: http://localhost:18180/realms/lamb
    - BD de tu servicio: `docker ps` → `lamb-postgres-evaluacionn-dev` *Up*
    - Config Server: http://localhost:18888/lamb-evaluacionn-ms/dev
    - Eureka: http://localhost:18761
    - Tu servicio: http://localhost:8095/actuator/health
    - Gateway: http://localhost:18080/actuator/health

    La primera vez, antes de `npm start`: `cd clients\lamb-frontend; npm install`. El frontend se arranca **en esta sesión** (paso 4).

## 1. Introducción

Agregas páginas al frontend Angular de Lamb (`clients\lamb-frontend`), **sin diseño**: HTML simple, sin
estilos ni componentes bonitos. Siguen el patrón de `features/catalogo` de Pagatu: un **service** que llama al
Gateway y componentes con su `.ts` y su `.html`. El login de Keycloak y el envío del token **ya los hace Lamb**.

| Camino | Páginas |
|---|---|
| A - Sin CRUD | `/evaluacionn`: "Hola mundo" + botón *Enviar evento a Kafka* |
| B - Con CRUD | lo del camino A + `/evaluacionn/examenes` (tabla), `/evaluacionn/examenes/nuevo` y `/evaluacionn/examenes/:id/editar` (formulario) |

Crea la carpeta `evaluacionn` (cambia por tu nombre): en VS Code, clic derecho en `clients\lamb-frontend\src\app\features\`
→ **New Folder** → `evaluacionn`. Ahí van **todos** los archivos `.ts` y `.html` de esta sesión (salvo `app.routes.ts`).

```text
clients\lamb-frontend\src\app\
├── app.routes.ts          <- se MODIFICA (rutas)
└── features\
    ├── admision\  inicio\  trayectoria\ ...   <- ya existen, no se tocan
    └── evaluacionn\        <- NUEVA: aquí creas todos los archivos
```

## 2. Código común (los dos caminos): la página Hola mundo

### 2.1 `evaluacionn-service.ts`

**Crea el archivo:** `evaluacionn-service.ts`  
**En la ruta:** `clients\lamb-frontend\src\app\features\evaluacionn\`  
**Cómo:** clic derecho en la carpeta `evaluacionn` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

{{lamb:clients/lamb-frontend/src/app/features/biblioteca/biblioteca-service.ts}}

!!! note "Qué cambia"
    El nombre de la clase y las rutas `/api/v1/evaluacionn/...`. `environment.apiUrl` es el Gateway (`http://localhost:18080`): no lo cambies.

### 2.2 `saludo.ts`

**Crea el archivo:** `saludo.ts`  
**En la ruta:** `clients\lamb-frontend\src\app\features\evaluacionn\`  
**Cómo:** clic derecho en la carpeta `evaluacionn` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

{{lamb:clients/lamb-frontend/src/app/features/biblioteca/saludo.ts}}

### 2.3 `saludo.html`

**Camino A - Sin CRUD:** quita la última línea (el link *Ver examenes*). **Camino B:** pégalo completo.

**Crea el archivo:** `saludo.html`  
**En la ruta:** `clients\lamb-frontend\src\app\features\evaluacionn\`  
**Cómo:** clic derecho en la carpeta `evaluacionn` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

{{lamb:clients/lamb-frontend/src/app/features/biblioteca/saludo.html}}

!!! note "Camino A: el aviso de `RouterLink`"
    Si quitas el link, Angular avisa que `RouterLink` no se usa (es solo un aviso). Para quitarlo,
    borra `RouterLink` del `import` y del `imports: [...]` en `saludo.ts`.

### 2.4 La ruta: `clients\lamb-frontend\src\app\app.routes.ts`

**Abre el archivo:** `app.routes.ts`  
**En la ruta:** `clients\lamb-frontend\src\app\`  
**Qué hacer:** Busca (`Ctrl+F`) la línea `{ path: '**', redirectTo: '' }` (casi al final) y pega **justo arriba** de ella. No borres lo demás del archivo.


```ts
  // ====== EVALUACIONN (paginas simples, sin diseño; piden login de Keycloak) ======
  {
    path: 'evaluacionn',
    canActivate: [adminGuard],
    loadComponent: () => import('./features/evaluacionn/saludo').then((m) => m.Saludo)
  },
```

`adminGuard` (ya existe en Lamb) pide iniciar sesión en Keycloak. No lo cambies.

**Si elegiste el camino A, aquí terminas la S11:** ve a la sección 4 para probar.

## 3. Camino B - Con CRUD: lista y formulario

**Si quieres agregar el servicio con CRUD, sigue estos pasos.** Todos se crean en `clients\lamb-frontend\src\app\features\evaluacionn\` (la misma carpeta del paso 2).

### 3.1 `examen.model.ts`

**Crea el archivo:** `examen.model.ts`  
**En la ruta:** `clients\lamb-frontend\src\app\features\evaluacionn\`  
**Cómo:** clic derecho en la carpeta `evaluacionn` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

{{lamb:clients/lamb-frontend/src/app/features/biblioteca/libro.model.ts}}

!!! note "Qué cambia"
    Un atributo por cada campo del `ExamenResponse` (S1). `id?` siempre se queda. Tipos: texto → `string`,
    números → `number`, fecha → `string`, sí/no → `boolean`. Opcional → con `?`.

### 3.2 `examen-service.ts`

**Crea el archivo:** `examen-service.ts`  
**En la ruta:** `clients\lamb-frontend\src\app\features\evaluacionn\`  
**Cómo:** clic derecho en la carpeta `evaluacionn` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

{{lamb:clients/lamb-frontend/src/app/features/biblioteca/libro-service.ts}}

!!! note "Qué cambia"
    Los nombres y la ruta `/api/v1/examenes`. Los 5 métodos se quedan igual.

### 3.3 `examen-list.ts` (la tabla)

**Crea el archivo:** `examen-list.ts`  
**En la ruta:** `clients\lamb-frontend\src\app\features\evaluacionn\`  
**Cómo:** clic derecho en la carpeta `evaluacionn` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

{{lamb:clients/lamb-frontend/src/app/features/biblioteca/libro-list.ts}}

### 3.4 `examen-list.html`

**Crea el archivo:** `examen-list.html`  
**En la ruta:** `clients\lamb-frontend\src\app\features\evaluacionn\`  
**Cómo:** clic derecho en la carpeta `evaluacionn` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

{{lamb:clients/lamb-frontend/src/app/features/biblioteca/libro-list.html}}

!!! note "Qué cambia"
    Un `<th>` y un `<td>{{ examen.x }}</td>` por cada atributo que quieras mostrar, y las rutas `/evaluacionn/examenes...`.
    Ajusta el `colspan="3"` al número de columnas.

### 3.5 `examen-form.ts` (agregar y editar con el mismo formulario)

Si la ruta trae `:id`, carga el examen y al guardar hace `PUT`; si no, hace `POST`.

**Crea el archivo:** `examen-form.ts`  
**En la ruta:** `clients\lamb-frontend\src\app\features\evaluacionn\`  
**Cómo:** clic derecho en la carpeta `evaluacionn` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

{{lamb:clients/lamb-frontend/src/app/features/biblioteca/libro-form.ts}}

!!! note "Qué cambia"
    En `form = this.fb.nonNullable.group({...})` va **un control por atributo** con sus validaciones
    (las mismas del Request), y en `mensajeValidacion(campo: 'titulo' | 'curso')` la lista de tus atributos.
    Ejemplo para Pagos:
    ```ts
    protected readonly form = this.fb.nonNullable.group({
      monto: [0, [Validators.required, Validators.min(0)]],
      fecha: ['', [Validators.required]],
      metodoPago: ['', [Validators.maxLength(30)]],
    });
    ```

### 3.6 `examen-form.html`

**Crea el archivo:** `examen-form.html`  
**En la ruta:** `clients\lamb-frontend\src\app\features\evaluacionn\`  
**Cómo:** clic derecho en la carpeta `evaluacionn` → **New File** (es un archivo, no una carpeta) → escribe el nombre exacto.  
**Y pega el siguiente código:**

{{lamb:clients/lamb-frontend/src/app/features/biblioteca/libro-form.html}}

!!! note "Qué cambia"
    Un bloque `<label>...<input formControlName="x" />...</label>` + su mensaje por cada atributo.
    El tipo de `<input>` está en la tabla de la S1 (6.1).

### 3.7 Las rutas: `app.routes.ts`

**Abre el archivo:** `app.routes.ts`  
**En la ruta:** `clients\lamb-frontend\src\app\`  
**Qué hacer:** El mismo archivo del paso 2.4. Busca la ruta `path: 'evaluacionn'` que pegaste y pega esto **debajo** de su `},`. No borres lo demás del archivo.


```ts
  {
    path: 'evaluacionn/examenes',
    canActivate: [adminGuard],
    loadComponent: () => import('./features/evaluacionn/examen-list').then((m) => m.ExamenList)
  },
  {
    path: 'evaluacionn/examenes/nuevo',
    canActivate: [adminGuard],
    loadComponent: () => import('./features/evaluacionn/examen-form').then((m) => m.ExamenForm)
  },
  {
    path: 'evaluacionn/examenes/:id/editar',
    canActivate: [adminGuard],
    loadComponent: () => import('./features/evaluacionn/examen-form').then((m) => m.ExamenForm)
  },
```

## 4. Probar

**Dónde abrir la terminal:** En la **raíz del proyecto** (`Lamb-Modulo02-Admision-Trayectoria`). En VS Code: *Terminal → New Terminal*. Déjala abierta: el frontend se queda corriendo ahí.

```powershell
cd clients\lamb-frontend
npm start
```

### Camino A - Sin CRUD

1. Abre http://localhost:4200/evaluacionn → te pide login → `admin` / `admin123`.
2. Si te deja en la página de inicio, vuelve a abrir http://localhost:4200/evaluacionn.
3. Ves **"Hola mundo desde lamb-evaluacionn-ms"**. Pulsa *Enviar evento a Kafka* → "Evento enviado a Kafka."
4. Cierra sesión, entra como `estudiante1` / `estudiante123` y pulsa el botón → "No tienes permiso para esta acción."

### Camino B - Con CRUD

1. Entra como `admin` → http://localhost:4200/evaluacionn → *Ver examenes*.
2. *Nuevo examen* → llena el formulario → *Guardar* → aparece en la tabla.
3. *Editar* → cambia el título → *Guardar* → se actualiza.
4. *Eliminar* → confirma → desaparece.
5. Como `estudiante1`: ve la tabla, pero al guardar sale "No tienes permiso para esta acción."

!!! note "Errores de Keycloak en la consola (F12)"
    Lamb muestra un error de CORS al cargar el perfil (`/realms/lamb/account`). Ya existía y no afecta a tus páginas.

!!! note "Archivos de esta sesión"
    **Los dos caminos** - creas en `clients\lamb-frontend\src\app\features\evaluacionn\`:

    - `evaluacionn-service.ts`, `saludo.ts`, `saludo.html`

    **Solo camino B** - además creas (en la misma carpeta):

    - `examen.model.ts`, `examen-service.ts`, `examen-list.ts`, `examen-list.html`, `examen-form.ts`, `examen-form.html`

    **Modificas:**

    - `clients\lamb-frontend\src\app\app.routes.ts` - 1 ruta (camino A) o 4 rutas (camino B)

## 5. Evidencia de cierre de la S11

- Capturas de las páginas funcionando (login, Hola mundo, y en el camino B: tabla, formulario, eliminar).
- Captura del `403` para `estudiante1`.

Siguiente: [S12 - Evaluación de la Unidad II](S12_Evaluacion_Unidad_2.md).
