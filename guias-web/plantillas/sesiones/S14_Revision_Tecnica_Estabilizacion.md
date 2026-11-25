# S14 - Revisión técnica y estabilización del producto

*Guía única: agregar un microservicio nuevo al proyecto Lamb - Parte 14 de 16 - 2026*

!!! note "Antes de empezar: debe estar levantado"
    - Haber hecho la [S13](S13_Validacion_End_to_End_Producto.md): aquí se arregla lo que falló allí.

## 1. Problemas comunes

Resumen rápido. La explicación completa, con el archivo a abrir y qué cambiar, está en [Solución de errores](Solucion_Errores.md).

| Síntoma | Causa | Solución |
|---|---|---|
| `Could not resolve placeholder 'app.kafka.topic.evaluacionn'` | El servicio no encontró su configuración | Config Server apagado, o el archivo no se llama `spring.application.name` + `-dev.yml` (S2) |
| `Connection refused ... 15460` | La BD de tu servicio no está levantada | `docker compose -f services/lamb-evaluacionn-ms/compose-dev.yml up -d` |
| `Migration checksum mismatch` | Editaste un `V1__...sql` ya aplicado | Devuelve el V1 como estaba y crea un `V2`. En desarrollo: `docker compose -f ... down -v` (**borra los datos**) |
| `Schema-validation: missing column` | La entidad tiene un atributo que no está en la tabla | Agrega la columna con una migración `V2` o corrige el nombre (`metodoPago` ↔ `metodo_pago`) |
| `Port 8095 was already in use` | Otra instancia sigue corriendo | Cierra la otra terminal o usa otro puerto |
| `No qualifying bean of type '...ExamenMapper'` | Falta `mapstruct-processor` en el `pom.xml` | S1, paso 6.5 (en **las dos** `annotationProcessorPaths`) |
| Gateway `404` | Falta la ruta o no reiniciaste el Gateway | S4, paso 2 |
| Gateway `401` en un `GET` | Falta la regla `permitAll` | S7, paso 3 |
| Gateway `503` | Tu servicio aún no está en Eureka | Espera 30 s y revisa http://localhost:18761 |
| `403` siendo `admin` | El usuario no tiene el rol o su token es viejo | Revisa *Role mapping* (S7b) y vuelve a iniciar sesión |
| El consumidor no recibe / `ClassNotFoundException` del evento | `default.type` o `trusted.packages` mal escritos | S2, paso 3 y S8, paso 2.2 |
| No salen logs en Loki | Falta el job de Promtail o arrancaste el servicio en otra carpeta | S10, paso 2 |
| Frontend: pantalla en blanco | Error de compilación de Angular | Mira la terminal de `npm start` |
| `Unable to rename ... .jar` | El Gateway seguía corriendo al recompilar | Detén el Gateway y recompila |

## 2. Adaptarlo a otro caso: lista de verificación

Si te dan otro servicio (ej. **Tesorería** con **Pagos**), revisa que cambiaste todo:

| Dónde | Qué cambia | Sesión |
|---|---|---|
| Carpeta y `pom.xml` | `lamb-tesoreria-ms` | S1 |
| Paquete Java | `pe.edu.upeu.lamb.tesoreria` (en **todos** los archivos) | S1 |
| Clase principal | `LambTesoreriaMsApplication` | S1 |
| Tabla | `V1__create_pagos.sql` con tus atributos | S1 |
| Entidad, DTO, mapper, repositorio, servicio, controlador | `Pago...`, tus atributos, `/api/v1/pagos` | S1 |
| `logback-spring.xml` | `logs/tesoreria.log` | S1 |
| `compose-dev.yml` | nombres y puerto de la BD | S1 |
| `application.yml` | `name: lamb-tesoreria-ms` | S2 |
| `lamb-tesoreria-ms-dev.yml` | puerto, BD, group-id, tipo de evento, topic, logging | S2 |
| Gateway: ruta y seguridad | `lb://lamb-tesoreria-ms`, `/api/v1/tesoreria/**`, `/api/v1/pagos/**` | S4, S7 |
| Evento y Kafka | `TesoreriaEvento`, `tesoreria-eventos`, `pago.creado` | S8 |
| Promtail | `lamb-tesoreria-ms` | S10 |
| Frontend | `features/tesoreria`, `pago-*`, rutas `/tesoreria/pagos` | S11 |

Para encontrar lo que olvidaste, busca la palabra vieja en tu servicio (si no imprime nada, está limpio):

```powershell
Get-ChildItem services\lamb-tesoreria-ms\src -Recurse -File | Select-String -Pattern "evaluacionn|examen"
```

!!! note "Lo que nunca cambias"
    Puertos de Lamb (18888, 18761, 18080, 18092, 18180), realm `lamb`, prefijos `pe.edu.upeu.lamb` y `/api/v1/`,
    los archivos "copia tal cual" (`OpenApiConfig`, `CorrelationIdFilter`, `GlobalExceptionHandler`,
    `ResourceNotFoundException`, el productor) salvo su `package`, y los bloques `eureka` y `management` del `-dev.yml`.

## 3. Evidencia de cierre de la S14

Lista de los problemas que encontraste y cómo los resolviste. Ejecuta de nuevo la [S13](S13_Validacion_End_to_End_Producto.md) completa.

Siguiente: [S15 - Evaluación de la Unidad III](S15_Evaluacion_Unidad_3.md).
