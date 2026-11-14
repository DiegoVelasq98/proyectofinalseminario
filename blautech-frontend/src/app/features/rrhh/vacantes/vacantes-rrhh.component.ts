import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { NavContextService } from '../../../core/services/nav-context.service';
import { VacanteService } from '../../../core/services/vacante.service';
import { PostulacionService } from '../../../core/services/postulacion.service';
import { VacanteResponse } from '../../../core/models/vacante.model';

type FiltroEstado = 'TODAS' | 'ACTIVA' | 'CERRADA' | 'BORRADOR';

@Component({
  selector: 'app-vacantes-rrhh',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './vacantes-rrhh.component.html',
  styleUrl: './vacantes-rrhh.component.css',
})
export class VacantesRrhhComponent implements OnInit {
  vacantes: VacanteResponse[] = [];
  conteoPostulaciones: Record<number, number> = {};
  cargando = true;
  // Por defecto solo mostramos ACTIVA: una vacante cerrada no debe
  // seguir estorbando en el día a día -- el usuario puede cambiar el
  // filtro a 'Todas' o 'Cerrada' cuando sí la necesite ver.
  filtro: FiltroEstado = 'ACTIVA';
  busqueda = '';
  publicando: number | null = null;

  constructor(
    private vacanteService: VacanteService,
    private postulacionService: PostulacionService,
    public nav: NavContextService
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.vacanteService.listarTodas().subscribe({
      next: (v) => {
        this.vacantes = v;
        this.cargando = false;
        this.cargarConteos(v);
      },
      error: () => (this.cargando = false),
    });
  }

  // Cuenta cuántas postulaciones tiene cada vacante consultando el
  // endpoint real por vacante (no hay un conteo agregado en el backend
  // todavía, así que se arma en el frontend con los datos existentes).
  private cargarConteos(vacantes: VacanteResponse[]): void {
    if (!vacantes.length) return;
    const llamadas = vacantes.map((v) =>
      this.postulacionService.listarPorVacante(v.idVacante).pipe(catchError(() => of([])))
    );
    forkJoin(llamadas).subscribe((listas) => {
      listas.forEach((lista, i) => {
        this.conteoPostulaciones[vacantes[i].idVacante] = lista.length;
      });
    });
  }

  get filtradas(): VacanteResponse[] {
    let lista = this.vacantes;
    if (this.filtro !== 'TODAS') {
      lista = lista.filter((v) => v.estado === this.filtro);
    }
    const q = this.busqueda.trim().toLowerCase();
    if (q) {
      lista = lista.filter(
        (v) =>
          v.titulo.toLowerCase().includes(q) ||
          (v.ubicacion || '').toLowerCase().includes(q) ||
          (v.areaNombre || '').toLowerCase().includes(q)
      );
    }
    return lista;
  }

  setFiltro(f: FiltroEstado): void {
    this.filtro = f;
  }

  cerrar(id: number): void {
    if (!confirm('¿Cerrar esta vacante? Ya no recibirá nuevas postulaciones.')) return;
    this.vacanteService.cerrar(id).subscribe(() => this.cargar());
  }

  publicar(id: number): void {
    this.publicando = id;
    this.vacanteService.publicar(id).subscribe({
      next: () => {
        this.publicando = null;
        this.cargar();
      },
      error: () => (this.publicando = null),
    });
  }

  claseBadge(estado: string): string {
    return 'badge badge-' + estado.toLowerCase();
  }
}
