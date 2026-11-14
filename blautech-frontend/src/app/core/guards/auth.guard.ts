import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

const RUTA_CAMBIO_OBLIGATORIO = '/cambiar-contrasena';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.estaAutenticado()) {
    router.navigate(['/login']);
    return false;
  }

  // Si entró con una contraseña temporal (reiniciada por el Admin), no
  // lo dejamos ver ninguna otra pantalla hasta que la cambie -- salvo,
  // claro, la pantalla donde la va a cambiar.
  if (auth.debeCambiarContrasenaActual() && !state.url.startsWith(RUTA_CAMBIO_OBLIGATORIO)) {
    router.navigate([RUTA_CAMBIO_OBLIGATORIO]);
    return false;
  }

  return true;
};
