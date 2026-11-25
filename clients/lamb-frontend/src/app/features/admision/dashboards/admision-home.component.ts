import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

interface Tarjeta {
  label: string;
  desc: string;
  icon: string;
  link: string;
}

@Component({
  selector: 'app-admision-home',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="wrap">
      <header class="head">
        <h2>Módulo de Admisión</h2>
        <p>Gestión completa del proceso de admisión del periodo vigente.</p>
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
      border-top: 3px solid #f0a500;
    }
    .card:hover { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(15,30,63,.08); border-color: #c9ced8; }
    .card .ico { font-size: 2rem; color: #f0a500; }
    .card h3 { margin: .5rem 0 .3rem; font-size: 1.05rem; }
    .card p { margin: 0; font-size: .85rem; color: #666; line-height: 1.35; }
  `]
})
export class AdmisionHomeComponent {
  tarjetas: Tarjeta[] = [
    { label: 'Bandeja de prospectos', desc: 'Postulantes del wizard público', icon: 'inbox', link: '/admin/prospectos' },
    { label: 'Pago de derecho de admisión', desc: 'Caja, Yape, BCP, Plin, web', icon: 'payments', link: '/admin/derecho-admision' },
    { label: 'Convocatorias', desc: 'Periodos, vacantes y cronograma', icon: 'campaign', link: '/admin/crud/convocatorias' },
    { label: 'Postulantes', desc: 'Datos y seguimiento del postulante', icon: 'person', link: '/admin/crud/postulantes' },
    { label: 'Exámenes de admisión', desc: 'Programación y aulas', icon: 'quiz', link: '/admin/crud/examenes-admision' },
    { label: 'Calificaciones', desc: 'Puntajes y orden de mérito', icon: 'grade', link: '/admin/crud/calificaciones' },
    { label: 'Adjudicación de vacantes', desc: 'Lista de ingresantes', icon: 'verified', link: '/admin/crud/adjudicaciones' },
    { label: 'Seguimiento', desc: 'Contactos y medios de captación', icon: 'timeline', link: '/admin/crud/seguimientos' },
    { label: 'Casos especiales', desc: 'Ficha socioeconómica, prórrogas', icon: 'warning', link: '/admin/crud/casos-especiales' },
    { label: 'Consulta RENIEC', desc: 'Buscador de DNI', icon: 'badge', link: '/admin/reniec' }
  ];
}
