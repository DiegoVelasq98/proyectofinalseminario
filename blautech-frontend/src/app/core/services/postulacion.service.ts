import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import {
  PostulacionRequest,
  PostulacionResponse,
  CambiarEstadoRequest,
  HistorialPostulacion,
  PostulacionEstadisticasResponse,
} from '../models/postulacion.model';

// Coincide 1:1 con PostulacionController.java real.
@Injectable({ providedIn: 'root' })
export class PostulacionService {
  private base = `${environment.apiUrl}/postulaciones`;

  constructor(private http: HttpClient) {}

  postularse(req: PostulacionRequest) {
    return this.http.post<PostulacionResponse>(this.base, req);
  }

  misPostulaciones() {
    return this.http.get<PostulacionResponse[]>(`${this.base}/mias`);
  }

  listarPorVacante(idVacante: number) {
    return this.http.get<PostulacionResponse[]>(`${this.base}/vacante/${idVacante}`);
  }

  listarPorCandidato(idCandidato: number) {
    return this.http.get<PostulacionResponse[]>(`${this.base}/candidato/${idCandidato}`);
  }

  cambiarEstado(id: number, req: CambiarEstadoRequest) {
    return this.http.put<PostulacionResponse>(`${this.base}/${id}/estado`, req);
  }

  obtenerHistorial(id: number) {
    return this.http.get<HistorialPostulacion[]>(`${this.base}/${id}/historial`);
  }

  // El propio candidato retira SU postulación (solo mientras sigue en
  // RECIBIDA -- el backend es quien valida esa regla, aquí solo se llama).
  retirar(id: number) {
    return this.http.put<PostulacionResponse>(`${this.base}/${id}/retirar`, {});
  }

  obtenerEstadisticas() {
    return this.http.get<PostulacionEstadisticasResponse>(`${this.base}/estadisticas`);
  }
}
