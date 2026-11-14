import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CambiarContrasenaRequest,
  LoginRequest,
  LoginResponse,
  RegistroRequest,
  UsuarioResponse,
} from '../models/auth.model';

const STORAGE_KEY = 'blautech_session';

interface Sesion {
  token: string;
  idUsuario: number;
  nombre: string;
  correo: string;
  rol: string;
  debeCambiarContrasena: boolean;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private sesion = signal<Sesion | null>(this.leerSesionGuardada());

  readonly usuarioActual = computed(() => this.sesion());
  readonly estaAutenticado = computed(() => !!this.sesion());
  readonly rolActual = computed(() => this.sesion()?.rol ?? null);
  readonly debeCambiarContrasenaActual = computed(() => this.sesion()?.debeCambiarContrasena ?? false);

  constructor(private http: HttpClient) {}

  private leerSesionGuardada(): Sesion | null {
    try {
      const raw = sessionStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as Sesion) : null;
    } catch {
      return null;
    }
  }

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/auth/login`, request).pipe(
      tap((res) => {
        const s: Sesion = {
          token: res.token,
          idUsuario: res.idUsuario,
          nombre: res.nombre,
          correo: res.correo,
          rol: res.rol,
          debeCambiarContrasena: res.debeCambiarContrasena,
        };
        sessionStorage.setItem(STORAGE_KEY, JSON.stringify(s));
        this.sesion.set(s);
      })
    );
  }

  // PUT /mi-cuenta/contrasena -> lo usa la pantalla de cambio obligatorio
  // (y, a futuro, cualquier pantalla de "cambiar mi contraseña" normal).
  // Al tener éxito apaga la bandera en la sesión guardada, sin necesidad
  // de volver a hacer login para que el guard deje pasar al resto del sistema.
  cambiarContrasena(request: CambiarContrasenaRequest): Observable<void> {
    return this.http.put<void>(`${environment.apiUrl}/mi-cuenta/contrasena`, request).pipe(
      tap(() => this.marcarContrasenaActualizada())
    );
  }

  private marcarContrasenaActualizada(): void {
    const actual = this.sesion();
    if (!actual) return;
    const actualizada: Sesion = { ...actual, debeCambiarContrasena: false };
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(actualizada));
    this.sesion.set(actualizada);
  }

  registrar(request: RegistroRequest): Observable<UsuarioResponse> {
    return this.http.post<UsuarioResponse>(`${environment.apiUrl}/auth/registro`, request);
  }

  logout(): void {
    sessionStorage.removeItem(STORAGE_KEY);
    this.sesion.set(null);
  }

  obtenerToken(): string | null {
    return this.sesion()?.token ?? null;
  }
}
