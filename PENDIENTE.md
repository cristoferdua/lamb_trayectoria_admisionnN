# PENDIENTE — Modulo 02 Admision / Trayectoria

Lista de cosas por arreglar o completar. Prioridad de arriba hacia abajo.

---

## Bugs a arreglar

### CRITICO

**[HECHO 2026-11-11] 1. `PagoReceivedListener` — Estado PAGADO nunca se persiste**
- Archivo: `services/lamb-admision-ms/src/main/java/pe/edu/upeu/lamb/admision/service/PagoReceivedListener.java`
- Problema: dos `setEstado()` consecutivos (PAGADO -> APTO_EXAMEN) sin flush. El primero se pisa en memoria, en BD nunca queda PAGADO. El timeline lo muestra verde por ordinal pero no hay rastro real.
- Fix: dejar solo `setEstado(APTO_EXAMEN)` con comentario explicando que PAGADO es una transicion conceptual intermedia.

**[HECHO 2026-11-11] 2. Wizard — signals de campos eliminados quedan muertas**
- Archivo: `clients/lamb-frontend/src/app/features/admision/wizard-postulacion.component.ts`
- Problema: hay signals `colegio`, `anioEgreso`, `promedio`, `apoderado`, `telefonoApoderado` que ya no se envian en el payload. Dead code.
- Fix: borrar los signals, borrar sus resets en `nuevo()`, borrar imports no usados.

### MEDIO

**3. Cargos en memoria se pierden al reiniciar admision-ms**
- Archivo: `services/lamb-admision-ms/src/main/java/pe/edu/upeu/lamb/admision/adapter/FinanzasInMemoryAdapter.java`
- Problema: al reiniciar, los postulantes en FORMALIZADO quedan sin cargo asociado -> bloqueados.
- Fix temporal: persistir los cargos en una tabla `cargos_admision` dentro del adapter fake.
- Fix definitivo: cuando exista ms Finanzas, cambiar a `FinanzasHttpAdapter`.

**[HECHO 2026-11-11] 4. `generarCodigo()` — race condition con concurrencia**
- Archivo: `PostulacionWizardService.java` linea ~138
- Problema: `findByConvocatoriaId(...).size()+1` dos postulantes simultaneos pueden generar el mismo codigo.
- Fix: usar una secuencia dedicada en BD o `countByConvocatoriaId` + unique constraint + retry.

**[HECHO 2026-11-11] 5. `ProspectosComponent` filtra 4 estados pero el titulo dice "PRE_INSCRITO"**
- Archivo: `clients/lamb-frontend/src/app/features/admin/prospectos.component.ts`
- Fix: renombrar titulo a "Bandeja de admision" o filtrar solo PRE_INSCRITO.

**[HECHO 2026-11-11] 6. Evento `postulante.pre-inscrito` envia `puntajeExamen = BigDecimal.ZERO`**
- Archivo: `PostulacionWizardService.java` linea ~96
- Fix: enviar `null` en vez de 0 (aun no hay examen).

### BAJO

**[HECHO 2026-11-11] 7. Campo `FinanzasPort port` sin uso en `PagoWebPublicoController`**
- Fix: borrar el campo.

**[HECHO 2026-11-11] 8. `pagar.component.ts` y `derecho-admision.component.ts` no manejan error en la accion de pago**
- Fix: agregar `error:` callback que muestre mensaje al usuario.

**[HECHO 2026-11-11] 9. `TimelinePublicoService.ordinal` mapea estados viejos (REGISTRADO, HABILITADO, EVALUADO) que ya no se usan**
- Fix: quitar o dejar solo para compatibilidad historica.

**[HECHO 2026-11-11] 10. Enum `EstadoPostulante` conserva estados viejos (REGISTRADO, HABILITADO, EVALUADO) "por compatibilidad"**
- Fix: evaluar si eliminarlos. Si no hay datos con esos estados en BD de demo, borrarlos.

---

## Features faltantes del flujo

### Admin

**11. Pantalla "Completar postulacion" (al formalizar)**
- Hoy `POST /postulantes/{id}/formalizar` no pide nada. En el flujo real el admin recibe los documentos del postulante en oficina, llena datos academicos faltantes (colegio, año egreso, promedio, apoderado) y sube los documentos.
- A implementar:
  - DTO `CompletarPostulacionRequest` con: colegio, anioEgreso, promedio, apoderado, telefonoApoderado, lista de documentos subidos.
  - Modal en `/admin/prospectos` con form + upload de archivos (DNI, certificado estudios, partida, foto).
  - Endpoint `POST /api/v1/postulantes/{id}/completar` que combine: actualizar datos + guardar documentos + formalizar + crear cargo.

**12. Pantalla `/admin/postulantes` con semaforo de colores por estado**
- PRE_INSCRITO (amarillo), FORMALIZADO (naranja), APTO_EXAMEN (verde), CALIFICADO (azul), ADMITIDO / NO_ADMITIDO.
- Hoy solo hay bandeja de prospectos.

**13. Pantalla / endpoint de **adjudicacion**
- Boton "Correr adjudicacion" por convocatoria + modalidad + programa.
- Ordena postulantes CALIFICADO por puntaje, asigna vacantes hasta llenar cupo.
- Marca los que entran como ADMITIDO, el resto como NO_ADMITIDO.
- Publica evento `PostulanteAdmitido` a Trayectoria.

### Postulante

**14. Correo con opciones de pago despues de formalizar**
- Plantilla `plantillaOpcionesDePago(nombre, codigo, monto)` con:
  - Opcion 1: Caja UPeU (ir con DNI)
  - Opcion 2: Yape al 999-999-999
  - Opcion 3: Transferencia BCP cuenta XXX
  - Opcion 4: Mercado Pago (link)
- Enviar via `EmailService` tras `FormalizacionService.formalizar()`.

**15. Correo "pago recibido" cuando llega el evento de Finanzas**
- Plantilla `plantillaPagoRecibido(nombre, codigo, fechaExamen, aula)`.
- Enviar desde `PagoReceivedListener.onCargoPagado()` tras promover a APTO_EXAMEN.

**16. Correo "resultado de admision" tras adjudicacion**
- Admitido: felicidades + siguientes pasos para matricula.
- No admitido: gracias + opciones (segunda convocatoria).

---

## Integracion con otros modulos

**17. Evento `PostulanteAdmitido` -> Trayectoria**
- Cuando un postulante pasa a ADMITIDO, publicar evento Kafka en topico `admision-eventos`.
- Trayectoria debe crear registro `Trayectoria` inicial para ese estudiante.
- Hoy el evento se publica en pre-inscripcion con tipo `postulante.pre-inscrito`. Falta el evento `postulante.admitido`.

**18. Adaptador HTTP real para `FinanzasPort`**
- Cuando exista `lamb-finanzas-ms`, crear `FinanzasHttpAdapter implements FinanzasPort` con WebClient.
- Marcar `@Primary` o eliminar `FinanzasInMemoryAdapter`.
- Agregar `@KafkaListener(topic="finanzas.cargo-pagado")` en `PagoReceivedListener` (hoy lo invoca el controller fake).

**19. Adaptador HTTP real para `PersonaPort`**
- Cuando exista `lamb-personas-ms`, crear `PersonaHttpAdapter` con WebClient.
- Migrar datos de `personas_admision` al ms oficial (script SQL unico).
- Eliminar `PersonaLocalAdapter` y entidad local `Persona`.

**20. Catalogo (sedes, programas) migrable a `lamb-institucional-ms` / `lamb-curriculo-ms`**
- Hoy viven en `CatalogoSeeder`. Cuando existan los ms Institucional y Curriculo, migrar a clientes HTTP y eliminar el seeder + entidades `Sede` + `ProgramaAcademico`.

---

## Seguridad / produccion

**21. Keycloak — roles reales**
- Hoy el guard admin solo verifica `authenticated()`. Falta chequear que el usuario tenga el rol `ADMIN`.
- Fix: `kc.hasRole('ADMIN')` en el guard.

**22. Gateway — cerrar permitAll de admision**
- Hoy admision esta todo `permitAll` ("DEV: lectura y escritura de admision sin token"). En produccion debe exigir `hasRole('ADMIN')` para los endpoints administrativos.
- Mantener publicos solo: `/api/v1/public/**`, `/api/v1/admision/postulacion-wizard`, `/api/v1/admision/reniec/**`.

**23. Rate limiting en endpoints publicos**
- `/postular`, `/pagar`, `/seguimiento` son publicos. Agregar rate limit por IP para evitar abuso.

**24. HTTPS / cors productivo**
- CORS hoy permite `localhost:4200`. En prod debe apuntar al dominio real.

---

## Arquitectura / limpieza

**25. Hibernate ddl-auto en produccion**
- Hoy esta en `update`. En prod debe ser `validate` + migraciones Flyway/Liquibase.

**26. `@JsonIgnore` en relaciones bidireccionales**
- Fix aplicado en `ModalidadAdmision.documentosRequeridos`. Revisar que no haya mas lugares con recursion infinita en JSON.

**27. Pantalla de resultados de examen / adjudicacion para el postulante**
- En `/seguimiento` mostrar el puntaje final y resultado de adjudicacion cuando esten disponibles.

---

## Para probar el flujo end-to-end

Flujo esperado (una vez arreglados los bugs criticos):

1. Reiniciar `lamb-admision-ms` y `lamb-gateway`.
2. `http://localhost:4200/postular` → llenar wizard con DNI real RENIEC.
3. `http://localhost:4200/admin/prospectos` (login Keycloak) → ver prospecto → click "Formalizar".
4. `http://localhost:4200/pagar` → DNI del postulante → click "Pagar con Yape".
   O: `/admin/derecho-admision` → "Confirmar pago en caja".
5. `/admin/crud/examenes-admision` → crear examen.
6. `/admin/crud/calificaciones` → registrar calificacion (deberia bloquear si no esta APTO_EXAMEN).
7. `/seguimiento` → DNI → ver timeline completo.

---

Fecha: 2026-11-10 (ultima actualizacion)
