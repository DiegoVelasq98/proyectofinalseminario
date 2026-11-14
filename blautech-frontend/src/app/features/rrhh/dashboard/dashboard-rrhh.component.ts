import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { VacanteService } from '../../../core/services/vacante.service';
import { PostulacionService } from '../../../core/services/postulacion.service';
import { CandidatoService } from '../../../core/services/candidato.service';
import { VacanteResponse } from '../../../core/models/vacante.model';
import { PostulacionResponse, PostulacionEstadisticasResponse } from '../../../core/models/postulacion.model';

interface FilaVacante {
  idVacante: number;
  titulo: string;
  total: number;
  pendientes: number;
}

interface FilaEmbudo {
  estado: string;
  etiqueta: string;
  cantidad: number;
  porcentajeAncho: number;
}

// Estados que cuentan como "en proceso" (no terminal) para la alerta de
// postulaciones estancadas -- RECHAZADA/CONTRATADO ya no necesitan que
// nadie les dé seguimiento.
const ESTADOS_EN_PROCESO = ['RECIBIDA', 'EN_REVISION', 'PREOSELECCIONADO'];
const ORDEN_EMBUDO: Array<{ estado: string; etiqueta: string }> = [
  { estado: 'RECIBIDA', etiqueta: 'Recibida' },
  { estado: 'EN_REVISION', etiqueta: 'En revisión' },
  { estado: 'PREOSELECCIONADO', etiqueta: 'Preseleccionado' },
  { estado: 'CONTRATADO', etiqueta: 'Contratado' },
  { estado: 'RECHAZADA', etiqueta: 'Rechazada' },
];
const DIAS_PARA_ALERTA = 7;

@Component({
  selector: 'app-dashboard-rrhh',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard-rrhh.component.html',
  styleUrl: './dashboard-rrhh.component.css',
})
export class DashboardRrhhComponent implements OnInit {
  cargando = true;
  vacantes: VacanteResponse[] = [];
  todasPostulaciones: PostulacionResponse[] = [];
  candidatosRegistrados = 0;
  filasPorVacante: FilaVacante[] = [];
  estadisticas: PostulacionEstadisticasResponse | null = null;

  constructor(
    private vacanteService: VacanteService,
    private postulacionService: PostulacionService,
    private candidatoService: CandidatoService
  ) {}

  ngOnInit(): void {
    this.cargando = true;
    this.vacanteService.listarTodas().subscribe((vacantes) => {
      this.vacantes = vacantes;

      const llamadasPostulaciones = vacantes.map((v) =>
        this.postulacionService.listarPorVacante(v.idVacante).pipe(catchError(() => of([])))
      );

      forkJoin({
        postulacionesPorVacante: vacantes.length ? forkJoin(llamadasPostulaciones) : of([]),
        candidatos: this.candidatoService.listarTodos().pipe(catchError(() => of([]))),
        estadisticas: this.postulacionService.obtenerEstadisticas().pipe(catchError(() => of(null))),
      }).subscribe(({ postulacionesPorVacante, candidatos, estadisticas }) => {
        this.candidatosRegistrados = candidatos.length;
        this.estadisticas = estadisticas;

        // Una vacante CERRADA o CANCELADA ya no es parte del trabajo del
        // día a día -- no debe seguir ocupando una fila en este resumen
        // (para eso está el listado completo de Vacantes, con su propio
        // filtro, si alguna vez se necesita ver el historial).
        this.filasPorVacante = vacantes
          .filter((v) => v.estado !== 'CERRADA' && v.estado !== 'CANCELADA')
          .map((v) => {
            const i = vacantes.indexOf(v);
            const lista = (postulacionesPorVacante as PostulacionResponse[][])[i] || [];
            return {
              idVacante: v.idVacante,
              titulo: v.titulo,
              total: lista.length,
              pendientes: lista.filter((p) => p.estado === 'RECIBIDA').length,
            };
          });

        this.todasPostulaciones = (postulacionesPorVacante as PostulacionResponse[][]).flat();
        this.cargando = false;
      });
    });
  }

  get vacantesActivas(): number {
    return this.vacantes.filter((v) => v.estado === 'ACTIVA').length;
  }

  get postulacionesRecibidas(): number {
    return this.todasPostulaciones.length;
  }

  get pendientesRevision(): number {
    return this.todasPostulaciones.filter((p) => p.estado === 'RECIBIDA').length;
  }

  get actividadReciente(): PostulacionResponse[] {
    return [...this.todasPostulaciones]
      .sort((a, b) => (b.fechaPostulacion || '').localeCompare(a.fechaPostulacion || ''))
      .slice(0, 6);
  }

  // Embudo de conversión: cuántas postulaciones hay en cada etapa, con el
  // ancho de la barra relativo a la etapa con más postulaciones (siempre
  // RECIBIDA, por ser la primera). Usa this.estadisticas (ya calculado en
  // el backend) en vez de contar otra vez del lado del cliente.
  get embudo(): FilaEmbudo[] {
    const porEstado = this.estadisticas?.porEstado ?? {};
    const maximo = Math.max(1, ...ORDEN_EMBUDO.map((e) => porEstado[e.estado] ?? 0));
    return ORDEN_EMBUDO.map((e) => {
      const cantidad = porEstado[e.estado] ?? 0;
      return {
        estado: e.estado,
        etiqueta: e.etiqueta,
        cantidad,
        porcentajeAncho: Math.round((cantidad / maximo) * 100),
      };
    });
  }

  get tiempoPromedioContratacion(): number | null {
    return this.estadisticas?.tiempoPromedioContratacionDias ?? null;
  }

  // Postulaciones que llevan más de DIAS_PARA_ALERTA días sin llegar a un
  // estado final (CONTRATADO/RECHAZADA) -- las más viejas primero, para
  // que RRHH sepa cuáles están a punto de "enfriarse".
  get postulacionesEstancadas(): Array<PostulacionResponse & { dias: number }> {
    const ahora = Date.now();
    return this.todasPostulaciones
      .filter((p) => ESTADOS_EN_PROCESO.includes(p.estado))
      .map((p) => ({ ...p, dias: Math.floor((ahora - new Date(p.fechaPostulacion).getTime()) / 86400000) }))
      .filter((p) => p.dias >= DIAS_PARA_ALERTA)
      .sort((a, b) => b.dias - a.dias)
      .slice(0, 8);
  }
}
