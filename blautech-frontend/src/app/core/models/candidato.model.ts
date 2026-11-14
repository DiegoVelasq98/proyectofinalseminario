// Refleja com.blautech.reclutamiento.candidato.* (entidades y DTOs reales)

export interface ExperienciaLaboral {
  idExperiencia?: number;
  empresa: string;
  puesto: string;
  descripcion?: string;
  fechaInicio: string; // ISO date (yyyy-MM-dd)
  fechaFin?: string;
  actualmenteTrabaja: boolean;
}

export interface Educacion {
  idEducacion?: number;
  institucion: string;
  titulo: string;
  nivelAcademico?: string;
  fechaInicio?: string;
  fechaFin?: string;
  estado?: string; // EN_CURSO / FINALIZADO
}

export interface CandidatoHabilidad {
  idCandidatoHabilidad?: number;
  idHabilidad: number;
  nivel?: string; // BASICO / INTERMEDIO / AVANZADO
  aniosExperiencia?: number;
}

// PUT /candidatos/habilidades/{id} -> le pone/cambia el nivel a una
// habilidad que el candidato ya tiene (típicamente una que detectó el
// currículum y quedó sin nivel).
export interface ActualizarNivelHabilidadRequest {
  nivel: string;
}

export interface CandidatoPerfilRequest {
  telefono?: string;
  fechaNacimiento?: string;
  direccion?: string;
  ciudad?: string;
  pais?: string;
  descripcionPerfil?: string;
  disponibilidad?: string;
}

export interface CandidatoPerfilResponse {
  idCandidato: number;
  nombre: string;
  apellido: string;
  correo: string;
  telefono?: string;
  fechaNacimiento?: string;
  direccion?: string;
  ciudad?: string;
  pais?: string;
  descripcionPerfil?: string;
  disponibilidad?: string;
  experiencias: ExperienciaLaboral[];
  educaciones: Educacion[];
  habilidades: CandidatoHabilidad[];
  // Del currículum más reciente, si tiene alguno registrado.
  curriculumNombreArchivo?: string;
  curriculumFechaCarga?: string;
}

// La subida real del archivo se manda como multipart/form-data (ver
// CandidatoService.agregarCurriculum), no como JSON -- por eso ya no hay
// un "CurriculumRequest" con nombre/ruta escritos a mano.

// Refleja com.blautech.reclutamiento.candidato.dto.CurriculumProcesadoResponse
export interface CurriculumProcesadoResponse {
  idCurriculum: number;
  nombreArchivo: string;
  fechaCarga: string;
  nombreCompleto: string;
  profesion: string;
  experiencia: string;
  educacion: string;
  habilidadesDetectadas: string[];
  habilidadesAgregadasAlPerfil: string[];
  // De las agregadas, las que no existían en el catálogo de la plataforma
  // y se crearon automáticamente a partir de lo que la IA detectó en el
  // texto del currículum.
  habilidadesNuevasEnCatalogo: string[];
}

export interface Habilidad {
  idHabilidad: number;
  nombre: string;
  descripcion?: string;
}
