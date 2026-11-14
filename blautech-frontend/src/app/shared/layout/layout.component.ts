import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.css',
})
export class LayoutComponent {
  constructor(public auth: AuthService, private router: Router) {}

  cerrarSesion(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }

  nombreRol(rol: string): string {
    const nombres: Record<string, string> = {
      CANDIDATO: 'Candidato',
      RRHH: 'Recursos Humanos',
      ADMIN: 'Administrador',
    };
    return nombres[rol] || rol;
  }

  iniciales(nombre: string): string {
    return nombre
      .split(' ')
      .map((p) => p[0])
      .join('')
      .slice(0, 2)
      .toUpperCase();
  }
}
