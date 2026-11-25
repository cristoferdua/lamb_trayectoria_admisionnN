import { CrudDef } from './crud.types';
import { ADMISION_CRUDS } from '../admision/admision.cruds';
import { TRAYECTORIA_CRUDS } from '../trayectoria/trayectoria.cruds';

export const CRUDS: CrudDef[] = [...ADMISION_CRUDS, ...TRAYECTORIA_CRUDS];

export const CRUD_BY_SLUG = new Map(CRUDS.map(c => [c.slug, c]));
