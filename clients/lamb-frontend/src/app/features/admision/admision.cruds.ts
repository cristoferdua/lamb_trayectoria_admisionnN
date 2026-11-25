import { CrudDef, f } from '../crud-generic/crud.types';

export const ADMISION_CRUDS: CrudDef[] = [
  {
    group: 'Admisión', slug: 'convocatorias', label: 'Convocatoria y oferta',
    icon: 'campaign', path: '/api/v1/convocatorias',
    description: 'Proceso de admisión, modalidad, vacantes y cronograma del periodo',
    fields: [
      f('id', 'ID', 'id', { editable: false }),
      f('codigo', 'Código', 'text', { required: true }),
      f('nombre', 'Nombre', 'text', { required: true }),
      f('periodo', 'Periodo', 'text', { required: true, default: '2026-2' }),
      f('fechaInicio', 'Inicio', 'date', { required: true }),
      f('fechaFin', 'Fin', 'date', { required: true }),
      f('fechaExamen', 'Fecha examen', 'date'),
      f('vacantesTotales', 'Vacantes', 'number', { required: true, default: 0 }),
      f('estado', 'Estado', 'enum', { required: true, options: ['PLANIFICADA','ABIERTA','CERRADA','ANULADA'], default: 'PLANIFICADA' }),
      f('observacion', 'Observación', 'textarea', { listed: false })
    ]
  },
  {
    group: 'Admisión', slug: 'postulantes', label: 'Postulante',
    icon: 'person', path: '/api/v1/postulantes',
    description: 'Datos del postulante, apoderado y colegio de procedencia',
    createVia: '/postular',
    rowDownload: {
      label: 'Descargar carnet PDF',
      icon: 'picture_as_pdf',
      path: '/api/v1/postulantes/{id}/carnet.pdf',
      filename: 'carnet-{codigo}.pdf',
      codeField: 'codigoPostulante'
    },
    fields: [
      f('id', 'ID', 'id', { editable: false }),
      f('__reniec', 'Buscar por DNI (RENIEC)', 'reniec', { listed: false }),
      f('idPersona', 'ID Persona', 'number', { required: true }),
      f('idConvocatoria', 'ID Convocatoria', 'number', { required: true }),
      f('idPrograma', 'ID Programa', 'number', { required: true }),
      f('idModalidad', 'ID Modalidad', 'number', { required: true }),
      // Código autogenerado por backend: POS-{periodo}-{correlativo}. No se edita.
      f('codigoPostulante', 'Código', 'text', { editable: false }),
      f('colegioProcedencia', 'Colegio', 'text'),
      f('anioEgresoColegio', 'Año egreso', 'number'),
      f('promedioColegio', 'Promedio', 'decimal'),
      f('telefonoContacto', 'Teléfono', 'text'),
      f('correoContacto', 'Correo', 'text'),
      f('nombreApoderado', 'Apoderado', 'text'),
      f('telefonoApoderado', 'Tel. apoderado', 'text', { listed: false }),
      f('estado', 'Estado', 'enum', { required: true, options: ['PRE_INSCRITO','FORMALIZADO','PAGADO','APTO_EXAMEN','CALIFICADO','ADMITIDO','NO_ADMITIDO','ANULADO'], default: 'PRE_INSCRITO' })
    ]
  },
  {
    group: 'Admisión', slug: 'examenes-admision', label: 'Examen de admisión',
    icon: 'quiz', path: '/api/v1/examenes-admision',
    description: 'Examen, aula asignada, aforo y estado',
    fields: [
      f('id', 'ID', 'id', { editable: false }),
      f('idConvocatoria', 'ID Convocatoria', 'number', { required: true }),
      f('codigo', 'Código', 'text', { required: true }),
      f('nombre', 'Nombre', 'text', { required: true }),
      f('fechaExamen', 'Fecha', 'datetime', { required: true }),
      f('duracionMinutos', 'Duración (min)', 'number', { required: true, default: 120 }),
      f('idAula', 'ID Aula', 'number'),
      f('aforoMaximo', 'Aforo máx.', 'number', { required: true, default: 30 }),
      f('aforoAsignado', 'Aforo asignado', 'number', { required: true, default: 0 }),
      f('estado', 'Estado', 'enum', { required: true, options: ['PROGRAMADO','EN_CURSO','FINALIZADO','CANCELADO'], default: 'PROGRAMADO' })
    ]
  },
  {
    group: 'Admisión', slug: 'seguimientos', label: 'Seguimiento y cierre',
    icon: 'timeline', path: '/api/v1/seguimientos',
    description: 'Contactos, medios de captación y reclamos',
    fields: [
      f('id', 'ID', 'id', { editable: false }),
      f('idPostulante', 'ID Postulante', 'number', { required: true }),
      f('canal', 'Canal', 'enum', { required: true, options: ['LLAMADA','WHATSAPP','CORREO','PRESENCIAL'], default: 'LLAMADA' }),
      f('medioCaptacion', 'Medio captación', 'text'),
      f('asunto', 'Asunto', 'text', { required: true }),
      f('detalle', 'Detalle', 'textarea', { listed: false }),
      f('resultado', 'Resultado', 'enum', { required: true, options: ['CONTACTADO','SIN_RESPUESTA','INTERESADO','DESCARTADO'], default: 'CONTACTADO' }),
      f('idResponsable', 'ID Responsable', 'number'),
      f('fechaContacto', 'Fecha contacto', 'datetime', { required: true }),
      f('proximoContacto', 'Próximo contacto', 'datetime', { listed: false })
    ]
  },
  {
    group: 'Admisión', slug: 'calificaciones', label: 'Examen y calificación',
    icon: 'grade', path: '/api/v1/calificaciones',
    description: 'Puntajes del examen, supervisor y encargado',
    fields: [
      f('id', 'ID', 'id', { editable: false }),
      f('idExamen', 'ID Examen', 'number', { required: true }),
      f('idPostulante', 'ID Postulante', 'number', { required: true }),
      f('puntajeTotal', 'Puntaje total', 'decimal', { required: true }),
      f('puntajeAptitud', 'Aptitud', 'decimal'),
      f('puntajeConocimiento', 'Conocimiento', 'decimal'),
      f('respuestasCorrectas', 'Correctas', 'number'),
      f('respuestasIncorrectas', 'Incorrectas', 'number'),
      f('respuestasBlanco', 'Blanco', 'number', { listed: false }),
      f('ordenMerito', 'Orden mérito', 'number'),
      f('idSupervisor', 'ID Supervisor', 'number'),
      f('idEncargado', 'ID Encargado', 'number'),
      f('observacion', 'Observación', 'textarea', { listed: false })
    ]
  },
  {
    group: 'Admisión', slug: 'adjudicaciones', label: 'Vacantes e ingreso',
    icon: 'verified', path: '/api/v1/adjudicaciones',
    description: 'Adjudicación de vacantes y lista de ingresantes',
    fields: [
      f('id', 'ID', 'id', { editable: false }),
      f('idPostulante', 'ID Postulante', 'number', { required: true }),
      f('idModalidad', 'ID Modalidad', 'number', { required: true }),
      f('idPrograma', 'ID Programa', 'number', { required: true }),
      f('puntajeFinal', 'Puntaje final', 'decimal', { required: true }),
      f('ordenMerito', 'Orden mérito', 'number'),
      f('estado', 'Estado', 'enum', { required: true, options: ['PENDIENTE','ADJUDICADA','PUBLICADA','ANULADA'], default: 'PENDIENTE' }),
      f('numeroResolucion', 'N° Resolución', 'text'),
      f('fechaPublicacion', 'Publicación', 'datetime', { listed: false }),
      f('observacion', 'Observación', 'textarea', { listed: false })
    ]
  },
  {
    group: 'Admisión', slug: 'casos-especiales', label: 'Casos especiales',
    icon: 'warning', path: '/api/v1/casos-especiales',
    description: 'Ficha socioeconómica, discapacidad, prórrogas, convalidación, anulación',
    fields: [
      f('id', 'ID', 'id', { editable: false }),
      f('idPostulante', 'ID Postulante', 'number', { required: true }),
      f('tipoCaso', 'Tipo', 'enum', { required: true, options: ['FICHA_SOCIOECONOMICA','DISCAPACIDAD','PRORROGA_EXAMEN','CONVALIDACION_TRASLADO','ANULACION_INSCRIPCION','CARGA_MASIVA','RECLAMO_APELACION'], default: 'FICHA_SOCIOECONOMICA' }),
      f('descripcion', 'Descripción', 'textarea', { required: true }),
      f('requiereAjuste', 'Requiere ajuste', 'boolean', { required: true, default: false }),
      f('ajusteOtorgado', 'Ajuste otorgado', 'textarea', { listed: false }),
      f('nivelSocioeconomico', 'Nivel socioec.', 'enum', { options: ['A','B','C','D','E'], listed: false }),
      f('idResponsable', 'ID Responsable', 'number'),
      f('estado', 'Estado', 'enum', { required: true, options: ['ABIERTO','EN_REVISION','RESUELTO','RECHAZADO'], default: 'ABIERTO' })
    ]
  }
];
