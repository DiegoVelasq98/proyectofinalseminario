// Contrato PROPUESTO e implementado para CU05 (Gestionar usuarios y accesos).
// No existía antes en el backend: se agregó UsuarioController siguiendo
// exactamente el mismo patrón (Controller + Service + DTO) que ya usan
// VacanteController y PostulacionController. Ver /reclutamiento (backend)
// -> usuario/UsuarioController.java.

export interface CambiarRolRequest {
  nombreRol: string; // CANDIDATO | RRHH | ADMIN
}

export interface CambiarEstadoUsuarioRequest {
  estado: string; // ACTIVO | INACTIVO
}

// Lo que responde el backend al reiniciar la contraseña de un usuario
// (PUT /usuarios/{id}/contrasena/reiniciar). contrasenaTemporal viene en
// texto plano SOLO en esta respuesta -- es la única vez que se puede ver,
// el backend ya la guardó hasheada con BCrypt.
export interface ReiniciarContrasenaResponse {
  idUsuario: number;
  correo: string;
  contrasenaTemporal: string;
}
