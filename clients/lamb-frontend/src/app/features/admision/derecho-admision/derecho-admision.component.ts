import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../../core/api.service';

interface Cargo {
  id: number;
  idPostulante: number;
  periodo: string;
  monto: number;
  saldo: number;
  fechaCreacion: string;
}

@Component({
  selector: 'app-derecho-admision',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="wrap">
      <h2>Derecho de admision</h2>
      <p class="hint">Si el postulante trae voucher de caja, click en "Confirmar pago en caja".
        Los pagos web (Yape / BCP / MercadoPago) se confirman solos.</p>

      @if (mensaje()) { <p class="ok">{{ mensaje() }}</p> }
      @if (error()) { <p class="pend">{{ error() }}</p> }

      <table>
        <thead>
          <tr><th>#</th><th>Postulante</th><th>Periodo</th><th>Monto</th><th>Saldo</th><th>Creado</th><th></th></tr>
        </thead>
        <tbody>
          @for (c of cargos(); track c.id) {
            <tr>
              <td>{{ c.id }}</td>
              <td>{{ c.idPostulante }}</td>
              <td>{{ c.periodo }}</td>
              <td>S/ {{ c.monto }}</td>
              <td>
                @if (c.saldo === 0) { <span class="ok">Pagado</span> }
                @else { <span class="pend">S/ {{ c.saldo }}</span> }
              </td>
              <td>{{ c.fechaCreacion | date:'short' }}</td>
              <td>
                @if (c.saldo > 0) {
                  <button (click)="confirmarCaja(c)">Confirmar pago en caja</button>
                } @else {
                  <small class="ok">Pagado</small>
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
    .ok { color: #0a7a2f; font-weight: 600; }
    .pend { color: #c45500; font-weight: 600; }
    table { width: 100%; border-collapse: collapse; background: #fff; }
    th, td { padding: .6rem .75rem; border-bottom: 1px solid #eee; text-align: left; }
    th { background: #f7f7f7; font-size: .85rem; text-transform: uppercase; color: #555; }
    button { background: #ffb300; border: 0; padding: .4rem .8rem; border-radius: 4px; cursor: pointer; font-weight: 600; }
  `]
})
export class DerechoAdmisionComponent {
  private api = inject(ApiService);
  cargos = signal<Cargo[]>([]);
  mensaje = signal<string | null>(null);
  error = signal<string | null>(null);

  constructor() { this.cargar(); }

  cargar() {
    this.api.list<Cargo>('/api/v1/derecho-admision').subscribe(r => this.cargos.set(r));
  }

  confirmarCaja(c: Cargo) {
    this.error.set(null);
    this.api.action(`/api/v1/derecho-admision/${c.id}/confirmar-caja`).subscribe({
      next: () => {
        this.mensaje.set(`Pago en caja confirmado. Postulante #${c.idPostulante} ahora esta APTO_EXAMEN.`);
        this.cargar();
      },
      error: e => {
        const msg = e?.error?.message || e?.message || 'No se pudo confirmar el pago.';
        this.error.set(`Error: ${msg}`);
      }
    });
  }
}
