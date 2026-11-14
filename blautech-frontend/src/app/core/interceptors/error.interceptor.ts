import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

// Maneja de forma centralizada las respuestas 401/403 del backend, que
// es exactamente donde Spring Security aplica la seguridad real (ver
// SecurityConfig, @PreAuthorize y las validaciones de dueño del dato en
// los Service, como en PostulacionService.obtenerHistorial). Antes de
// este interceptor, cada componente tenía que manejar por su cuenta qué
// hacer si el backend rechazaba la petición -- y la mayoría no lo hacía,
// así que un 401/403 se quedaba solo en la consola sin que la persona
// entendiera qué pasó.
//
// 401 (no autenticado / token vencido o inválido) -> cierra la sesión
// local y manda a /login: el token que Angular tiene guardado ya no
// sirve, así que no tiene caso mantenerlo.
//
// 403 (autenticado, pero sin permiso sobre ESTE recurso) -> manda a
// /no-autorizado. Es distinto de 401: aquí la sesión sigue siendo
// válida, solo que esta acción en particular no le corresponde a este
// usuario (ej. un candidato pidiendo el historial de otro).
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
        if (error.status === 401) {
          auth.logout();
          router.navigate(['/login']);
        } else if (error.status === 403) {
          router.navigate(['/no-autorizado']);
        }
      }
      // Siempre se vuelve a lanzar el error: así cada componente que ya
      // maneja su propio mensaje (ej. "correo o contraseña incorrectos"
      // en el login, que es un 400, no un 401/403) sigue funcionando
      // exactamente igual que antes.
      return throwError(() => error);
    })
  );
};
