import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Habilidad } from '../models/candidato.model';

@Injectable({ providedIn: 'root' })
export class HabilidadService {
  private base = `${environment.apiUrl}/habilidades`;

  constructor(private http: HttpClient) {}

  listar() {
    return this.http.get<Habilidad[]>(this.base);
  }

  // Para el selector de habilidades tipo "escribe y agrega": si el
  // usuario escribe un nombre que no existe todavía en el catálogo, se
  // crea de una vez contra el backend real (POST /habilidades ya existe).
  crear(nombre: string, descripcion?: string) {
    return this.http.post<Habilidad>(this.base, { nombre, descripcion });
  }
}
