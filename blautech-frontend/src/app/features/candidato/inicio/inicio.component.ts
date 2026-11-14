import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { CandidatoService } from '../../../core/services/candidato.service';
import { PostulacionService } from '../../../core/services/postulacion.service';
import { VacanteService } from '../../../core/services/vacante.service';
import { CandidatoPerfilResponse } from '../../../core/models/candidato.model';
import { PostulacionResponse } from '../../../core/models/postulacion.model';
import { VacanteResponse } from '../../../core/models/vacante.model';
import { porcentajePerfil } from '../../../core/utils/candidato-utils';

@Component({
  selector: 'app-inicio-candidato',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './inicio.component.html',
  styleUrl: './inicio.component.css',
})
export class InicioCandidatoComponent implements OnInit {
  perfil: CandidatoPerfilResponse | null = null;
  postulaciones: PostulacionResponse[] = [];
  vacantesRecomendadas: VacanteResponse[] = [];
  cargando = true;
  perfilInexistente = false;

  constructor(
    private candidatoService: CandidatoService,
    private postulacionService: PostulacionService,
    private vacanteService: VacanteService
  ) {}

  ngOnInit(): void {
    forkJoin({
      perfil: this.candidatoService.obtenerPerfil().pipe(catchError(() => of(null))),
      postulaciones: this.postulacionService.misPostulaciones().pipe(catchError(() => of([] as PostulacionResponse[]))),
      vacantes: this.vacanteService.listarActivas().pipe(catchError(() => of([] as VacanteResponse[]))),
    }).subscribe(({ perfil, postulaciones, vacantes }) => {
      this.perfil = perfil;
      this.perfilInexistente = !perfil;
      this.postulaciones = [...postulaciones].sort(
        (a, b) => new Date(b.fechaPostulacion).getTime() - new Date(a.fechaPostulacion).getTime()
      );
      this.vacantesRecomendadas = perfil ? this.calcularRecomendadas(perfil, vacantes) : vacantes.slice(0, 3);
      this.cargando = false;
    });
  }

  // "Recomendadas" con datos reales: vacantes activas ordenadas por
  // cuántas de sus habilidades requeridas coinciden con las habilidades
  // que el candidato ya tiene en su perfil. Si no hay ninguna coincidencia
  // (o el candidato aún no registró habilidades), se muestran las
  // vacantes activas más recientes -- nunca una predicción inventada.
  private calcularRecomendadas(perfil: CandidatoPerfilResponse, vacantes: VacanteResponse[]): VacanteResponse[] {
    const idsCandidato = new Set((perfil.habilidades || []).map((h) => h.idHabilidad));
    const conCoincidencias = vacantes
      .map((v) => ({
        vacante: v,
        coincidencias: (v.habilidadesRequeridas || []).filter((h) => idsCandidato.has(h.idHabilidad)).length,
      }))
      .filter((x) => x.coincidencias > 0)
      .sort((a, b) => b.coincidencias - a.coincidencias);

    if (conCoincidencias.length) {
      return conCoincidencias.slice(0, 3).map((x) => x.vacante);
    }
    return [...vacantes]
      .sort((a, b) => new Date(b.fechaPublicacion).getTime() - new Date(a.fechaPublicacion).getTime())
      .slice(0, 3);
  }

  claseBadge(estado: string): string {
    return 'badge badge-' + estado.toLowerCase();
  }

  get porcentajePerfil(): number {
    return porcentajePerfil(this.perfil);
  }
}
