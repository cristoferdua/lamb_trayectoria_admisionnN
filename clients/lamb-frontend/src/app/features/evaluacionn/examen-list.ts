import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { ExamenService } from './examen-service';
import { Examen } from './examen.model';

@Component({
  selector: 'app-examen-list',
  imports: [RouterLink],
  templateUrl: './examen-list.html',
})
export class ExamenList implements OnInit {
  private readonly examenService = inject(ExamenService);
  protected readonly examenes = signal<Examen[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly errorAccion = signal<string | null>(null);

  ngOnInit(): void {
    this.cargar();
  }

  private cargar(): void {
    this.cargando.set(true);
    this.examenService.listar().subscribe({
      next: (data) => {
        this.examenes.set(data);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar la lista de examenes.');
        this.cargando.set(false);
      },
    });
  }

  eliminar(id: number): void {
    if (!confirm(`¿Seguro que quieres eliminar el examen ${id}?`)) {
      return;
    }
    this.errorAccion.set(null);
    this.examenService.eliminar(id).subscribe({
      next: () => this.cargar(),
      error: (err: HttpErrorResponse) => {
        this.errorAccion.set(
          err.status === 403 ? 'No tienes permiso para esta acción.' : 'No se pudo eliminar el examen.',
        );
      },
    });
  }
}
