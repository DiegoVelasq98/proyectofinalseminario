import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { NavContextService } from '../../../core/services/nav-context.service';
import { PostulacionService } from '../../../core/services/postulacion.service';
import { AnalisisService } from '../../../core/services/analisis.service';
import { CandidatoService } from '../../../core/services/candidato.service';
import { HabilidadService } from '../../../core/services/habilidad.service';
import { PostulacionResponse, AnalisisResponse } from '../../../core/models/postulacion.model';
import { CandidatoPerfilResponse, Habilidad } from '../../../core/models/candidato.model';
import { aniosExperienciaTotal, iniciales, verOdescargarBlob } from '../../../core/utils/candidato-utils';

const ESTADOS = ['RECIBIDA', 'EN_REVISION', 'PREOSELECCIONADO', 'RECHAZADA', 'CONTRATADO'];

@Component({
  selector: 'app-postulacion-detalle',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './postulacion-detalle.component.html',
  styleUrl: './postulacion-detalle.component.css',
})
export class PostulacionDetalleComponent implements OnInit {
  idPostulacion!: number;
  postulacion: PostulacionResponse | null = null;
  candidato: CandidatoPerfilResponse | null = null;
  analisis: AnalisisResponse | null = null;
  habilidadesCatalogo: Habilidad[] = [];

  cargando = true;
  noEncontrada = false;
  generando = false;
  cambiandoEstado = false;
  mostrarSelectorEstado = false;
  estados = ESTADOS;
  mensajeEstado: { tipo: 'exito' | 'error'; texto: string } | null = null;

  constructor(
    private router: Router,
    public nav: NavContextService,
    private postulacionService: PostulacionService,
    private analisisService: AnalisisService,
    private candidatoService: CandidatoService,
    private habilidadService: HabilidadService
  ) {}

  ngOnInit(): void {
    const ctx = this.nav.leer('/postulaciones/detalle');
    this.idPostulacion = Number(ctx?.id);
    const idVacante = Number(ctx?.vacante);

    this.habilidadService.listar().subscribe((h) => (this.habilidadesCatalogo = h));

    if (!idVacante) {
      this.noEncontrada = true;
      this.cargando = false;
      return;
    }

    this.postulacionService.listarPorVacante(idVacante).subscribe({
      next: (lista) => {
        const encontrada = lista.find((p) => p.idPostulacion === this.idPostulacion);
        if (!encontrada) {
          this.noEncontrada = true;
          this.cargando = false;
          return;
        }
        this.postulacion = encontrada;
        this.cargarCandidatoYAnalisis(encontrada);
      },
      error: () => {
        this.noEncontrada = true;
        this.cargando = false;
      },
    });
  }

  private cargarCandidatoYAnalisis(p: PostulacionResponse): void {
    this.candidatoService.obtenerPorId(p.idCandidato).subscribe({
      next: (c) => {
        this.candidato = c;
        this.cargando = false;
      },
      error: () => (this.cargando = false),
    });
    this.analisisService.obtener(p.idPostulacion).subscribe({
      next: (a) => (this.analisis = a),
      error: () => (this.analisis = null),
    });
  }

  nombreHabilidad(id: number): string {
    return this.habilidadesCatalogo.find((h) => h.idHabilidad === id)?.nombre ?? `#${id}`;
  }

  get anios(): number {
    return aniosExperienciaTotal(this.candidato);
  }

  get siglas(): string {
    return this.candidato ? iniciales(this.candidato.nombre, this.candidato.apellido) : '';
  }

  claseCompatibilidad(pct: number): string {
    if (pct >= 75) return 'compat-alta';
    if (pct >= 45) return 'compat-media';
    return 'compat-baja';
  }


  generarAnalisis(): void {
    if (!this.postulacion) return;
    this.generando = true;
    this.analisisService.generar(this.postulacion.idPostulacion).subscribe({
      next: (a) => {
        this.analisis = a;
        this.generando = false;
      },
      error: () => (this.generando = false),
    });
  }

  private etiquetaEstado(estado: string): string {
    const etiquetas: Record<string, string> = {
      RECIBIDA: 'Recibida',
      EN_REVISION: 'En revisión',
      PREOSELECCIONADO: 'Preseleccionado para entrevista',
      CONTRATADO: 'Contratado',
      RECHAZADA: 'Rechazada',
    };
    return etiquetas[estado] ?? estado;
  }

  cambiarEstado(estadoNuevo: string): void {
    if (!this.postulacion) return;
    this.cambiandoEstado = true;
    this.mensajeEstado = null;
    this.postulacionService.cambiarEstado(this.postulacion.idPostulacion, { estadoNuevo }).subscribe({
      next: (actualizada) => {
        this.postulacion = actualizada;
        this.cambiandoEstado = false;
        this.mostrarSelectorEstado = false;
        this.mensajeEstado = { tipo: 'exito', texto: `Estado actualizado a "${this.etiquetaEstado(estadoNuevo)}".` };
        setTimeout(() => (this.mensajeEstado = null), 4000);
      },
      error: () => {
        this.cambiandoEstado = false;
        this.mensajeEstado = { tipo: 'error', texto: 'No se pudo actualizar el estado. Intenta de nuevo.' };
      },
    });
  }

  claseBadge(estado: string): string {
    return 'badge badge-' + estado.toLowerCase();
  }

  irAAnalisis(): void {
    if (!this.postulacion) return;
    this.nav.ir('/analisis', { vacante: this.postulacion.idVacante, postulacion: this.postulacion.idPostulacion });
  }

  descargarCv(): void {
    if (!this.candidato) return;
    this.candidatoService
      .descargarCurriculumDeCandidato(this.candidato.idCandidato)
      .subscribe((blob) => verOdescargarBlob(blob, this.candidato!.curriculumNombreArchivo || 'curriculum'));
  }
}
