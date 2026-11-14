import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import {
  VacanteRequest,
  VacanteResponse,
  VacanteHabilidadRequest,
  VacanteHabilidad,
  ResponsableOpcion,
} from '../models/vacante.model';

// Coincide 1:1 con VacanteController.java real.
@Injectable({ providedIn: 'root' })
export class VacanteService {
  private base = `${environment.apiUrl}/vacantes`;

  constructor(private http: HttpClient) {}

  crear(req: VacanteRequest) {
    return this.http.post<VacanteResponse>(this.base, req);
  }

  listarActivas() {
    return this.http.get<VacanteResponse[]>(this.base);
  }

  listarTodas() {
    return this.http.get<VacanteResponse[]>(`${this.base}/todas`);
  }

  obtener(id: number) {
    return this.http.get<VacanteResponse>(`${this.base}/${id}`);
  }

  actualizar(id: number, req: VacanteRequest) {
    return this.http.put<VacanteResponse>(`${this.base}/${id}`, req);
  }

  cerrar(id: number) {
    return this.http.put<VacanteResponse>(`${this.base}/${id}/cerrar`, {});
  }

  publicar(id: number) {
    return this.http.put<VacanteResponse>(`${this.base}/${id}/publicar`, {});
  }

  agregarHabilidad(id: number, req: VacanteHabilidadRequest) {
    return this.http.post<VacanteHabilidad>(`${this.base}/${id}/habilidades`, req);
  }

  listarResponsables() {
    return this.http.get<ResponsableOpcion[]>(`${this.base}/responsables`);
  }

  asignarResponsable(id: number, idUsuarioResponsable: number) {
    return this.http.put<VacanteResponse>(`${this.base}/${id}/responsable`, { idUsuarioResponsable });
  }
}
