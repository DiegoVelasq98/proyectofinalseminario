// Modelos que reflejan EXACTAMENTE los DTOs reales del backend
// (com.blautech.reclutamiento.auth.dto.*). No se inventó ningún campo.

export interface LoginRequest {
  correo: string;
  contrasena: string;
}

export interface RegistroRequest {
  nombre: string;
  apellido: string;
  correo: string;
  contrasena: string;
  telefono?: string;
}

export interface LoginResponse {
  token: string;
  tipo: string; // "Bearer"
  idUsuario: number;
  nombre: string;
  correo: string;
  rol: string; // CANDIDATO | RRHH | ADMIN
  debeCambiarContrasena: boolean; // true si entró con una temporal que generó el Admin
}

// Lo que manda el propio usuario logueado para cambiar su contraseña
// (PUT /mi-cuenta/contrasena). No es lo mismo que el reinicio del Admin:
// aquí hay que escribir la contraseña actual para demostrar que es suya.
export interface CambiarContrasenaRequest {
  contrasenaActual: string;
  contrasenaNueva: string;
}

export interface UsuarioResponse {
  idUsuario: number;
  nombre: string;
  apellido: string;
  correo: string;
  telefono: string;
  rol: string;
  estado: string;
  fechaCreacion: string;
}
