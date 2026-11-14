import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { NavContextService } from '../../../core/services/nav-context.service';
import { PostulacionService } from '../../../core/services/postulacion.service';
import { AnalisisService } from '../../../core/services/analisis.service';
import { VacanteService } from '../../../core/services/vacante.service';
import { CandidatoService } from '../../../core/services/candidato.service';
import { PostulacionResponse, AnalisisResponse, DetalleAnalisis } from '../../../core/models/postulacion.model';
import { VacanteResponse } from '../../../core/models/vacante.model';
import { CandidatoPerfilResponse } from '../../../core/models/candidato.model';
import { aniosExperienciaTotal } from '../../../core/utils/candidato-utils';

interface FilaRanking {
  postulacion: PostulacionResponse;
  analisis: AnalisisResponse | null;
}

@Component({
  selector: 'app-analisis-candidatos',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './analisis-candidatos.component.html',
  styleUrl: './analisis-candidatos.component.css',
})
export class AnalisisCandidatosComponent implements OnInit {
  vacantes: VacanteResponse[] = [];
  idVacanteSeleccionada: number | null = null;
  candidatosPorId: Record<number, CandidatoPerfilResponse> = {};

  filas: FilaRanking[] = [];
  seleccionada: FilaRanking | null = null;
  idPostulacionAResaltar: number | null = null;
  cargando = true;
  ejecutando = false;

  constructor(
    public nav: NavContextService,
    private postulacionService: PostulacionService,
    private analisisService: AnalisisService,
    private vacanteService: VacanteService,
    private candidatoService: CandidatoService
  ) {}

  ngOnInit(): void {
    const ctx = this.nav.consumir('/analisis');
    const idQuery = ctx?.vacante ?? null;
    this.idPostulacionAResaltar = ctx?.postulacion ?? null;

    this.candidatoService.listarTodos().pipe(catchError(() => of([]))).subscribe((candidatos) => {
      candidatos.forEach((c) => (this.candidatosPorId[c.idCandidato] = c));
    });

    this.vacanteService.listarTodas().subscribe((v) => {
      this.vacantes = v;
      const preseleccionada = idQuery ? Number(idQuery) : v.find((x) => x.estado === 'ACTIVA')?.idVacante;
      this.idVacanteSeleccionada = preseleccionada ?? v[0]?.idVacante ?? null;
      if (this.idVacanteSeleccionada) {
        this.cargar();
      } else {
        this.cargando = false;
      }
    });
  }

  cambiarVacante(id: number): void {
    this.idVacanteSeleccionada = Number(id);
    this.cargar();
  }

  cargar(): void {
    if (!this.idVacanteSeleccionada) return;
    this.cargando = true;
    this.seleccionada = null;
    this.postulacionService.listarPorVacante(this.idVacanteSeleccionada).subscribe((postulaciones) => {
      if (!postulaciones.length) {
        this.filas = [];
        this.cargando = false;
        return;
      }
      const llamadas = postulaciones.map((p) =>
        this.analisisService.obtener(p.idPostulacion).pipe(catchError(() => of(null)))
      );
      forkJoin(llamadas).subscribe((resultados) => {
        this.filas = postulaciones
          .map((p, i) => ({ postulacion: p, analisis: resultados[i] }))
          .sort((a, b) => (b.analisis?.porcentajeCoincidencia ?? -1) - (a.analisis?.porcentajeCoincidencia ?? -1));

        const resaltada = this.idPostulacionAResaltar
          ? this.filas.find((f) => f.postulacion.idPostulacion === this.idPostulacionAResaltar)
          : null;
        this.seleccionada = resaltada ?? this.filas.find((f) => f.analisis) ?? this.filas[0] ?? null;
        this.idPostulacionAResaltar = null; // solo se usa para el primer cargue
        this.cargando = false;
      });
    });
  }

  seleccionar(f: FilaRanking): void {
    this.seleccionada = f;
  }

  ejecutarAnalisis(f: FilaRanking): void {
    this.ejecutando = true;
    this.analisisService.generar(f.postulacion.idPostulacion).subscribe({
      next: (a) => {
        f.analisis = a;
        this.filas = [...this.filas].sort(
          (a1, b1) => (b1.analisis?.porcentajeCoincidencia ?? -1) - (a1.analisis?.porcentajeCoincidencia ?? -1)
        );
        this.seleccionada = f;
        this.ejecutando = false;
      },
      error: () => (this.ejecutando = false),
    });
  }

  candidatoDe(f: FilaRanking): CandidatoPerfilResponse | undefined {
    return this.candidatosPorId[f.postulacion.idCandidato];
  }

  aniosDe(f: FilaRanking): number {
    return aniosExperienciaTotal(this.candidatoDe(f));
  }

  coincidencias(f: FilaRanking): number {
    return f.analisis?.detalles.filter((d) => (d.porcentajeCoincidencia ?? 0) >= 100).length ?? 0;
  }

  claseCompatibilidad(pct: number): string {
    if (pct >= 75) return 'compat-alta';
    if (pct >= 45) return 'compat-media';
    return 'compat-baja';
  }

  cumple(porcentajeCoincidencia: number | undefined): boolean {
    return (porcentajeCoincidencia ?? 0) >= 100;
  }

  // Un criterio es "parcial" cuando el candidato tiene algo de la
  // habilidad (la tiene registrada, o tiene un nivel por debajo del
  // pedido) pero no llega al 100% -- distinto de no tenerla en lo
  // absoluto, que es 0%.
  esParcial(porcentajeCoincidencia: number | undefined): boolean {
    const pct = porcentajeCoincidencia ?? 0;
    return pct > 0 && pct < 100;
  }

  textoResultado(porcentajeCoincidencia: number | undefined): string {
    if (this.cumple(porcentajeCoincidencia)) return 'Cumple';
    if (this.esParcial(porcentajeCoincidencia)) return 'Cumple parcialmente';
    return 'No cumple';
  }

  claseResultado(porcentajeCoincidencia: number | undefined): string {
    if (this.cumple(porcentajeCoincidencia)) return 'badge badge-activa';
    if (this.esParcial(porcentajeCoincidencia)) return 'badge badge-parcial';
    return 'badge badge-rechazada';
  }

  habilidadesCoincidentes(detalles: DetalleAnalisis[]): DetalleAnalisis[] {
    return detalles.filter((d) => this.cumple(d.porcentajeCoincidencia));
  }

  habilidadesParciales(detalles: DetalleAnalisis[]): DetalleAnalisis[] {
    return detalles.filter((d) => this.esParcial(d.porcentajeCoincidencia));
  }

  // Ahora sí "faltantes" son solo las que el candidato no tiene en lo
  // absoluto (0%) -- las parciales ya no caen aquí, tienen su propio
  // apartado arriba.
  habilidadesFaltantes(detalles: DetalleAnalisis[]): DetalleAnalisis[] {
    return detalles.filter((d) => (d.porcentajeCoincidencia ?? 0) <= 0);
  }
}
