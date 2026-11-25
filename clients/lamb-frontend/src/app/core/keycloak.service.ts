import { Injectable, signal } from '@angular/core';
import Keycloak from 'keycloak-js';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class KeycloakService {
  private kc: Keycloak | null = null;
  readonly authenticated = signal(false);
  readonly profile = signal<{ name: string; initials: string; filial: string } | null>(null);

  async init(): Promise<boolean> {
    this.kc = new Keycloak({
      url: environment.keycloak.url,
      realm: environment.keycloak.realm,
      clientId: environment.keycloak.clientId
    });
    const ok = await this.kc.init({
      onLoad: 'check-sso',
      pkceMethod: 'S256',
      silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html'
    });
    this.authenticated.set(ok);
    if (ok) this.loadProfile();
    return ok;
  }

  login() { this.kc?.login({ redirectUri: window.location.origin }); }
  logout() { this.kc?.logout({ redirectUri: window.location.origin }); }
  get token(): string | undefined { return this.kc?.token; }

  private async loadProfile() {
    const p = await this.kc?.loadUserProfile();
    const name = `${p?.firstName ?? ''} ${p?.lastName ?? ''}`.trim() || p?.username || 'Usuario';
    const initials = name.split(' ').filter(Boolean).slice(0, 2).map(s => s[0]!.toUpperCase()).join('');
    this.profile.set({ name: name.toUpperCase(), initials, filial: 'FILIAL JULIACA' });
  }
}
