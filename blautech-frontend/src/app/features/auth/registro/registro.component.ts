import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './registro.component.html',
  styleUrl: '../auth.shared.css',
})
export class RegistroComponent {
  cargando = false;
  errorMsg = '';
  exito = false;

  private fb = inject(FormBuilder);

  form = this.fb.group({
    nombre: ['', Validators.required],
    apellido: ['', Validators.required],
    correo: ['', [Validators.required, Validators.email]],
    contrasena: ['', [Validators.required, Validators.minLength(8)]],
    telefono: [''],
  });

  constructor(private auth: AuthService, private router: Router) {}

  enviar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.cargando = true;
    this.errorMsg = '';
    this.auth.registrar(this.form.getRawValue() as any).subscribe({
      next: () => {
        this.cargando = false;
        this.exito = true;
        setTimeout(() => this.router.navigate(['/login']), 1500);
      },
      error: (err) => {
        this.cargando = false;
        this.errorMsg = err?.error?.mensaje || 'No se pudo completar el registro. Verifica tus datos.';
      },
    });
  }
}
