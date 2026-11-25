import { Routes } from '@angular/router';
import { adminGuard } from './core/admin.guard';

export const routes: Routes = [
  // ====== PUBLICO ======
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./features/inicio/home.component').then(m => m.HomeComponent)
  },
  {
    path: 'postular',
    loadComponent: () => import('./features/admision/postulacion/wizard-postulacion.component').then(m => m.WizardPostulacionComponent)
  },
  {
    path: 'seguimiento',
    loadComponent: () => import('./features/publico/seguimiento.component').then(m => m.SeguimientoComponent)
  },
  {
    path: 'pagar',
    loadComponent: () => import('./features/publico/pagar.component').then(m => m.PagarComponent)
  },

  // ====== ADMIN HOME (fullscreen, sin sidebar) ======
  {
    path: 'admin',
    pathMatch: 'full',
    canActivate: [adminGuard],
    loadComponent: () => import('./features/inicio/admin-home.component').then(m => m.AdminHomeComponent)
  },

  // ====== ADMIN (con shell + sidebar) ======
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadComponent: () => import('./layout/shell.component').then(m => m.ShellComponent),
    children: [
      {
        path: 'admision',
        loadComponent: () => import('./features/admision/dashboards/admision-home.component').then(m => m.AdmisionHomeComponent)
      },
      {
        path: 'trayectoria',
        pathMatch: 'full',
        loadComponent: () => import('./features/trayectoria/dashboards/trayectoria-home.component').then(m => m.TrayectoriaHomeComponent)
      },
      {
        path: 'prospectos',
        loadComponent: () => import('./features/admision/prospectos/prospectos.component').then(m => m.ProspectosComponent)
      },
      {
        path: 'derecho-admision',
        loadComponent: () => import('./features/admision/derecho-admision/derecho-admision.component').then(m => m.DerechoAdmisionComponent)
      },
      {
        path: 'crud/:slug',
        loadComponent: () => import('./features/crud-generic/crud-generic.component').then(m => m.CrudGenericComponent)
      },
      {
        path: 'reniec',
        loadComponent: () => import('./features/admision/reniec/reniec-buscador.component').then(m => m.ReniecBuscadorComponent)
      },
      {
        path: 'trayectoria/plan',
        loadComponent: () => import('./features/trayectoria/plan-academico/plan-academico.component').then(m => m.PlanAcademicoComponent)
      }
    ]
  },

  // ====== EVALUACIONN: CRUD de examenes (paginas simples, sin diseño; piden login de Keycloak) ======
  { path: 'evaluacionn', redirectTo: 'evaluacionn/examenes', pathMatch: 'full' },
  {
    path: 'evaluacionn/examenes',
    canActivate: [adminGuard],
    loadComponent: () => import('./features/evaluacionn/examen-list').then((m) => m.ExamenList)
  },
  {
    path: 'evaluacionn/examenes/nuevo',
    canActivate: [adminGuard],
    loadComponent: () => import('./features/evaluacionn/examen-form').then((m) => m.ExamenForm)
  },
  {
    path: 'evaluacionn/examenes/:id/editar',
    canActivate: [adminGuard],
    loadComponent: () => import('./features/evaluacionn/examen-form').then((m) => m.ExamenForm)
  },

  { path: '**', redirectTo: '' }
];
