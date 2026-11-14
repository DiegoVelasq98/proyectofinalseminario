import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UsuarioAdminService } from '../../../core/services/usuario-admin.service';
import { UsuarioResponse } from '../../../core/models/auth.model';
import { ReiniciarContrasenaResponse } from '../../../core/models/usuario-admin.model';

const ROLES = ['CANDIDATO', 'RRHH', 'ADMIN'];

@Component({
  selector: 'app-usuarios-admin',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './usuarios.component.html',
  styleUrl: './usuarios.component.css',
})
export class UsuariosAdminComponent implements OnInit {
  usuarios: UsuarioResponse[] = [];
  cargando = true;
  errorMsg = '';
  roles = ROLES;
  actualizando: number | null = null;

  // Resultado a mostrar en el modal justo después de reiniciar una
  // contraseña (RF16/CU05). Vive solo en memoria del componente: se
  // pierde al cerrar el modal o refrescar la página, igual que en el
  // backend, que tampoco la guarda en texto plano en ningún lado.
  reinicioResultado: ReiniciarContrasenaResponse | null = null;
  reinicioErrorMsg = '';

  constructor(private usuarioAdminService: UsuarioAdminService) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.usuarioAdminService.listar().subscribe({
      next: (u) => {
        this.usuarios = u;
        this.cargando = false;
      },
      error: (err) => {
        this.cargando = false;
        this.errorMsg = err?.error?.mensaje || 'No se pudo cargar el listado de usuarios.';
      },
    });
  }

  cambiarRol(idUsuario: number, nombreRol: string): void {
    this.actualizando = idUsuario;
    this.usuarioAdminService.cambiarRol(idUsuario, { nombreRol }).subscribe({
      next: () => {
        this.actualizando = null;
        this.cargar();
      },
      error: () => (this.actualizando = null),
    });
  }

  toggleEstado(usuario: UsuarioResponse): void {
    const nuevoEstado = usuario.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO';
    this.actualizando = usuario.idUsuario;
    this.usuarioAdminService.cambiarEstado(usuario.idUsuario, { estado: nuevoEstado }).subscribe({
      next: () => {
        this.actualizando = null;
        this.cargar();
      },
      error: () => (this.actualizando = null),
    });
  }

  reiniciarContrasena(usuario: UsuarioResponse): void {
    const confirmado = confirm(
      `¿Reiniciar la contraseña de ${usuario.nombre} ${usuario.apellido} (${usuario.correo})?\n\n` +
        'Se generará una contraseña temporal nueva y la actual dejará de funcionar.'
    );
    if (!confirmado) {
      return;
    }

    this.actualizando = usuario.idUsuario;
    this.reinicioErrorMsg = '';
    this.usuarioAdminService.reiniciarContrasena(usuario.idUsuario).subscribe({
      next: (res) => {
        this.actualizando = null;
        this.reinicioResultado = res;
      },
      error: (err) => {
        this.actualizando = null;
        this.reinicioErrorMsg = err?.error?.mensaje || 'No se pudo reiniciar la contraseña.';
      },
    });
  }

  cerrarModalReinicio(): void {
    this.reinicioResultado = null;
  }

  copiarContrasena(): void {
    if (!this.reinicioResultado) {
      return;
    }
    navigator.clipboard?.writeText(this.reinicioResultado.contrasenaTemporal).catch(() => {});
  }

  claseBadge(estado: string): string {
    return 'badge badge-' + estado.toLowerCase();
  }
}
