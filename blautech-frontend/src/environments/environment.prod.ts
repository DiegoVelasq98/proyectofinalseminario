export const environment = {
  production: true,
  // Ruta relativa: Nginx (ver nginx.conf) reenvía todo /api/* al backend
  // dentro de la red interna de Docker. Así funciona sin importar si se
  // accede por IP, por dominio, con o sin HTTPS -- no hay que tocar este
  // archivo cuando cambie la dirección del servidor.
  apiUrl: '/api',
};
