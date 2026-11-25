export type FieldType = 'text' | 'number' | 'decimal' | 'date' | 'datetime' | 'enum' | 'boolean' | 'textarea' | 'id' | 'reniec';

export interface FieldDef {
  key: string;
  label: string;
  type: FieldType;
  required?: boolean;
  options?: string[];     // para enum
  listed?: boolean;       // se muestra en la tabla
  editable?: boolean;     // se puede editar en el formulario (false para campos calculados)
  default?: any;
}

export interface RowDownload {
  label: string;
  icon: string;
  // Path relativo al backend que devuelve el archivo. Soporta {id} como placeholder.
  path: string;
  // Sufijo del filename. Soporta {id} y {codigo}.
  filename: string;
  // Campo del row usado como "codigo" en filename (ej. codigoPostulante).
  codeField?: string;
}

export interface CrudDef {
  slug: string;
  label: string;
  icon: string;
  path: string;
  group: 'Admisión' | 'Trayectoria';
  description: string;
  fields: FieldDef[];
  // Si está definido, el botón "Nuevo" redirige a esta ruta (wizard) en vez de abrir el form genérico.
  createVia?: string;
  // Descarga adicional disponible por fila (ej. carnet PDF del postulante).
  rowDownload?: RowDownload;
}

// Helper
export const f = (key: string, label: string, type: FieldType, opts: Partial<FieldDef> = {}): FieldDef =>
  ({ key, label, type, listed: true, editable: true, ...opts });
