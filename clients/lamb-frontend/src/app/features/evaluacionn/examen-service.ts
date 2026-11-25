import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Examen } from './examen.model';

@Injectable({ providedIn: 'root' })
export class ExamenService {
  private readonly http = inject(HttpClient);

  listar(): Observable<Examen[]> {
    return this.http.get<Examen[]>(`${environment.apiUrl}/api/v1/examenes`);
  }

  obtener(id: number): Observable<Examen> {
    return this.http.get<Examen>(`${environment.apiUrl}/api/v1/examenes/${id}`);
  }

  crear(examen: Examen): Observable<Examen> {
    return this.http.post<Examen>(`${environment.apiUrl}/api/v1/examenes`, examen);
  }

  actualizar(id: number, examen: Examen): Observable<Examen> {
    return this.http.put<Examen>(`${environment.apiUrl}/api/v1/examenes/${id}`, examen);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/api/v1/examenes/${id}`);
  }
}
