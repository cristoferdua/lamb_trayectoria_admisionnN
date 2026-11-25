import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { KeycloakService } from '../../core/keycloak.service';

interface Tile {
  label: string;
  icon: string;
  link: string;
}

@Component({
  selector: 'app-admin-home',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="landing">
      <header class="top">
        <div class="brand">
          <div class="chip">A</div>
          <span class="brand-text">A | UPeU</span>
        </div>
        <div class="user">
          <span class="material-symbols-outlined cal">calendar_month</span>
          <div class="user-meta">
            <div class="name">{{ kc.profile()?.name || 'NELSON ANTONY LARICO QUISPE' }}</div>
            <div class="filial">{{ kc.profile()?.filial || 'FILIAL JULIACA' }}</div>
          </div>
          <div class="avatar">{{ kc.profile()?.initials || 'NL' }}</div>
          <button class="logout" (click)="kc.logout()" title="Cerrar sesión">
            <span class="material-symbols-outlined">logout</span>
          </button>
        </div>
      </header>

      <main class="hero">
        <div class="tiles">
          @for (t of tiles; track t.link) {
            <a [routerLink]="t.link" class="tile">
              <span class="material-symbols-outlined tile__icon">{{ t.icon }}</span>
              <span class="tile__label">{{ t.label }}</span>
            </a>
          }
        </div>
      </main>

      <footer class="foot">
        <span>versión 2.0</span>
        <span class="sep">·</span>
        <span class="online"><span class="dot"></span> En línea</span>
      </footer>
    </div>
  `,
  styles: [`
    :host { display: block; }
    .landing {
      min-height: 100vh;
      background:
        linear-gradient(180deg, rgba(15,30,63,.55), rgba(15,30,63,.78)),
        url('/graduados.jpg') center/cover no-repeat;
      display: flex; flex-direction: column; color: #fff;
    }
    .top {
      display: flex; justify-content: space-between; align-items: center;
      padding: .9rem 1.5rem; background: rgba(15,30,63,.6); backdrop-filter: blur(6px);
    }
    .brand { display: flex; align-items: center; gap: .65rem; }
    .chip {
      width: 40px; height: 40px; border-radius: 10px; background: #F0A500;
      color: #0F1E3F; font-weight: 800; font-size: 1.1rem;
      display: grid; place-items: center;
    }
    .brand-text { font-weight: 700; letter-spacing: .5px; font-size: 1rem; }
    .brand-text::first-letter { color: #F0A500; }
    .user { display: flex; align-items: center; gap: .9rem; }
    .cal {
      background: rgba(255,255,255,.14); padding: .5rem; border-radius: 10px; font-size: 1.2rem;
    }
    .user-meta { text-align: right; line-height: 1.1; }
    .name { font-weight: 800; font-size: .88rem; letter-spacing: .3px; }
    .filial { font-size: .72rem; opacity: .85; letter-spacing: .5px; margin-top: 2px; }
    .avatar {
      width: 42px; height: 42px; border-radius: 50%; background: #F0A500; color: #0F1E3F;
      display: grid; place-items: center; font-weight: 800; font-size: .85rem;
    }
    .logout {
      background: transparent; border: 0; color: #fff; cursor: pointer; opacity: .85;
      display: grid; place-items: center; padding: .25rem;
    }
    .logout:hover { opacity: 1; }

    .hero { flex: 1; display: grid; place-items: center; padding: 2rem 1.5rem; }
    .tiles {
      display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 170px));
      gap: 1.25rem; justify-content: center; max-width: 1200px; width: 100%;
    }
    .tile {
      background: rgba(255,255,255,.12); backdrop-filter: blur(14px);
      border: 1px solid rgba(255,255,255,.2); border-radius: 18px;
      padding: 1.6rem 1rem;
      display: flex; flex-direction: column; align-items: center; gap: .9rem;
      text-decoration: none; color: #fff;
      transition: transform .18s ease, background .18s ease, border-color .18s ease;
      min-height: 150px; justify-content: center; text-align: center;
    }
    .tile:hover {
      transform: translateY(-4px); background: rgba(255,255,255,.22);
      border-color: rgba(255,255,255,.35);
    }
    .tile__icon {
      font-size: 2rem; opacity: .95;
      background: rgba(255,255,255,.12); padding: .55rem; border-radius: 12px;
    }
    .tile__label {
      font-weight: 700; letter-spacing: .6px; font-size: .82rem;
      text-transform: uppercase; line-height: 1.25;
    }

    .foot {
      text-align: center; padding: 1rem; font-size: .8rem; opacity: .85;
      display: flex; justify-content: center; gap: .5rem; align-items: center;
    }
    .sep { opacity: .55; }
    .online { display: inline-flex; align-items: center; gap: .35rem; }
    .dot { width: 8px; height: 8px; border-radius: 50%; background: #3ddc84; }
  `]
})
export class AdminHomeComponent {
  kc = inject(KeycloakService);
  tiles: Tile[] = [
    { label: 'Admisión',   icon: 'how_to_reg', link: '/admin/admision' },
    { label: 'Trayectoria', icon: 'route',      link: '/admin/trayectoria' }
  ];
}
