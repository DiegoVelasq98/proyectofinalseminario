import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-no-autorizado',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="page" style="text-align:center; padding-top: 4rem;">
      <h1>Acceso no autorizado</h1>
      <p>Tu rol no tiene permiso para ver esta pantalla.</p>
      <a class="btn btn-primario" routerLink="/login">Volver al inicio</a>
    </div>
  `,
})
export class NoAutorizadoComponent {}
