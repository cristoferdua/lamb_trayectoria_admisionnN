# Resumen rápido: todos los archivos

*Guía única: agregar un microservicio nuevo al proyecto Lamb - Para quien ya sabe y solo quiere copiar - 2026*

Esta página junta **todo lo que tocas** en la guía, con la sesión donde está la **versión final** de cada archivo.
Si es tu primera vez, mejor sigue las sesiones en orden desde la [S1](S01_Construccion_Servicio_Base.md).

!!! note "Cuidado con los archivos que se reemplazan"
    `SecurityConfig.java` (del servicio), `SaludoController.java` y `ExamenService.java` tienen una versión
    temprana y una final. Copia **siempre la versión de la sesión que dice la tabla**.

## 1. Cómo queda tu servicio

```text
services\lamb-evaluacionn-ms\
├── compose-dev.yml                          S1
├── pom.xml  mvnw  mvnw.cmd  .mvn\           S1 (copiados de Trayectoria; + MapStruct en camino B)
└── src\main\
    ├── java\pe\edu\upeu\lamb\evaluacionn\
    │   ├── LambEvaluacionnMsApplication.java     S1
    │   ├── config\
    │   │   ├── SecurityConfig.java              S7  (versión final)
    │   │   ├── OpenApiConfig.java               S1
    │   │   └── KafkaTopicsConfig.java           S8
    │   ├── filter\CorrelationIdFilter.java      S1
    │   ├── event\EvaluacionnEvento.java          S8
    │   ├── messaging\
    │   │   ├── EvaluacionnEventosProducer.java   S8
    │   │   └── EvaluacionnEventosConsumer.java   S8
    │   ├── controller\
    │   │   ├── SaludoController.java            S8  (versión final)
    │   │   └── ExamenController.java             S1  (B)
    │   ├── entity\Examen.java                    S1  (B)
    │   ├── dto\ExamenRequest.java                S1  (B)
    │   ├── dto\ExamenResponse.java               S1  (B)
    │   ├── mapper\ExamenMapper.java              S1  (B)
    │   ├── repository\ExamenRepository.java      S1  (B)
    │   ├── exception\ResourceNotFoundException.java   S1  (B)
    │   ├── exception\GlobalExceptionHandler.java      S1  (B)
    │   └── service\ExamenService.java            S8  (B, versión final)
    └── resources\
        ├── application.yml                      S2
        ├── logback-spring.xml                   S1
        └── db\migration\V1__create_examenes.sql   S1  (B)
```

`(B)` = solo si elegiste el camino **con CRUD**.

## 2. Archivos fuera de tu servicio

| Archivo | Qué haces | Sesión |
|---|---|---|
| `infra\lamb-config\config-repo\lamb-evaluacionn-ms-dev.yml` | Creas (toda la configuración) | [S2](S02_Configuracion_Centralizada_Ambientes.md) |
| `infra\lamb-config\config-repo\lamb-gateway-dev.yml` | Agregas 1 ruta (A) o 2 rutas (B) | [S4](S04_Punto_Unico_Acceso_Distribucion_Trafico.md) |
| `infra\lamb-gateway\...\config\SecurityConfig.java` (Gateway) | Agregas las reglas por rol | [S7](S07_Seguridad_Distribuida_Control_Acceso.md) |
| `obs\promtail\promtail-config-dev.yml` | Agregas 1 job | [S10](S10_Observabilidad_Diagnostico_Sistemas_Distribuidos.md) |
| `clients\lamb-frontend\src\app\features\evaluacionn\` | Creas las páginas | [S11](S11_Integracion_Cliente_Frontend_Angular_Keycloak.md) |
| `clients\lamb-frontend\src\app\app.routes.ts` | Agregas 1 ruta (A) o 4 rutas (B) | [S11](S11_Integracion_Cliente_Frontend_Angular_Keycloak.md) |
| Keycloak (consola web) | Solo si te piden un rol o usuario nuevo | [S7b](S07b_Seguridad_Distribuida_Keycloak.md) |

## 3. Frontend

```text
clients\lamb-frontend\src\app\features\evaluacionn\
├── evaluacionn-service.ts     S11
├── saludo.ts                 S11
├── saludo.html               S11  (camino A: sin el link "Ver examenes")
├── examen.model.ts            S11  (B)
├── examen-service.ts          S11  (B)
├── examen-list.ts / .html     S11  (B)
└── examen-form.ts / .html     S11  (B)
```

## 4. Pasos en orden (lista corta)

| N. | Paso | Sesión |
|---|---|---|
| 1 | Elegir nombres (servicio, recurso, atributos, puertos) | [S1](S01_Construccion_Servicio_Base.md#2-elige-tus-nombres) |
| 2 | Crear el proyecto copiando Trayectoria | [S1](S01_Construccion_Servicio_Base.md#3-crear-el-proyecto) |
| 3 | Código común + `SaludoController` | [S1](S01_Construccion_Servicio_Base.md#4-codigo-comun-los-dos-caminos) |
| 4 | (B) Tabla, entidad, DTO, mapper, repositorio, excepciones, servicio, controlador | [S1](S01_Construccion_Servicio_Base.md#6-camino-b-con-crud-tabla-entidad-dto-mapper-servicio-y-controlador) |
| 5 | BD en Docker y compilar | [S1](S01_Construccion_Servicio_Base.md#7-base-de-datos-en-docker-los-dos-caminos) |
| 6 | `application.yml` + `-dev.yml` en el Config Server | [S2](S02_Configuracion_Centralizada_Ambientes.md) |
| 7 | Arrancar y verlo en Eureka | [S3](S03_Registro_Descubrimiento_Ejecucion_Concurrente.md) |
| 8 | Ruta en el Gateway | [S4](S04_Punto_Unico_Acceso_Distribucion_Trafico.md) |
| 9 | `SecurityConfig` final + roles en el Gateway | [S7](S07_Seguridad_Distribuida_Control_Acceso.md) |
| 10 | Kafka: topic, evento, productor, consumidor; reemplazar `SaludoController` (y `ExamenService` en B) | [S8](S08_Mensajeria_Asincrona_Entre_Servicios.md) |
| 11 | Job de Promtail | [S10](S10_Observabilidad_Diagnostico_Sistemas_Distribuidos.md) |
| 12 | Frontend y rutas | [S11](S11_Integracion_Cliente_Frontend_Angular_Keycloak.md) |
| 13 | Probar todo | [S13](S13_Validacion_End_to_End_Producto.md) |

Si algo falla: [Solución de errores](Solucion_Errores.md). Todos los links: [Enlaces del sistema](Enlaces_Sistema.md).
