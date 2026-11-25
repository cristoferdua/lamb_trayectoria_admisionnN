import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../../core/api.service';

interface Postulante {
  id: number;
  codigoPostulante: string;
  idPersona: number;
  estado: string;
  correoContacto: string;
  telefonoContacto: string;
  fechaRegistro: string;
}

@Component({
  selector: 'app-prospectos',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="wrap">
      <h2>Bandeja de admisión</h2>
      <p class="hint">Postulantes que llegaron por el wizard publico y aun no formalizan en oficina.</p>

      @if (loading()) { <p>Cargando...</p> }
      @if (mensaje()) { <p class="ok">{{ mensaje() }}</p> }
      @if (error()) { <p class="err">{{ error() }}</p> }

      <table>
        <thead>
          <tr><th>Codigo</th><th>Estado</th><th>Correo</th><th>Telefono</th><th>Registro</th><th></th></tr>
        </thead>
        <tbody>
          @for (p of filtrados(); track p.id) {
            <tr>
              <td>{{ p.codigoPostulante }}</td>
              <td><span class="badge">{{ p.estado }}</span></td>
              <td>{{ p.correoContacto }}</td>
              <td>{{ p.telefonoContacto }}</td>
              <td>{{ p.fechaRegistro | date:'short' }}</td>
              <td>
                @if (p.estado === 'PRE_INSCRITO') {
                  <button (click)="formalizar(p)">Formalizar</button>
                } @else {
                  <small>&mdash;</small>
                }
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
  styles: [`
    .wrap { padding: 1rem; font-family: system-ui; }
    h2 { color: #003366; margin: 0 0 .25rem; }
    .hint { color: #666; margin-bottom: 1rem; }
    .ok { color: #0a7a2f; }
    .err { color: #c00; }
    table { width: 100%; border-collapse: collapse; background: #fff; }
    th, td { padding: .6rem .75rem; border-bottom: 1px solid #eee; text-align: left; }
    th { background: #f7f7f7; font-size: .85rem; text-transform: uppercase; color: #555; }
    .badge { background: #fff4d6; color: #a65f00; padding: .15rem .5rem; border-radius: 4px; font-size: .8rem; }
    button { background: #003366; color: #fff; border: 0; padding: .4rem .8rem; border-radius: 4px; cursor: pointer; }
  `]
})
export class ProspectosComponent {
  private api = inject(ApiService);

  postulantes = signal<Postulante[]>([]);
  loading = signal(false);
  mensaje = signal<string | null>(null);
  error = signal<string | null>(null);

  filtrados = computed(() => this.postulantes().filter(p =>
    ['PRE_INSCRITO', 'FORMALIZADO', 'PAGADO', 'APTO_EXAMEN'].includes(p.estado)));

  constructor() { this.cargar(); }

  cargar() {
    this.loading.set(true);
    this.api.list<Postulante>('/api/v1/postulantes').subscribe({
      next: r => { this.postulantes.set(r); this.loading.set(false); },
      error: () => { this.loading.set(false); this.error.set('No se pudo cargar'); }
    });
  }

  formalizar(p: Postulante) {
    this.mensaje.set(null);
    this.error.set(null);
    this.api.action<{ idCargo: number }>(`/api/v1/postulantes/${p.id}/formalizar`).subscribe({
      next: r => { this.mensaje.set(`Formalizado. Cargo generado #${r.idCargo}`); this.cargar(); },
      error: e => this.error.set(e?.error?.message || 'Error al formalizar')
    });
  }
}
