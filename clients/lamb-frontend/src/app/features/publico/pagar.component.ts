import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';

interface CargoPendiente {
  idCargo: number;
  idPostulante: number;
  monto: number;
  saldo: number;
  periodo: string;
}

@Component({
  selector: 'app-pagar',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="wrap">
      <header class="top">
        <a routerLink="/" class="back">&larr; Inicio</a>
        <h1>Pagar derecho de admision</h1>
      </header>

      <div class="card">
        <label>Ingresa tu DNI</label>
        <div class="row">
          <input type="text" [(ngModel)]="dni" maxlength="8" placeholder="12345678" />
          <button (click)="buscar()" [disabled]="dni().length !== 8">Buscar</button>
        </div>
        @if (error()) { <p class="err">{{ error() }}</p> }
      </div>

      @if (cargos().length > 0) {
        <div class="result">
          <h2>Cargos pendientes</h2>
          @for (c of cargos(); track c.idCargo) {
            <div class="cargo">
              <div>
                <p><b>Derecho de admision {{ c.periodo }}</b></p>
                <p>Monto: <b>S/ {{ c.monto }}</b></p>
              </div>
              <div class="pay-row">
                <button class="yape" (click)="pagar(c, 'YAPE')">Pagar con Yape</button>
                <button class="bcp" (click)="pagar(c, 'BCP')">Pagar con BCP</button>
                <button class="mp" (click)="pagar(c, 'MERCADOPAGO')">Pagar con Mercado Pago</button>
              </div>
            </div>
          }
        </div>
      }

      @if (exito()) {
        <div class="ok-box">
          <h2>Pago confirmado</h2>
          <p>{{ exito() }}</p>
          <p>Ya puedes rendir el examen. Revisa tu correo y tu <a routerLink="/seguimiento">seguimiento</a>.</p>
        </div>
      }
    </div>
  `,
  styles: [`
    .wrap { max-width: 720px; margin: 2rem auto; padding: 1rem; font-family: system-ui; }
    .top { display: flex; align-items: center; gap: 1rem; margin-bottom: 1.5rem; }
    .back { color: #555; text-decoration: none; }
    h1 { margin: 0; color: #003366; }
    .card, .result, .ok-box { background: #fff; padding: 1.5rem; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,.08); margin-bottom: 1rem; }
    .row { display: flex; gap: .5rem; margin-top: .5rem; }
    input { flex: 1; padding: .6rem; border: 1px solid #ccc; border-radius: 4px; font-size: 1rem; }
    button { padding: .6rem 1.2rem; border: 0; border-radius: 4px; cursor: pointer; font-weight: 600; color: #fff; }
    .err { color: #c00; margin-top: .5rem; }
    .cargo { padding: 1rem 0; border-top: 1px solid #eee; }
    .cargo:first-of-type { border-top: 0; }
    .pay-row { display: flex; gap: .5rem; flex-wrap: wrap; margin-top: .5rem; }
    .yape { background: #742782; }
    .bcp { background: #003366; }
    .mp { background: #00b1ea; }
    .ok-box { background: #e8f5e9; }
    .ok-box h2 { color: #0a7a2f; }
  `]
})
export class PagarComponent {
  private api = inject(ApiService);
  dni = signal('');
  cargos = signal<CargoPendiente[]>([]);
  error = signal<string | null>(null);
  exito = signal<string | null>(null);

  buscar() {
    this.error.set(null);
    this.exito.set(null);
    this.api.list<CargoPendiente>(`/api/v1/public/pagar/pendientes/${this.dni()}`).subscribe({
      next: r => {
        this.cargos.set(r);
        if (r.length === 0) this.error.set('No tienes cargos pendientes o tu DNI no esta registrado.');
      },
      error: () => this.error.set('No se pudo consultar. Intenta mas tarde.')
    });
  }

  pagar(c: CargoPendiente, canal: string) {
    this.error.set(null);
    this.api.action<any>(`/api/v1/public/pagar/${c.idCargo}?canal=${canal}`).subscribe({
      next: () => {
        this.exito.set(`Pago de S/ ${c.monto} via ${canal} confirmado.`);
        this.cargos.set([]);
      },
      error: e => {
        const msg = e?.error?.message || e?.message || 'No se pudo registrar el pago.';
        this.error.set(`Error al pagar: ${msg}`);
      }
    });
  }
}
