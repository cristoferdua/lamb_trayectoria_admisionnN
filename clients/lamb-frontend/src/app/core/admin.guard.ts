import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { KeycloakService } from './keycloak.service';

/**
 * Guarda rutas /admin/**: exige sesion Keycloak. Si no hay, dispara login.
 * Rol: por ahora basta con estar autenticado (unico rol = ADMIN).
 */
export const adminGuard: CanActivateFn = () => {
  const kc = inject(KeycloakService);
  const router = inject(Router);
  if (kc.authenticated()) return true;
  kc.login();
  router.navigateByUrl('/');
  return false;
};
