import { ApplicationConfig, provideAppInitializer, inject, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

import { routes } from './app.routes';
import { KeycloakService } from './core/keycloak.service';
import { authInterceptor } from './core/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAnimationsAsync(),
    provideAppInitializer(async () => {
      const kc = inject(KeycloakService);
      try { await kc.init(); } catch (e) { console.warn('Keycloak init fallo:', e); }
    })
  ]
};
