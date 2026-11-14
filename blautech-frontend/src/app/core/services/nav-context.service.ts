import { Injectable } from '@angular/core';
import { Router } from '@angular/router';

// Contexto de navegación: los identificadores (id de vacante, de
// postulación, de candidato) viajan aquí en lugar de ir en la URL, para
// que la barra de direcciones muestre solo el módulo (p. ej.
// /postulaciones/detalle) y nunca algo como /postulaciones/11?vacante=7.
//
// Se guarda en sessionStorage para que sobreviva a un F5 dentro de la
// misma pestaña. Cada entrada recuerda a qué pantalla (destino) estaba
// dirigida, así un contexto viejo nunca se aplica a otra pantalla.
export interface NavCtx {
  id?: number;
  vacante?: number;
  postulacion?: number;
}

interface Guardado extends NavCtx {
  destino: string;
}

const CLAVE = 'blautech_nav_ctx';

@Injectable({ providedIn: 'root' })
export class NavContextService {
  constructor(private router: Router) {}

  /** Guarda el contexto y navega a la ruta (sin ids en la URL). */
  ir(destino: string, ctx: NavCtx = {}): void {
    this.guardar({ ...ctx, destino });
    this.router.navigateByUrl(destino);
  }

  /** Lee el contexto sin borrarlo (pantallas de detalle: sobrevive a F5). */
  leer(destino: string): NavCtx | null {
    const g = this.cargarGuardado();
    return g && g.destino === destino ? g : null;
  }

  /** Lee el contexto y lo borra (preselecciones de un solo uso). */
  consumir(destino: string): NavCtx | null {
    const g = this.leer(destino);
    if (g) {
      try {
        sessionStorage.removeItem(CLAVE);
      } catch {
        /* sin storage: no pasa nada */
      }
    }
    return g;
  }

  private guardar(g: Guardado): void {
    try {
      sessionStorage.setItem(CLAVE, JSON.stringify(g));
    } catch {
      /* sin storage: la pantalla destino simplemente no tendrá contexto */
    }
  }

  private cargarGuardado(): Guardado | null {
    try {
      const raw = sessionStorage.getItem(CLAVE);
      return raw ? (JSON.parse(raw) as Guardado) : null;
    } catch {
      return null;
    }
  }
}
