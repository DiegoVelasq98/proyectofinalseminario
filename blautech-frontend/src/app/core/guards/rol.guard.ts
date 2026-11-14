import { inject } from '@angular/core';
import { CanActivateFn, CanMatchFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

// Guard parametrizable por rol: se usa como rolGuard(['RRHH']) en las rutas.
// Corresponde a las mismas reglas que ya aplica @PreAuthorize en el backend
// (VacanteController, PostulacionController, etc.) -- aquí solo evitamos
// que el usuario VEA una pantalla que no le corresponde; la seguridad real
// la sigue aplicando el backend con el token JWT.
export function rolGuard(rolesPermitidos: string[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (!auth.estaAutenticado()) {
      router.navigate(['/login']);
      return false;
    }

    if (rolesPermitidos.includes(auth.rolActual() ?? '')) return true;

    router.navigate(['/no-autorizado']);
    return false;
  };
}

// Variante para `canMatch`: se usa cuando dos roles distintos comparten la
// MISMA url (ej. /vacantes muestra un componente distinto para CANDIDATO
// que para RRHH) y así la URL no revela el rol del usuario. El Router
// prueba en orden las rutas con el mismo path y usa la primera cuyo
// canMatch devuelva true; si ninguna coincide, cae al wildcard ('**').
export function rolMatch(rolesPermitidos: string[]): CanMatchFn {
  return () => {
    const auth = inject(AuthService);
    return rolesPermitidos.includes(auth.rolActual() ?? '');
  };
}
