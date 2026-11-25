import { Component, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ApiService } from '../../../core/api.service';

interface Alerta {
  id: number;
  idEstudiante: number;
  descripcion: string;
  nivelRiesgo: string;
  fechaDeteccion: string;
}

@Component({
  selector: 'app-alertas',
  standalone: true,
  imports: [CommonModule, DatePipe],
  template: `
    <div class="upeu-card">
      <h2 style="color:var(--upeu-navy);margin:0 0 12px">Alertas de riesgo</h2>
      @if (loading()) { <p>Cargando...</p> }
      @else if (error(); as e) { <p class="err">{{ e }}</p> }
      @else if (items().length === 0) { <p class="muted">Sin alertas.</p> }
      @else {
        <table style="width:100%;border-collapse:collapse">
          <thead>
            <tr style="background:#F9FAFB">
              <th style="text-align:left;padding:10px">ID</th>
              <th style="text-align:left;padding:10px">Estudiante</th>
              <th style="text-align:left;padding:10px">Descripción</th>
              <th style="text-align:left;padding:10px">Nivel</th>
              <th style="text-align:left;padding:10px">Fecha</th>
            </tr>
          </thead>
          <tbody>
            @for (a of items(); track a.id) {
              <tr style="border-top:1px solid #F3F4F6">
                <td style="padding:10px">#{{ a.id }}</td>
                <td style="padding:10px">{{ a.idEstudiante }}</td>
                <td style="padding:10px">{{ a.descripcion }}</td>
                <td style="padding:10px">{{ a.nivelRiesgo }}</td>
                <td style="padding:10px">{{ a.fechaDeteccion | date:'short' }}</td>
              </tr>
            }
          </tbody>
        </table>
      }
    </div>
  `,
  styles: [`.err{color:var(--upeu-bad)}.muted{color:var(--upeu-muted)}`]
})
export class AlertasComponent {
  private api = inject(ApiService);
  items = signal<Alerta[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  constructor() {
    this.api.list<Alerta>('/api/v1/alertas-riesgo').subscribe({
      next: r => { this.items.set(r); this.loading.set(false); },
      error: e => { this.error.set('Error: ' + (e.message ?? e.status)); this.loading.set(false); }
    });
  }
}
