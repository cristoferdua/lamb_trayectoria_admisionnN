import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ExamenService } from './examen-service';

@Component({
  selector: 'app-examen-form',
  imports: [ReactiveFormsModule],
  templateUrl: './examen-form.html',
})
export class ExamenForm {
  private readonly fb = inject(FormBuilder);
  private readonly examenService = inject(ExamenService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly id = signal<number | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    titulo: ['', [Validators.required, Validators.maxLength(150)]],
    curso: ['', [Validators.maxLength(100)]],
  });

  constructor() {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = Number(idParam);
      this.id.set(id);
      this.cargando.set(true);
      this.examenService.obtener(id).subscribe({
        next: (examen) => {
          this.form.patchValue(examen);
          this.cargando.set(false);
        },
        error: () => {
          this.error.set('No se pudo cargar el examen.');
          this.cargando.set(false);
        },
      });
    }
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const valor = this.form.getRawValue();
    const id = this.id();
    const peticion = id ? this.examenService.actualizar(id, valor) : this.examenService.crear(valor);

    peticion.subscribe({
      next: () => this.router.navigate(['/evaluacionn/examenes']),
      error: (err: HttpErrorResponse) => {
        this.error.set(
          err.status === 403 ? 'No tienes permiso para esta acción.' : 'No se pudo guardar el examen.',
        );
      },
    });
  }

  cancelar(): void {
    this.router.navigate(['/evaluacionn/examenes']);
  }

  protected mensajeValidacion(campo: 'titulo' | 'curso'): string {
    const control = this.form.controls[campo];

    if (!control.touched) return '';
    if (control.hasError('required')) return 'Este campo es obligatorio.';
    if (control.hasError('maxlength')) {
      return `Máximo ${control.getError('maxlength').requiredLength} caracteres.`;
    }
    return '';
  }
}
