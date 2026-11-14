import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

// Pantalla de cambio OBLIGATORIO de contraseña: a ella llega cualquier
// usuario (CANDIDATO, RRHH o ADMIN) que entró con la contraseña temporal
// que le generó el Administrador desde "Usuario y Acceso" (ver
// authGuard, que redirige aquí mientras auth.debeCambiarContrasenaActual()
// sea true). No tiene menú ni "Cancelar" a propósito: no se puede volver
// a ver ninguna otra pantalla hasta completar el cambio.
@Component({
  selector: 'app-cambiar-contrasena',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './cambiar-contrasena.component.html',
  styleUrl: '../auth.shared.css',
})
export class CambiarContrasenaComponent {
  cargando = false;
  errorMsg = '';

  private fb = inject(FormBuilder);

  form = this.fb.group({
    contrasenaActual: ['', Validators.required],
    contrasenaNueva: ['', [Validators.required, Validators.minLength(8)]],
    confirmarContrasena: ['', Validators.required],
  });

  constructor(private auth: AuthService, private router: Router) {}

  enviar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { contrasenaActual, contrasenaNueva, confirmarContrasena } = this.form.getRawValue();
    if (contrasenaNueva !== confirmarContrasena) {
      this.errorMsg = 'La confirmación no coincide con la nueva contraseña.';
      return;
    }

    this.cargando = true;
    this.errorMsg = '';
    this.auth.cambiarContrasena({ contrasenaActual: contrasenaActual!, contrasenaNueva: contrasenaNueva! }).subscribe({
      next: () => {
        this.cargando = false;
        const rol = this.auth.rolActual();
        const destino = rol === 'RRHH' ? '/vacantes' : rol === 'ADMIN' ? '/usuarios' : '/vacantes';
        this.router.navigate([destino]);
      },
      error: (err) => {
        this.cargando = false;
        this.errorMsg = err?.error?.mensaje || 'No se pudo cambiar la contraseña.';
      },
    });
  }
}
