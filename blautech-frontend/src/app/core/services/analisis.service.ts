import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AnalisisResponse } from '../models/postulacion.model';

// Coincide 1:1 con AnalisisController.java real.
@Injectable({ providedIn: 'root' })
export class AnalisisService {
  private base = `${environment.apiUrl}/analisis`;

  constructor(private http: HttpClient) {}

  generar(idPostulacion: number) {
    return this.http.post<AnalisisResponse>(`${this.base}/generar/${idPostulacion}`, {});
  }

  obtener(idPostulacion: number) {
    return this.http.get<AnalisisResponse>(`${this.base}/postulacion/${idPostulacion}`);
  }
}
