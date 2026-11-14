// Refleja com.blautech.reclutamiento.vacante.dto.* (contrato real)

export interface VacanteHabilidadRequest {
  idHabilidad: number;
  nivelRequerido?: string;
  esObligatoria: boolean;
}

export interface VacanteHabilidad {
  idVacanteHabilidad?: number;
  idHabilidad: number;
  // El backend (VacanteHabilidadResponse) ya manda el nombre resuelto,
  // así que en la respuesta no hace falta ir a buscar el catálogo de
  // habilidades aparte -- solo viene vacío cuando se usa esta misma
  // interfaz para mandar el request de "agregar habilidad".
  nombreHabilidad?: string;
  nivelRequerido?: string;
  esObligatoria: boolean;
}

export interface VacanteRequest {
  titulo: string;
  idArea?: number | null;
  descripcion?: string;
  conocimientosRequeridos?: string;
  ubicacion?: string;
  modalidad?: string;
  experienciaRequerida?: string;
  nivelAcademico?: string;
  fechaCierre?: string;
  // Solo aplica al crear: 'BORRADOR' o 'ACTIVA' (por defecto ACTIVA).
  estadoInicial?: string;
}

export interface VacanteResponse {
  idVacante: number;
  idArea?: number;
  areaNombre?: string;
  titulo: string;
  descripcion?: string;
  conocimientosRequeridos?: string;
  ubicacion?: string;
  modalidad?: string;
  experienciaRequerida?: string;
  nivelAcademico?: string;
  fechaPublicacion: string;
  fechaCierre?: string;
  estado: string;
  idUsuarioCreador?: number;
  creadoPor: string;
  habilidadesRequeridas: VacanteHabilidad[];
}

// Coincide con ResponsableResponse.java (usuarios RRHH activos que se
// pueden asignar como responsables de una vacante).
export interface ResponsableOpcion {
  idUsuario: number;
  nombreCompleto: string;
}
