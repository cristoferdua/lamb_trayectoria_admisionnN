-- Actualiza el CHECK de estado del postulante al nuevo ciclo de vida.
-- Ver EstadoPostulante.java: PRE_INSCRITO, FORMALIZADO, PAGADO, APTO_EXAMEN,
-- CALIFICADO, ADMITIDO, NO_ADMITIDO, ANULADO.

-- Normalizar datos legacy si existen.
UPDATE postulantes SET estado = 'PRE_INSCRITO' WHERE estado = 'REGISTRADO';
UPDATE postulantes SET estado = 'FORMALIZADO'  WHERE estado = 'HABILITADO';
UPDATE postulantes SET estado = 'CALIFICADO'   WHERE estado = 'EVALUADO';

ALTER TABLE postulantes DROP CONSTRAINT IF EXISTS ck_postulante_estado;
ALTER TABLE postulantes
    ADD CONSTRAINT ck_postulante_estado
    CHECK (estado IN (
        'PRE_INSCRITO','FORMALIZADO','PAGADO','APTO_EXAMEN',
        'CALIFICADO','ADMITIDO','NO_ADMITIDO','ANULADO'
    ));
