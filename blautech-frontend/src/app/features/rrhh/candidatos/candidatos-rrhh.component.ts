import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { NavContextService } from '../../../core/services/nav-context.service';
import { CandidatoService } from '../../../core/services/candidato.service';
import { HabilidadService } from '../../../core/services/habilidad.service';
import { PostulacionService } from '../../../core/services/postulacion.service';
import { CandidatoPerfilResponse, Habilidad } from '../../../core/models/candidato.model';
import { PostulacionResponse } from '../../../core/models/postulacion.model';
import { aniosExperienciaTotal, formacionPrincipal, iniciales, exportarCsv } from '../../../core/utils/candidato-utils';

@Component({
  selector: 'app-candidatos-rrhh',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './candidatos-rrhh.component.html',
  styleUrl: './candidatos-rrhh.component.css',
})
export class CandidatosRrhhComponent implements OnInit {
  candidatos: CandidatoPerfilResponse[] = [];
  habilidadesCatalogo: Habilidad[] = [];
  // Postulaciones de cada candidato, para mostrar "X postulación(es)" y
  // para poder abrir directamente la más reciente desde "Ver detalle".
  postulacionesPorCandidato: Record<number, PostulacionResponse[]> = {};
  cargando = true;
  busqueda = '';

  // Un candidato cuyas postulaciones ya quedaron todas resueltas
  // (CONTRATADO o RECHAZADA) ya no tiene ningún proceso vivo -- por
  // defecto se oculta para no estorbar en el día a día, pero sigue
  // existiendo en el sistema y se puede ver con el filtro 'TODOS'.
  filtro: 'ACTIVOS' | 'TODOS' = 'ACTIVOS';

  constructor(
    private candidatoService: CandidatoService,
    private habilidadService: HabilidadService,
    private postulacionService: PostulacionService,
    public nav: NavContextService
  ) {}

  ngOnInit(): void {
    this.habilidadService.listar().subscribe((h) => (this.habilidadesCatalogo = h));
    this.candidatoService.listarTodos().subscribe({
      next: (c) => {
        this.candidatos = c;
        this.cargando = false;
        this.cargarPostulaciones(c);
      },
      error: () => (this.cargando = false),
    });
  }

  private cargarPostulaciones(candidatos: CandidatoPerfilResponse[]): void {
    if (!candidatos.length) return;
    const llamadas = candidatos.map((c) =>
      this.postulacionService.listarPorCandidato(c.idCandidato).pipe(catchError(() => of([])))
    );
    forkJoin(llamadas).subscribe((listas) => {
      listas.forEach((lista, i) => {
        this.postulacionesPorCandidato[candidatos[i].idCandidato] = lista;
      });
    });
  }

  // true solo cuando el candidato tiene al menos una postulación Y
  // TODAS ya están en un estado terminal -- un candidato sin ninguna
  // postulación (recién registrado) NO cuenta como "cerrado", sigue
  // siendo un prospecto disponible.
  procesoCerrado(c: CandidatoPerfilResponse): boolean {
    const lista = this.postulaciones(c);
    return lista.length > 0 && lista.every((p) => p.estado === 'CONTRATADO' || p.estado === 'RECHAZADA');
  }

  setFiltro(f: 'ACTIVOS' | 'TODOS'): void {
    this.filtro = f;
  }

  get filtrados(): CandidatoPerfilResponse[] {
    let lista = this.candidatos;
    if (this.filtro === 'ACTIVOS') {
      lista = lista.filter((c) => !this.procesoCerrado(c));
    }
    const q = this.busqueda.trim().toLowerCase();
    if (!q) return lista;
    return lista.filter((c) => {
      const nombreCompleto = `${c.nombre} ${c.apellido}`.toLowerCase();
      const habilidadesTexto = c.habilidades.map((h) => this.nombreHabilidad(h.idHabilidad).toLowerCase()).join(' ');
      return nombreCompleto.includes(q) || habilidadesTexto.includes(q) || formacionPrincipal(c).toLowerCase().includes(q);
    });
  }

  nombreHabilidad(id: number): string {
    return this.habilidadesCatalogo.find((h) => h.idHabilidad === id)?.nombre ?? `#${id}`;
  }

  anios(c: CandidatoPerfilResponse): number {
    return aniosExperienciaTotal(c);
  }

  formacion(c: CandidatoPerfilResponse): string {
    return formacionPrincipal(c);
  }

  siglas(c: CandidatoPerfilResponse): string {
    return iniciales(c.nombre, c.apellido);
  }

  postulaciones(c: CandidatoPerfilResponse): PostulacionResponse[] {
    return this.postulacionesPorCandidato[c.idCandidato] || [];
  }

  // La más reciente, para abrir directo su detalle completo (perfil +
  // postulación + análisis), igual que hace "Abrir" en Postulaciones.
  postulacionMasReciente(c: CandidatoPerfilResponse): PostulacionResponse | null {
    const lista = this.postulaciones(c);
    if (!lista.length) return null;
    return [...lista].sort((a, b) => (b.fechaPostulacion || '').localeCompare(a.fechaPostulacion || ''))[0];
  }

  // Exporta exactamente lo que se está viendo (ya filtrado por "Con
  // proceso activo"/"Todos" y por la búsqueda) -- 100% con datos ya
  // cargados, sin pedirle nada nuevo al backend.
  exportarCsv(): void {
    const encabezados = ['Nombre', 'Correo', 'Formación', 'Años de experiencia', 'Habilidades', '# Postulaciones'];
    const filas = this.filtrados.map((c) => [
      `${c.nombre} ${c.apellido}`,
      c.correo,
      this.formacion(c),
      this.anios(c),
      c.habilidades.map((h) => this.nombreHabilidad(h.idHabilidad)).join('; '),
      this.postulaciones(c).length,
    ]);
    exportarCsv('candidatos', encabezados, filas);
  }
}
