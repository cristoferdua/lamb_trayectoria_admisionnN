package pe.edu.upeu.lamb.admision.entity;

/**
 * Ciclo de vida del postulante en el proceso de admision.
 *
 * PRE_INSCRITO     — llego por el wizard publico, aun sin docs ni pago
 * FORMALIZADO      — admin recibio sus docs fisicos y los subio
 * PAGADO           — Finanzas confirmo el pago del derecho de admision
 * APTO_EXAMEN      — listo para rendir (requiere docs OK + pago)
 * CALIFICADO       — rindio examen y ya tiene puntaje
 * ADMITIDO         — adjudicado a vacante
 * NO_ADMITIDO      — no alcanzo vacante
 * ANULADO          — retirado del proceso
 */
public enum EstadoPostulante {
    PRE_INSCRITO,
    FORMALIZADO,
    PAGADO,
    APTO_EXAMEN,
    CALIFICADO,
    ADMITIDO,
    NO_ADMITIDO,
    ANULADO
}
