// Refleja com.blautech.reclutamiento.postulacion.dto.* y .analisis.dto.*

export interface PostulacionRequest {
  idVacante: number;
}

export interface PostulacionResponse {
  idPostulacion: number;
  idVacante: number;
  tituloVacante: string;
  idCandidato: number;
  nombreCandidato: string;
  fechaPostulacion: string;
  estado: string; // RECIBIDA / EN_REVISION / PREOSELECCIONADO / RECHAZADA / CONTRATADO
}

export interface CambiarEstadoRequest {
  estadoNuevo: string;
  observacion?: string;
}

export interface HistorialPostulacion {
  idHistorial?: number;
  estadoAnterior?: string;
  estadoNuevo: string;
  observacion?: string;
  fechaCambio?: string;
}

export interface DetalleAnalisis {
  idDetalle?: number;
  criterio?: string;
  valorCandidato?: string;
  valorEsperado?: string;
  porcentajeCoincidencia?: number;
  observacion?: string;
}

export interface HabilidadExtra {
  nombre: string;
  nivel: string;
}

export interface AnalisisResponse {
  idAnalisis: number;
  idPostulacion: number;
  porcentajeCoincidencia: number;
  clasificacion: string;
  resumen: string;
  fechaAnalisis: string;
  detalles: DetalleAnalisis[];
  habilidadesExtra?: HabilidadExtra[];
}

// Coincide con PostulacionEstadisticasResponse.java (datos agregados
// para el dashboard de RRHH).
export interface PostulacionEstadisticasResponse {
  porEstado: Record<string, number>;
  tiempoPromedioContratacionDias: number | null;
  totalContratados: number;
}
