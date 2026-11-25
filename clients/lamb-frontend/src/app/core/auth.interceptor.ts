import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { KeycloakService } from './keycloak.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const kc = inject(KeycloakService);
  const token = kc.token;
  if (token && req.url.startsWith('http://localhost:18080')) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }
  return next(req);
};
