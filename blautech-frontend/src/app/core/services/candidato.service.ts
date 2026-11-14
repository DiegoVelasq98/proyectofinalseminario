import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import {
  ActualizarNivelHabilidadRequest,
  CandidatoPerfilRequest,
  CandidatoPerfilResponse,
  CandidatoHabilidad,
  CurriculumProcesadoResponse,
  Educacion,
  ExperienciaLaboral,
} from '../models/candidato.model';

// Todos los endpoints coinciden 1:1 con CandidatoController.java real.
@Injectable({ providedIn: 'root' })
export class CandidatoService {
  private base = `${environment.apiUrl}/candidatos`;

  constructor(private http: HttpClient) {}

  crearPerfil(req: CandidatoPerfilRequest) {
    return this.http.post<CandidatoPerfilResponse>(`${this.base}/perfil`, req);
  }

  obtenerPerfil() {
    return this.http.get<CandidatoPerfilResponse>(`${this.base}/perfil`);
  }

  actualizarPerfil(req: CandidatoPerfilRequest) {
    return this.http.put<CandidatoPerfilResponse>(`${this.base}/perfil`, req);
  }

  agregarExperiencia(req: ExperienciaLaboral) {
    return this.http.post<ExperienciaLaboral>(`${this.base}/experiencia`, req);
  }

  agregarEducacion(req: Educacion) {
    return this.http.post<Educacion>(`${this.base}/educacion`, req);
  }

  // Corregir o quitar un registro ya guardado -- antes solo se podían
  // crear, nunca editar ni borrar (un error de dedo en una fecha o un
  // puesto se quedaba ahí para siempre).
  actualizarExperiencia(idExperiencia: number, req: ExperienciaLaboral) {
    return this.http.put<ExperienciaLaboral>(`${this.base}/experiencia/${idExperiencia}`, req);
  }

  eliminarExperiencia(idExperiencia: number) {
    return this.http.delete<void>(`${this.base}/experiencia/${idExperiencia}`);
  }

  actualizarEducacion(idEducacion: number, req: Educacion) {
    return this.http.put<Educacion>(`${this.base}/educacion/${idEducacion}`, req);
  }

  eliminarEducacion(idEducacion: number) {
    return this.http.delete<void>(`${this.base}/educacion/${idEducacion}`);
  }

  agregarHabilidad(req: { idHabilidad: number; nivel?: string; aniosExperiencia?: number }) {
    return this.http.post<CandidatoHabilidad>(`${this.base}/habilidades`, req);
  }

  eliminarHabilidad(idCandidatoHabilidad: number) {
    return this.http.delete<void>(`${this.base}/habilidades/${idCandidatoHabilidad}`);
  }

  actualizarNivelHabilidad(idCandidatoHabilidad: number, req: ActualizarNivelHabilidadRequest) {
    return this.http.put<CandidatoHabilidad>(`${this.base}/habilidades/${idCandidatoHabilidad}`, req);
  }

  // Subida real del archivo (PDF/DOCX): se manda como multipart/form-data,
  // el backend lo guarda en el servidor y lee su texto para detectar
  // habilidades reales -- ver CandidatoController.agregarCurriculum.
  agregarCurriculum(archivo: File) {
    const formData = new FormData();
    formData.append('archivo', archivo);
    return this.http.post<CurriculumProcesadoResponse>(`${this.base}/curriculum`, formData);
  }

  // Descarga el currículum ya registrado (propio, o de un candidato
  // puntual cuando lo pide RRHH).
  descargarMiCurriculum() {
    return this.http.get(`${this.base}/curriculum/archivo`, { responseType: 'blob' });
  }

  descargarCurriculumDeCandidato(idCandidato: number) {
    return this.http.get(`${this.base}/${idCandidato}/curriculum/archivo`, { responseType: 'blob' });
  }

  // Para RRHH: directorio completo de candidatos y detalle de uno solo.
  // Coincide 1:1 con los endpoints nuevos de CandidatoController.java.
  listarTodos() {
    return this.http.get<CandidatoPerfilResponse[]>(this.base);
  }

  obtenerPorId(id: number) {
    return this.http.get<CandidatoPerfilResponse>(`${this.base}/${id}`);
  }
}
