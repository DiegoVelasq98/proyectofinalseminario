import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Area } from '../models/area.model';

@Injectable({ providedIn: 'root' })
export class AreaService {
  private base = `${environment.apiUrl}/areas`;

  constructor(private http: HttpClient) {}

  // Solo las ACTIVAS -- para llenar el <select> al crear/editar una vacante.
  listar() {
    return this.http.get<Area[]>(this.base);
  }

  // Todas (incluidas las INACTIVAS) -- para la pantalla de administración
  // de áreas, donde RRHH también necesita poder reactivar una.
  listarTodas() {
    return this.http.get<Area[]>(`${this.base}/todas`);
  }

  crear(nombre: string) {
    return this.http.post<Area>(this.base, { nombre });
  }

  actualizar(idArea: number, nombre: string) {
    return this.http.put<Area>(`${this.base}/${idArea}`, { nombre });
  }

  desactivar(idArea: number) {
    return this.http.put<Area>(`${this.base}/${idArea}/desactivar`, {});
  }

  activar(idArea: number) {
    return this.http.put<Area>(`${this.base}/${idArea}/activar`, {});
  }
}
