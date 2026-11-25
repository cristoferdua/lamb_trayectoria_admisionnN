import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

interface Tarjeta {
  label: string;
  desc: string;
  icon: string;
  link: string;
}

@Component({
  selector: 'app-trayectoria-home',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="wrap">
      <header class="head">
        <h2>Módulo de Trayectoria</h2>
        <p>Seguimiento académico del estudiante desde el ingreso hasta el egreso.</p>
      </header>

      <div class="grid">
        @for (t of tarjetas; track t.link) {
          <a class="card" [routerLink]="t.link">
            <span class="material-symbols-outlined ico">{{ t.icon }}</span>
            <h3>{{ t.label }}</h3>
            <p>{{ t.desc }}</p>
          </a>
        }
      </div>
    </div>
  `,
  styles: [`
    .wrap { padding: 2rem 1.5rem; max-width: 1200px; margin: 0 auto; }
    .head { margin-bottom: 1.75rem; }
    .head h2 { margin: 0 0 .25rem; color: #0f1e3f; font-size: 1.5rem; }
    .head p { margin: 0; color: #666; }
    .grid {
      display: grid; grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
      gap: 1rem;
    }
    .card {
      background: #fff; border: 1px solid #e6e8ef; border-radius: 12px;
      padding: 1.25rem; text-decoration: none; color: #0f1e3f; display: block;
      transition: transform .15s ease, box-shadow .15s ease, border-color .15s ease;
      border-top: 3px solid #0f1e3f;
    }
    .card:hover { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(15,30,63,.08); border-color: #c9ced8; }
    .card .ico { font-size: 2rem; color: #0f1e3f; }
    .card h3 { margin: .5rem 0 .3rem; font-size: 1.05rem; }
    .card p { margin: 0; font-size: .85rem; color: #666; line-height: 1.35; }
  `]
})
export class TrayectoriaHomeComponent {
  tarjetas: Tarjeta[] = [
    { label: 'Plan académico', desc: 'Catálogo de planes y cursos', icon: 'menu_book', link: '/admin/trayectoria/plan' },
    { label: 'Trayectorias', desc: 'Estado académico del estudiante', icon: 'route', link: '/admin/crud/trayectorias' },
    { label: 'Avance y requisitos', desc: 'Cumplimientos de egreso', icon: 'checklist', link: '/admin/crud/cumplimientos-requisito' },
    { label: 'Riesgo académico', desc: 'Alertas y planes de refuerzo', icon: 'warning', link: '/admin/crud/alertas-riesgo' },
    { label: 'Tutoría y bienestar', desc: 'Programa y sesiones de tutoría', icon: 'school', link: '/admin/crud/tutorias' },
    { label: 'Reconocimientos', desc: 'Logros y portafolio', icon: 'military_tech', link: '/admin/crud/reconocimientos' },
    { label: 'Cierre y egreso', desc: 'Verificación de requisitos', icon: 'workspace_premium', link: '/admin/crud/egresos' }
  ];
}
