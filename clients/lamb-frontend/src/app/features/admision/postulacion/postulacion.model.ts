export type EstadoPostulacion = 'REGISTRADA' | 'EVALUADA' | 'ADMITIDA' | 'RECHAZADA';

export interface Postulacion {
  id: number;
  idPersona: number;
  idPrograma: number;
  periodo: string;
  puntajeExamen: number | null;
  estado: EstadoPostulacion;
  fechaRegistro: string;
}

export interface PostulacionRequest {
  idPersona: number;
  idPrograma: number;
  periodo: string;
}
