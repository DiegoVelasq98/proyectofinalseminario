import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent {
  cargando = false;
  errorMsg = '';

  private fb = inject(FormBuilder);

  form = this.fb.group({
    correo: ['', [Validators.required, Validators.email]],
    contrasena: ['', [Validators.required]],
  });

  constructor(private auth: AuthService, private router: Router) {}

  enviar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.cargando = true;
    this.errorMsg = '';
    this.auth.login(this.form.getRawValue() as any).subscribe({
      next: (res) => {
        this.cargando = false;

        if (res.debeCambiarContrasena) {
          // Entró con la contraseña temporal que le generó el Admin:
          // tiene que cambiarla antes de ver cualquier otra pantalla.
          this.router.navigate(['/cambiar-contrasena']);
          return;
        }

        const destino =
          res.rol === 'RRHH' ? '/vacantes' : res.rol === 'ADMIN' ? '/usuarios' : '/vacantes';
        this.router.navigate([destino]);
      },
      error: (err) => {
        this.cargando = false;
        this.errorMsg = err?.error?.mensaje || 'Correo o contraseña incorrectos.';
      },
    });
  }
}
