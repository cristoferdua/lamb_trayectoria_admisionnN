import { CrudDef, f } from '../crud-generic/crud.types';

export const TRAYECTORIA_CRUDS: CrudDef[] = [
  {
    group: 'Trayectoria', slug: 'trayectorias', label: 'Estudiante en el plan',
    icon: 'route', path: '/api/v1/trayectorias',
    description: 'Estado académico, cambios de plan, traslados internos y reincorporaciones',
    fields: [
      f('id', 'ID', 'id', { editable: false }),
      f('idPersona', 'ID Persona', 'number', { required: true }),
      f('idPrograma', 'ID Programa', 'number', { required: true }),
      f('idPostulacion', 'ID Postulación', 'number', { required: true }),
      f('idPlan', 'ID Plan', 'number'),
      f('idEscuela', 'ID Escuela', 'number', { listed: false }),
      f('idSede', 'ID Sede', 'number', { listed: false }),
      f('periodoIngreso', 'Periodo ingreso', 'text', { required: true, default: '2026-2' }),
      f('estado', 'Estado', 'enum', { required: true, options: ['ACTIVO','RESERVA','RETIRADO','SEPARADO','EGRESADO','REINCORPORADO'], default: 'ACTIVO' }),
      f('creditosTotalesPlan', 'Cr. plan', 'number', { listed: false }),
      f('creditosAprobados', 'Cr. aprob.', 'number', { required: true, default: 0 }),
      f('promedioPonderado', 'Promedio', 'decimal')
    ]
  },
  {
    group: 'Trayectoria', slug: 'cumplimientos-requisito', label: 'Avance y requisitos',
    icon: 'checklist', path: '/api/v1/cumplimientos-requisito',
    description: 'Convalidaciones, cumplimiento de requisitos de egreso, formación integral y prácticas',
    fields: [
      f('id', 'ID', 'id', { editable: false }),
      f('idTrayectoria', 'ID Trayectoria', 'number', { required: true }),
      f('idPersona', 'ID Persona', 'number', { required: true }),
      f('tipoRequisito', 'Tipo', 'enum', { required: true, options: ['REQUISITO_EGRESO','IDIOMA','PRACTICAS','INVESTIGACION','EXTRACURRICULAR','FORMACION_INTEGRAL','CONVALIDACION'], default: 'REQUISITO_EGRESO' }),
      f('idRequisitoExterno', 'ID Req. externo', 'number'),
      f('codigoExterno', 'Código', 'text'),
      f('descripcion', 'Descripción', 'textarea', { listed: false }),
      f('cumplido', 'Cumplido', 'boolean', { required: true, default: false }),
      f('valorCumplimiento', 'Valor', 'text', { listed: false }),
      f('creditosReconocidos', 'Cr. reconocidos', 'number'),
      f('fechaCumplimiento', 'Fecha', 'date'),
      f('evidenciaUrl', 'Evidencia', 'text', { listed: false })
    ]
  },
  {
    group: 'Trayectoria', slug: 'alertas-riesgo', label: 'Riesgo académico',
    icon: 'warning', path: '/api/v1/alertas-riesgo',
    description: 'Alertas, planes de refuerzo, reglas, niveles, casos de seguimiento y control de repitencia',
    fields: [
      f('id', 'ID', 'id', { editable: false }),
      f('idTrayectoria', 'ID Trayectoria', 'number', { required: true }),
      f('idPersona', 'ID Persona', 'number', { required: true }),
      f('nivelRiesgo', 'Nivel', 'enum', { required: true, options: ['BAJO','MEDIO','ALTO','CRITICO'], default: 'MEDIO' }),
      f('tipoAlerta', 'Tipo', 'enum', { required: true, options: ['PROMEDIO_BAJO','REPITENCIA','INASISTENCIA','ABANDONO','DESEMPENIO','OTRO'], default: 'PROMEDIO_BAJO' }),
      f('descripcion', 'Descripción', 'textarea', { required: true }),
      f('valorDetectado', 'Valor', 'decimal'),
      f('umbralRegla', 'Umbral', 'decimal', { listed: false }),
      f('numeroRepitencia', 'N° repitencia', 'number', { listed: false }),
      f('codigoCurso', 'Curso', 'text'),
      f('estado', 'Estado', 'enum', { required: true, options: ['ACTIVA','EN_INTERVENCION','CERRADA','DESCARTADA'], default: 'ACTIVA' }),
      f('fechaDeteccion', 'Detectada', 'datetime', { required: true })
    ]
  },
  {
    group: 'Trayectoria', slug: 'tutorias', label: 'Tutoría y bienestar',
    icon: 'school', path: '/api/v1/tutorias',
    description: 'Programa de tutoría, asignación de tutor, sesiones, observaciones y derivación a bienestar',
    fields: [
      f('id', 'ID', 'id', { editable: false }),
      f('idTrayectoria', 'ID Trayectoria', 'number', { required: true }),
      f('idPersona', 'ID Persona', 'number', { required: true }),
      f('idTutor', 'ID Tutor', 'number', { required: true }),
      f('idProgramaTutoria', 'ID Programa', 'number'),
      f('programaTutoria', 'Programa', 'text'),
      f('periodo', 'Periodo', 'text', { required: true, default: '2026-2' }),
      f('estado', 'Estado', 'enum', { required: true, options: ['ACTIVA','CERRADA','DERIVADA_BIENESTAR'], default: 'ACTIVA' }),
      f('fechaInicio', 'Inicio', 'date', { required: true }),
      f('fechaFin', 'Fin', 'date', { listed: false })
    ]
  },
  {
    group: 'Trayectoria', slug: 'reconocimientos', label: 'Logros y evidencias',
    icon: 'military_tech', path: '/api/v1/reconocimientos',
    description: 'Reconocimientos (tercio superior, beca de excelencia, mención) y portafolio de competencias',
    fields: [
      f('id', 'ID', 'id', { editable: false }),
      f('idTrayectoria', 'ID Trayectoria', 'number', { required: true }),
      f('idPersona', 'ID Persona', 'number', { required: true }),
      f('tipoReconocimiento', 'Tipo', 'enum', { required: true, options: ['TERCIO_SUPERIOR','BECA_EXCELENCIA','MENCION_HONROSA','PORTAFOLIO','COMPETENCIA','OTRO'], default: 'TERCIO_SUPERIOR' }),
      f('descripcion', 'Descripción', 'textarea', { required: true }),
      f('periodo', 'Periodo', 'text'),
      f('idCompetencia', 'ID Competencia', 'number', { listed: false }),
      f('autoevaluacion', 'Autoevaluación', 'textarea', { listed: false }),
      f('evidenciaUrl', 'Evidencia', 'text', { listed: false })
    ]
  },
  {
    group: 'Trayectoria', slug: 'egresos', label: 'Cierre y egreso',
    icon: 'workspace_premium', path: '/api/v1/egresos',
    description: 'Egreso con verificación de requisitos, solicitud de constancia, situación al cierre e informe',
    fields: [
      f('id', 'ID', 'id', { editable: false }),
      f('idTrayectoria', 'ID Trayectoria', 'number', { required: true }),
      f('idPersona', 'ID Persona', 'number', { required: true }),
      f('periodoEgreso', 'Periodo', 'text', { required: true, default: '2026-2' }),
      f('fechaEgreso', 'Fecha egreso', 'date', { required: true }),
      f('creditosTotales', 'Cr. totales', 'number', { required: true, default: 0 }),
      f('promedioFinal', 'Promedio final', 'decimal'),
      f('todosRequisitosCumplidos', 'Req. OK', 'boolean', { required: true, default: false }),
      f('numeroResolucion', 'N° Resolución', 'text'),
      f('observacion', 'Observación', 'textarea', { listed: false })
    ]
  }
];
