import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { UsuarioResponse } from '../models/auth.model';
import {
  CambiarRolRequest,
  CambiarEstadoUsuarioRequest,
  ReiniciarContrasenaResponse,
} from '../models/usuario-admin.model';

// Contrato agregado para CU05 (ver usuario/UsuarioController.java en el
// backend). Consume /usuarios, protegido con @PreAuthorize("hasRole('ADMIN')").
@Injectable({ providedIn: 'root' })
export class UsuarioAdminService {
  private base = `${environment.apiUrl}/usuarios`;

  constructor(private http: HttpClient) {}

  listar() {
    return this.http.get<UsuarioResponse[]>(this.base);
  }

  cambiarRol(id: number, req: CambiarRolRequest) {
    return this.http.put<UsuarioResponse>(`${this.base}/${id}/rol`, req);
  }

  cambiarEstado(id: number, req: CambiarEstadoUsuarioRequest) {
    return this.http.put<UsuarioResponse>(`${this.base}/${id}/estado`, req);
  }

  reiniciarContrasena(id: number) {
    return this.http.put<ReiniciarContrasenaResponse>(`${this.base}/${id}/contrasena/reiniciar`, {});
  }
}
