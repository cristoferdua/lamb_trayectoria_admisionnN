# S7b - Seguridad distribuida con Keycloak

*Guía única: agregar un microservicio nuevo al proyecto Lamb - Parte 7b de 16 - 2026*

!!! note "Antes de empezar: debe estar levantado"
    Desde la raíz del proyecto (`Lamb-Modulo02-Admision-Trayectoria`). Si ya está corriendo, no lo arranques otra vez.

    **Docker** (Docker Desktop abierto; se pueden pegar en la misma terminal):

    ```powershell
    docker compose -f keycloak/compose-dev.yml up -d    # Keycloak (tarda ~1 min)
    ```

    Comprueba que responden:

    - Keycloak: http://localhost:18180/realms/lamb

    Entra a la consola con `admin` / `admin123`.

## 1. Introducción

**Keycloak** (`http://localhost:18180`) es quien maneja los usuarios, las contraseñas y los roles de Lamb.
Tu servicio **no crea usuarios ni guarda contraseñas**: solo confía en el token que firma Keycloak.
Esta sesión es igual para los dos caminos y casi no tiene código: es saber **usar la consola de Keycloak**.

| Dato | Valor en Lamb |
|---|---|
| Consola | http://localhost:18180 → `admin` / `admin123` |
| Realm | `lamb` |
| Cliente del frontend | `lamb-frontend` |
| Roles | `ADMIN`, `COORDINADOR`, `DOCENTE`, `ESTUDIANTE` |
| Usuarios de prueba | `admin` / `admin123`, `docente1` / `docente123`, `estudiante1` / `estudiante123` |

!!! note "Esto NO se cambia"
    El realm, el cliente y el `issuer-uri` (`http://localhost:18180/realms/lamb`) son los mismos para todos los
    servicios de Lamb.

## 2. Cómo viaja el token

1. El usuario entra a una página protegida del frontend → Keycloak le pide usuario y contraseña.
2. Keycloak le da un **token** (JWT) con sus roles dentro (`realm_access.roles`).
3. El frontend manda el token en cada llamada al Gateway (`Authorization: Bearer ...`), automáticamente
   (lo hace `core\auth.interceptor.ts`, que ya existe en Lamb).
4. El Gateway revisa el rol (S7) y reenvía la petición, **con el token**, a tu servicio.
5. Tu servicio valida la firma del token (`oauth2ResourceServer`, S7).

## 3. Si te piden un rol nuevo

Ejemplo: "solo el **EVALUADOR** puede registrar examenes".

1. http://localhost:18180 → `admin` / `admin123` → arriba a la izquierda elige el realm **lamb**.
2. *Realm roles* → *Create role* → nombre `EVALUADOR` → *Save*.
3. *Users* → elige el usuario → pestaña *Role mapping* → *Assign role* → filtra *realm roles* → marca `EVALUADOR` → *Assign*.
4. En el Gateway (S7) usa `.hasAnyRole("ADMIN", "EVALUADOR")`. Reinicia el Gateway.
5. El usuario debe **cerrar sesión y volver a entrar** para que su token traiga el rol nuevo.

Cambia `EVALUADOR` por el rol que te pidan. En el código el rol se escribe **sin** `ROLE_` delante.

## 4. Si te piden un usuario nuevo

*Users* → *Add user* → llena *Username* → *Create* → pestaña *Credentials* → *Set password*
(desmarca *Temporary*) → pestaña *Role mapping* → asígnale sus roles.

!!! note "Archivos de esta sesión"
    En esta sesión **no creas ni cambias archivos**: todo se hace en la consola de Keycloak.

## 5. Evidencia de cierre de la S7b

- Captura del realm `lamb` → *Realm roles*.
- Captura del usuario con su rol asignado (*Role mapping*).
- En la S11 lo verás funcionando: `admin` puede modificar y `estudiante1` recibe "No tienes permiso".

Siguiente: [S8 - Mensajería asíncrona](S08_Mensajeria_Asincrona_Entre_Servicios.md).
