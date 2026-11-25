import { Component, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/api.service';
import { Postulacion, PostulacionRequest } from '../postulacion/postulacion.model';

@Component({
  selector: 'app-postulaciones',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  templateUrl: './postulaciones.component.html',
  styleUrl: './postulaciones.component.scss'
})
export class PostulacionesComponent {
  private api = inject(ApiService);
  items = signal<Postulacion[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  form: PostulacionRequest = { idPersona: 1, idPrograma: 1, periodo: '2026-2' };
  showForm = signal(false);

  constructor() { this.load(); }

  load() {
    this.loading.set(true); this.error.set(null);
    this.api.list<Postulacion>('/api/v1/postulaciones').subscribe({
      next: r => { this.items.set(r); this.loading.set(false); },
      error: e => { this.error.set('No se pudo cargar: ' + (e.message ?? e.status)); this.loading.set(false); }
    });
  }

  crear() {
    this.api.post<Postulacion>('/api/v1/postulaciones', this.form).subscribe({
      next: p => { this.items.update(a => [p, ...a]); this.showForm.set(false); },
      error: e => this.error.set('Error al crear: ' + (e.message ?? e.status))
    });
  }

  accion(p: Postulacion, accion: 'admitir' | 'rechazar' | 'evaluar') {
    const body = accion === 'evaluar' ? { puntajeExamen: 15 } : {};
    this.api.action<Postulacion>(`/api/v1/postulaciones/${p.id}/${accion}`, body).subscribe({
      next: updated => this.items.update(a => a.map(x => x.id === updated.id ? updated : x)),
      error: e => this.error.set('Error: ' + (e.message ?? e.status))
    });
  }

  estadoClass(e: string) {
    return {
      REGISTRADA: 'badge badge--yellow',
      EVALUADA: 'badge badge--blue',
      ADMITIDA: 'badge badge--green',
      RECHAZADA: 'badge badge--red'
    }[e] ?? 'badge';
  }
}
