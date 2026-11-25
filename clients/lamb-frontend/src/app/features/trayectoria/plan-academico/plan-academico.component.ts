import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

interface Curso { nombre: string; cr: number; ht: number; hp: number; th: number; nota: number | null; }
interface Ciclo { num: number; cursos: Curso[]; }

@Component({
  selector: 'app-plan-academico',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './plan-academico.component.html',
  styleUrl: './plan-academico.component.scss'
})
export class PlanAcademicoComponent {
  carrera = signal('2024-1 Ingeniería de Sistemas, Presencial');
  ciclos = signal<Ciclo[]>([
    { num: 1, cursos: [
      { nombre: 'Comunicación Oral y Escrita', cr: 4, ht: 3, hp: 2, th: 5, nota: 18 },
      { nombre: 'Desarrollo Personal I', cr: 3, ht: 2, hp: 1, th: 3, nota: 17 },
      { nombre: 'Formación Cristiana I', cr: 2, ht: 2, hp: 0, th: 2, nota: 17 },
      { nombre: 'Fundamentos de Matemática', cr: 3, ht: 2, hp: 2, th: 4, nota: 14 },
      { nombre: 'Fundamentos de Programación', cr: 5, ht: 3, hp: 4, th: 7, nota: 17 },
      { nombre: 'Introducción a la Ingeniería de Sistemas', cr: 3, ht: 2, hp: 2, th: 4, nota: 14 }
    ]},
    { num: 2, cursos: [
      { nombre: 'Álgebra Lineal', cr: 4, ht: 3, hp: 2, th: 5, nota: 14 },
      { nombre: 'Cálculo I', cr: 3, ht: 2, hp: 1, th: 3, nota: 14 },
      { nombre: 'Desarrollo Personal II', cr: 2, ht: 0, hp: 4, th: 4, nota: 15 },
      { nombre: 'Formación Cristiana II', cr: 2, ht: 2, hp: 0, th: 2, nota: 17 },
      { nombre: 'Gestión para el Aprendizaje y la Investigación', cr: 2, ht: 1, hp: 2, th: 3, nota: 16 },
      { nombre: 'Programación Orientada a Objetos', cr: 5, ht: 3, hp: 4, th: 7, nota: 15 }
    ]},
    { num: 3, cursos: [
      { nombre: 'Administración de Base de Datos', cr: 3, ht: 2, hp: 2, th: 4, nota: 15 },
      { nombre: 'Cálculo II', cr: 3, ht: 2, hp: 2, th: 4, nota: 14 },
      { nombre: 'Educación Ambiental y Desarrollo Sostenible', cr: 1, ht: 0, hp: 2, th: 2, nota: 16 },
      { nombre: 'Física I', cr: 4, ht: 3, hp: 2, th: 5, nota: 15 },
      { nombre: 'Formación Cristiana III', cr: 2, ht: 2, hp: 0, th: 2, nota: 16 },
      { nombre: 'Ingeniería de Requerimientos', cr: 3, ht: 2, hp: 2, th: 4, nota: 15 }
    ]},
    { num: 4, cursos: [
      { nombre: 'Administración de Base de Datos II', cr: 3, ht: 2, hp: 2, th: 4, nota: 14 },
      { nombre: 'Análisis y Diseño de Sistemas', cr: 3, ht: 2, hp: 2, th: 4, nota: null },
      { nombre: 'Cálculo III', cr: 4, ht: 3, hp: 2, th: 5, nota: 15 },
      { nombre: 'Física II', cr: 4, ht: 3, hp: 2, th: 5, nota: 13 },
      { nombre: 'Formación Cristiana IV', cr: 2, ht: 2, hp: 0, th: 2, nota: 18 },
      { nombre: 'Lenguaje de Programación II', cr: 3, ht: 2, hp: 2, th: 4, nota: 13 }
    ]}
  ]);

  notaClass(n: number | null): string {
    if (n === null) return 'nota nota--empty';
    if (n < 14) return 'nota nota--bad';
    if (n >= 17) return 'nota nota--top';
    return 'nota nota--ok';
  }
}
