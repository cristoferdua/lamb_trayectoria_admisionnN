import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';

interface EventoTimeline {
  etiqueta: string;
  detalle: string;
  fecha: string | null;
  cumplido: boolean;
}

interface TimelineResponse {
  dni: string;
  nombreCompleto: string;
  idPostulante: number;
  codigoPostulante: string;
  estado: string;
  periodo: string;
  puntajeExamen: number | null;
  timeline: EventoTimeline[];
}

@Component({
  selector: 'app-seguimiento',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="wrap">
      <header class="top">
        <a routerLink="/" class="back">&larr; Inicio</a>
        <h1>Consulta tu postulacion</h1>
      </header>

      <div class="card">
        <label>DNI</label>
        <div class="row">
          <input type="text" [(ngModel)]="dni" placeholder="12345678" maxlength="8" />
          <button (click)="buscar()" [disabled]="dni().length !== 8 || loading()">
            {{ loading() ? 'Buscando...' : 'Consultar' }}
          </button>
        </div>
        @if (error()) {
          <p class="error">{{ error() }}</p>
        }
      </div>

      @if (data(); as d) {
        <div class="result">
          <h2>{{ d.nombreCompleto }}</h2>
          <p class="codigo">Codigo: <b>{{ d.codigoPostulante }}</b> &middot; Periodo: {{ d.periodo }}</p>
          <p class="estado" [class.ok]="d.estado === 'ADMITIDO'" [class.no]="d.estado === 'NO_ADMITIDO'">
            Estado actual: <b>{{ d.estado }}</b>
          </p>
          @if (d.puntajeExamen != null) {
            <p>Puntaje del examen: <b>{{ d.puntajeExamen }}</b></p>
          }

          <ol class="timeline">
            @for (e of d.timeline; track e.etiqueta) {
              <li [class.done]="e.cumplido">
                <span class="dot">{{ e.cumplido ? '✓' : '·' }}</span>
                <div>
                  <b>{{ e.etiqueta }}</b>
                  <small>{{ e.detalle }}</small>
                </div>
              </li>
            }
          </ol>
        </div>
      }
    </div>
  `,
  styles: [`
    .wrap { max-width: 720px; margin: 2rem auto; padding: 1rem; font-family: system-ui; }
    .top { display: flex; align-items: center; gap: 1rem; margin-bottom: 1.5rem; }
    .back { color: #555; text-decoration: none; }
    h1 { margin: 0; color: #003366; }
    .card { background: #fff; padding: 1.5rem; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
    .row { display: flex; gap: .5rem; margin-top: .5rem; }
    input { flex: 1; padding: .6rem; border: 1px solid #ccc; border-radius: 4px; font-size: 1rem; }
    button { padding: .6rem 1.2rem; background: #ffb300; border: 0; border-radius: 4px; font-weight: 600; cursor: pointer; }
    button:disabled { opacity: .5; cursor: not-allowed; }
    .error { color: #c00; margin-top: .5rem; }
    .result { margin-top: 1.5rem; background: #fff; padding: 1.5rem; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
    .codigo { color: #666; }
    .estado.ok { color: #0a7a2f; }
    .estado.no { color: #c00; }
    .timeline { list-style: none; padding: 0; margin: 1.5rem 0 0; }
    .timeline li { display: flex; gap: .75rem; padding: .5rem 0; color: #999; }
    .timeline li.done { color: #222; }
    .dot { width: 24px; height: 24px; border-radius: 50%; background: #ddd; color: #fff;
           display: grid; place-items: center; font-weight: 700; flex: 0 0 24px; }
    .timeline li.done .dot { background: #0a7a2f; }
    small { display: block; color: #777; }
  `]
})
export class SeguimientoComponent {
  private api = inject(ApiService);
  dni = signal('');
  loading = signal(false);
  error = signal<string | null>(null);
  data = signal<TimelineResponse | null>(null);

  buscar() {
    this.loading.set(true);
    this.error.set(null);
    this.data.set(null);
    this.api.get<TimelineResponse>(`/api/v1/public/seguimiento/${this.dni()}`).subscribe({
      next: r => { this.data.set(r); this.loading.set(false); },
      error: e => {
        this.error.set(e?.error?.message || 'No encontramos postulacion para ese DNI');
        this.loading.set(false);
      }
    });
  }
}
