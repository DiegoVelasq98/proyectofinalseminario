import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    // El orden importa: authInterceptor va primero para que el token ya
    // esté puesto en el header antes de que la petición salga; luego
    // errorInterceptor observa la respuesta (o el error) de esa misma
    // petición ya autenticada.
    provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
  ],
};
