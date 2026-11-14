import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';
import { protegerContraPestanaDuplicada } from './app/core/utils/pestana-duplicada';

// Antes de arrancar Angular: una pestaña duplicada no hereda la sesión.
protegerContraPestanaDuplicada();

bootstrapApplication(AppComponent, appConfig)
  .catch((err) => console.error(err));
