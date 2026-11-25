import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/api.service';

interface ReniecResponse {
  dni: string;
  nombres: string | null;
  apellidoPaterno: string | null;
  apellidoMaterno: string | null;
  nombreCompleto: string | null;
  encontrado: boolean;
  fuente: string;
}

@Component({
  selector: 'app-reniec-buscador',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reniec-buscador.component.html',
  styleUrl: './reniec-buscador.component.scss'
})
export class ReniecBuscadorComponent {
  private api = inject(ApiService);

  dni = signal('');
  loading = signal(false);
  result = signal<ReniecResponse | null>(null);
  error = signal<string | null>(null);

  buscar() {
    const dni = this.dni().trim();
    if (!/^\d{8}$/.test(dni)) {
      this.error.set('El DNI debe tener exactamente 8 dígitos.');
      this.result.set(null);
      return;
    }
    this.error.set(null);
    this.loading.set(true);
    this.api.get<ReniecResponse>(`/api/v1/admision/reniec/${dni}`).subscribe({
      next: r => {
        this.result.set(r);
        this.loading.set(false);
        if (!r.encontrado) {
          this.error.set(`No se encontraron datos (${r.fuente}).`);
        }
      },
      error: e => {
        this.loading.set(false);
        this.error.set(`Error consultando RENIEC: ${e.status ?? ''} ${e.message ?? ''}`);
      }
    });
  }

  limpiar() {
    this.dni.set('');
    this.result.set(null);
    this.error.set(null);
  }
}
