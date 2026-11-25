# Enlaces del sistema

*Guía única: agregar un microservicio nuevo al proyecto Lamb - Todos los links en localhost - 2026*

Todos los links del proyecto Lamb en desarrollo. Solo funcionan si el componente está levantado
(orden de arranque en la [S13](S13_Validacion_End_to_End_Producto.md)).

## 1. Infraestructura

| Componente | Link | Usuario / contraseña | Para qué |
|---|---|---|---|
| Config Server | http://localhost:18888/lamb-evaluacionn-ms/dev | — | Ver la configuración de un servicio |
| Config Server (Gateway) | http://localhost:18888/lamb-gateway/dev | — | Ver las rutas del Gateway |
| Eureka | http://localhost:18761 | — | Servicios registrados e instancias |
| Gateway | http://localhost:18080 | — | Punto único de acceso a la API |
| Gateway - salud | http://localhost:18080/actuator/health | — | `UP` si el Gateway está bien |

## 2. Seguridad

| Componente | Link | Usuario / contraseña | Para qué |
|---|---|---|---|
| Keycloak - consola | http://localhost:18180/admin/master/console/#/lamb | `admin` / `admin123` | Usuarios, roles y clientes del realm `lamb` |
| Keycloak - realm `lamb` | http://localhost:18180/realms/lamb | — | `issuer-uri` que usan los servicios |
| Keycloak - claves públicas | http://localhost:18180/realms/lamb/protocol/openid-connect/certs | — | Claves con las que se valida el token |
| Keycloak - configuración OpenID | http://localhost:18180/realms/lamb/.well-known/openid-configuration | — | Endpoints de login y token |

Usuarios de prueba del frontend: `admin` / `admin123`, `docente1` / `docente123`, `estudiante1` / `estudiante123`.

## 3. Mensajería

| Componente | Link | Para qué |
|---|---|---|
| Kafka UI | http://localhost:18090 | Topics, mensajes y consumidores |
| Kafka UI - topics | http://localhost:18090/ui/clusters/lamb-dev/all-topics | Lista de topics (`evaluacionn-eventos`, `admision-eventos`...) |
| Kafka (broker) | `localhost:18092` | No se abre en el navegador: lo usan los servicios |

## 4. Observabilidad

| Componente | Link | Usuario / contraseña | Para qué |
|---|---|---|---|
| Grafana | http://localhost:13000 | `admin` / `admin` | Paneles, logs y métricas |
| Grafana - Explore | http://localhost:13000/explore | `admin` / `admin` | Consultas a Loki y Prometheus (S10) |
| Grafana - dashboard Lamb | http://localhost:13000/d/lamb-infra | `admin` / `admin` | *Lamb - Infraestructura* |
| Prometheus | http://localhost:19090 | — | Métricas |
| Prometheus - targets | http://localhost:19090/targets | — | Qué servicios está leyendo (UP / DOWN) |
| Loki - servicios con logs | http://localhost:13100/loki/api/v1/label/application/values | — | Lista de servicios que ya mandan logs (debe salir el tuyo) |

## 5. Tu servicio nuevo (ejemplo `lamb-evaluacionn-ms`)

Cambia `8095`, `evaluacionn` y `examenes` por los tuyos.

| Qué | Link |
|---|---|
| Hola mundo (por Gateway) | http://localhost:18080/api/v1/evaluacionn/saludo |
| CRUD (por Gateway, camino B) | http://localhost:18080/api/v1/examenes |
| Hola mundo (directo) | http://localhost:8095/api/v1/evaluacionn/saludo |
| CRUD (directo, camino B) | http://localhost:8095/api/v1/examenes |
| Swagger | http://localhost:8095/swagger-ui.html |
| Salud | http://localhost:8095/actuator/health |
| Métricas | http://localhost:8095/actuator/prometheus |
| Frontend - Hola mundo | http://localhost:4200/evaluacionn |
| Frontend - CRUD (camino B) | http://localhost:4200/evaluacionn/examenes |
| Base de datos | `localhost:15460` · BD `lamb_evaluacionn_db` · `lamb` / `lamb` |

## 6. Servicios de Lamb (Admisión y Trayectoria)

| Servicio | Swagger | Salud | Base de datos |
|---|---|---|---|
| `lamb-admision-ms` (8084) | http://localhost:8084/swagger-ui.html | http://localhost:8084/actuator/health | `localhost:15454` · `lamb_admision_db` · `lamb` / `lamb` |
| `lamb-trayectoria-ms` (8089) | http://localhost:8089/swagger-ui.html | http://localhost:8089/actuator/health | `localhost:15459` · `lamb_trayectoria_db` · `lamb` / `lamb` |

Para ver estas bases de datos en VS Code (extensión **Database Client**), usa Host `127.0.0.1` y el puerto, BD, usuario y contraseña de la tabla. Pasos en la [S2](S02_Configuracion_Centralizada_Ambientes.md#5-ver-la-base-de-datos-en-vs-code).

## 7. Frontend

| Página | Link |
|---|---|
| Inicio | http://localhost:4200 |
| Postulación | http://localhost:4200/postular |
| Seguimiento | http://localhost:4200/seguimiento |
| Panel de administración (pide login) | http://localhost:4200/admin |
| Tu servicio | http://localhost:4200/evaluacionn |

## 8. Puertos

| Puerto | Componente |
|---|---|
| 4200 | Frontend Angular |
| 8081–8089 | Microservicios de Lamb (8084 Admisión, 8089 Trayectoria) |
| 8095 | Tu servicio (ejemplo) |
| 13000 | Grafana |
| 13100 | Loki |
| 15436 | BD de Keycloak |
| 15451–15459 | BDs de los servicios de Lamb |
| 15460 | BD de tu servicio (ejemplo) |
| 18080 | Gateway |
| 18090 | Kafka UI |
| 18092 | Kafka |
| 18180 | Keycloak |
| 18761 | Eureka |
| 18888 | Config Server |
| 19090 | Prometheus |
