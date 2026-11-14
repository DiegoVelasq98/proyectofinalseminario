import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PostulacionService } from '../../../core/services/postulacion.service';
import { AnalisisService } from '../../../core/services/analisis.service';
import { PostulacionResponse, AnalisisResponse, HistorialPostulacion } from '../../../core/models/postulacion.model';

@Component({
  selector: 'app-mis-postulaciones',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './postulaciones.component.html',
  styleUrl: './postulaciones.component.css',
})
export class MisPostulacionesComponent implements OnInit {
  postulaciones: PostulacionResponse[] = [];
  cargando = true;
  analisisVisible: Record<number, AnalisisResponse | 'sin-datos' | undefined> = {};

  // Historial (bitácora de cambios) por postulación -- se pide solo
  // cuando el candidato abre "Ver historial" de esa fila, no de entrada
  // para todas (sería pedirle al backend datos que puede que nunca se
  // vean).
  historialVisible: Record<number, HistorialPostulacion[] | 'sin-datos' | undefined> = {};

  retirando: number | null = null;
  errorRetirar: Record<number, string> = {};

  constructor(private postulacionService: PostulacionService, private analisisService: AnalisisService) {}

  ngOnInit(): void {
    this.cargar();
  }

  private cargar(): void {
    this.postulacionService.misPostulaciones().subscribe({
      next: (p) => {
        this.postulaciones = p;
        this.cargando = false;
      },
      error: () => (this.cargando = false),
    });
  }

  verAnalisis(idPostulacion: number): void {
    this.analisisService.obtener(idPostulacion).subscribe({
      next: (a) => (this.analisisVisible[idPostulacion] = a),
      error: () => (this.analisisVisible[idPostulacion] = 'sin-datos'),
    });
  }

  verHistorial(idPostulacion: number): void {
    if (this.historialVisible[idPostulacion]) {
      // Ya estaba abierto -- lo que hace el botón es alternar mostrar/ocultar.
      this.historialVisible[idPostulacion] = undefined;
      return;
    }
    this.postulacionService.obtenerHistorial(idPostulacion).subscribe({
      next: (h) => (this.historialVisible[idPostulacion] = h),
      error: () => (this.historialVisible[idPostulacion] = 'sin-datos'),
    });
  }

  // Solo tiene sentido ofrecer "Retirar" mientras RRHH todavía no la ha
  // tocado -- el backend es quien de verdad impone esta regla, esto solo
  // evita mostrar un botón que de todas formas el servidor rechazaría.
  puedeRetirar(p: PostulacionResponse): boolean {
    return p.estado === 'RECIBIDA';
  }

  retirar(p: PostulacionResponse): void {
    if (!confirm(`¿Retirar tu postulación a "${p.tituloVacante}"? Esta acción no se puede deshacer.`)) {
      return;
    }
    this.retirando = p.idPostulacion;
    delete this.errorRetirar[p.idPostulacion];
    this.postulacionService.retirar(p.idPostulacion).subscribe({
      next: (actualizada) => {
        this.retirando = null;
        const i = this.postulaciones.findIndex((x) => x.idPostulacion === actualizada.idPostulacion);
        if (i >= 0) this.postulaciones[i] = actualizada;
      },
      error: (err) => {
        this.retirando = null;
        this.errorRetirar[p.idPostulacion] = err?.error?.mensaje || 'No se pudo retirar la postulación.';
      },
    });
  }

  claseBadge(estado: string): string {
    return 'badge badge-' + estado.toLowerCase();
  }
}
