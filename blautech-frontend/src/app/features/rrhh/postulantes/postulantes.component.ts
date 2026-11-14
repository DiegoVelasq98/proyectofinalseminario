import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
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
import { aniosExperienciaTotal, exportarCsv } from '../../../core/utils/candidato-utils';

const ESTADOS = ['RECIBIDA', 'EN_REVISION', 'PREOSELECCIONADO', 'RECHAZADA', 'CONTRATADO'];

@Component({
  selector: 'app-postulantes',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './postulantes.component.html',
  styleUrl: './postulantes.component.css',
})
export class PostulantesComponent implements OnInit {
  // Fijo cuando se entra desde el detalle de una vacante específica
  // (/vacantes/postulantes); si no viene, se muestra el
  // selector global de vacante (/postulaciones).
  vacanteFija = false;
  vacantes: VacanteResponse[] = [];
  idVacanteSeleccionada: number | null = null;

  postulaciones: PostulacionResponse[] = [];
  candidatosPorId: Record<number, CandidatoPerfilResponse> = {};
  analisisPorPostulacion: Record<number, AnalisisResponse | 'sin-datos' | undefined> = {};
  generando: number | null = null;
  cargando = true;
  estados = ESTADOS;

  // Orden y filtro por compatibilidad -- para que RRHH pueda ver primero
  // a los mejores candidatos sin tener que revisar uno por uno.
  orden: 'compatibilidad' | 'fecha' = 'compatibilidad';
  filtroClasificacion: 'TODAS' | 'ALTA' | 'MEDIA' | 'BAJA' = 'TODAS';

  // Comparación lado a lado -- hasta 3 postulantes de la misma vacante a
  // la vez, para no revisar uno por uno cuando ya se llegó a los
  // finalistas. Guarda el ORDEN en que se seleccionaron (un array, no un
  // Set) para que las columnas del comparador no se reordenen solas.
  readonly MAX_COMPARAR = 3;
  idsComparar: number[] = [];
  mostrarComparacion = false;

  constructor(
    private router: Router,
    public nav: NavContextService,
    private postulacionService: PostulacionService,
    private analisisService: AnalisisService,
    private vacanteService: VacanteService,
    private candidatoService: CandidatoService
  ) {}

  ngOnInit(): void {
    // /vacantes/postulantes (vacante fija) conserva su contexto tras F5;
    // /postulaciones (selector global) solo usa una preselección de un solo uso.
    const esVacanteFija = this.router.url.split('?')[0] === '/vacantes/postulantes';
    const ctx = esVacanteFija ? this.nav.leer('/vacantes/postulantes') : this.nav.consumir('/postulaciones');
    const idParam = esVacanteFija ? ctx?.id ?? null : null;
    const idQuery = !esVacanteFija ? ctx?.vacante ?? null : null;

    if (esVacanteFija && !idParam) {
      this.router.navigateByUrl('/vacantes');
      return;
    }

    this.candidatoService.listarTodos().pipe(catchError(() => of([]))).subscribe((candidatos) => {
      candidatos.forEach((c) => (this.candidatosPorId[c.idCandidato] = c));
    });

    if (idParam) {
      this.vacanteFija = true;
      this.idVacanteSeleccionada = Number(idParam);
      this.cargarPostulaciones();
    } else {
      this.vacanteService.listarTodas().subscribe((v) => {
        this.vacantes = v;
        const preseleccionada = idQuery ? Number(idQuery) : v.find((x) => x.estado === 'ACTIVA')?.idVacante;
        this.idVacanteSeleccionada = preseleccionada ?? v[0]?.idVacante ?? null;
        if (this.idVacanteSeleccionada) {
          this.cargarPostulaciones();
        } else {
          this.cargando = false;
        }
      });
    }
  }

  cambiarVacante(id: number): void {
    this.idVacanteSeleccionada = Number(id);
    this.cargarPostulaciones();
  }

  cargarPostulaciones(): void {
    if (!this.idVacanteSeleccionada) return;
    this.cargando = true;
    this.postulacionService.listarPorVacante(this.idVacanteSeleccionada).subscribe({
      next: (p) => {
        this.postulaciones = p;
        this.cargando = false;
        this.cargarAnalisisExistentes(p);
      },
      error: () => (this.cargando = false),
    });
  }

  private cargarAnalisisExistentes(postulaciones: PostulacionResponse[]): void {
    const llamadas = postulaciones.map((p) =>
      this.analisisService.obtener(p.idPostulacion).pipe(catchError(() => of(null)))
    );
    if (!llamadas.length) return;
    forkJoin(llamadas).subscribe((resultados) => {
      resultados.forEach((a, i) => {
        if (a) this.analisisPorPostulacion[postulaciones[i].idPostulacion] = a;
      });
    });
  }

  cambiarEstado(id: number, estadoNuevo: string): void {
    this.postulacionService.cambiarEstado(id, { estadoNuevo }).subscribe(() => this.cargarPostulaciones());
  }

  generarAnalisis(idPostulacion: number): void {
    this.generando = idPostulacion;
    this.analisisService.generar(idPostulacion).subscribe({
      next: (a) => {
        this.analisisPorPostulacion[idPostulacion] = a;
        this.generando = null;
      },
      error: () => {
        this.analisisPorPostulacion[idPostulacion] = 'sin-datos';
        this.generando = null;
      },
    });
  }

  aniosDelCandidato(idCandidato: number): number {
    return aniosExperienciaTotal(this.candidatosPorId[idCandidato]);
  }

  claseCompatibilidad(pct: number): string {
    if (pct >= 75) return 'compat-alta';
    if (pct >= 45) return 'compat-media';
    return 'compat-baja';
  }

  claseBadge(estado: string): string {
    return 'badge badge-' + estado.toLowerCase();
  }

  // Lista que realmente se pinta en la tabla: la original filtrada por
  // clasificación y ordenada según lo que elija RRHH en la barra de
  // arriba. No toca `postulaciones` para no perder el orden original
  // que manda el backend.
  get postulacionesMostradas(): PostulacionResponse[] {
    let lista = this.postulaciones;

    if (this.filtroClasificacion !== 'TODAS') {
      lista = lista.filter((p) => {
        const a = this.analisisPorPostulacion[p.idPostulacion];
        return a && a !== 'sin-datos' && a.clasificacion === this.filtroClasificacion;
      });
    }

    if (this.orden === 'compatibilidad') {
      lista = [...lista].sort((a, b) => this.porcentajeDe(b) - this.porcentajeDe(a));
    } else {
      lista = [...lista].sort(
        (a, b) => new Date(b.fechaPostulacion).getTime() - new Date(a.fechaPostulacion).getTime()
      );
    }

    return lista;
  }

  private porcentajeDe(p: PostulacionResponse): number {
    const a = this.analisisPorPostulacion[p.idPostulacion];
    return a && a !== 'sin-datos' ? a.porcentajeCoincidencia : -1;
  }

  // Exporta exactamente lo que se está viendo en la tabla (ya filtrado y
  // ordenado por "postulacionesMostradas") -- sin pedirle nada nuevo al
  // backend, se arma 100% con los datos que ya están cargados.
  enComparacion(idPostulacion: number): boolean {
    return this.idsComparar.includes(idPostulacion);
  }

  toggleComparar(idPostulacion: number): void {
    const i = this.idsComparar.indexOf(idPostulacion);
    if (i >= 0) {
      this.idsComparar.splice(i, 1);
      return;
    }
    if (this.idsComparar.length >= this.MAX_COMPARAR) return;
    this.idsComparar.push(idPostulacion);
  }

  abrirComparacion(): void {
    if (this.idsComparar.length >= 2) this.mostrarComparacion = true;
  }

  cerrarComparacion(): void {
    this.mostrarComparacion = false;
  }

  quitarDeComparacion(idPostulacion: number): void {
    this.idsComparar = this.idsComparar.filter((id) => id !== idPostulacion);
    if (this.idsComparar.length < 2) this.mostrarComparacion = false;
  }

  get postulacionesComparadas(): PostulacionResponse[] {
    return this.idsComparar
      .map((id) => this.postulaciones.find((p) => p.idPostulacion === id))
      .filter((p): p is PostulacionResponse => !!p);
  }

  analisisDe(idPostulacion: number): AnalisisResponse | null {
    const a = this.analisisPorPostulacion[idPostulacion];
    return a && a !== 'sin-datos' ? a : null;
  }

  private cumpleDetalle(porcentajeCoincidencia: number | undefined): boolean {
    return (porcentajeCoincidencia ?? 0) >= 100;
  }

  habilidadesCoincidentes(detalles: DetalleAnalisis[] | undefined): DetalleAnalisis[] {
    return (detalles ?? []).filter((d) => this.cumpleDetalle(d.porcentajeCoincidencia));
  }

  habilidadesFaltantes(detalles: DetalleAnalisis[] | undefined): DetalleAnalisis[] {
    return (detalles ?? []).filter((d) => !this.cumpleDetalle(d.porcentajeCoincidencia));
  }

  exportarCsv(): void {
    const encabezados = ['Candidato', 'Correo', 'Vacante', 'Fecha de postulación', 'Estado', '% Compatibilidad', 'Clasificación'];
    const filas = this.postulacionesMostradas.map((p) => {
      const candidato = this.candidatosPorId[p.idCandidato];
      const a = this.analisisPorPostulacion[p.idPostulacion];
      const tieneAnalisis = a && a !== 'sin-datos';
      return [
        p.nombreCandidato,
        candidato?.correo ?? '',
        p.tituloVacante,
        p.fechaPostulacion ? new Date(p.fechaPostulacion).toLocaleDateString('es-GT') : '',
        p.estado,
        tieneAnalisis ? a.porcentajeCoincidencia : '',
        tieneAnalisis ? a.clasificacion : '',
      ];
    });
    exportarCsv('postulantes', encabezados, filas);
  }
}
