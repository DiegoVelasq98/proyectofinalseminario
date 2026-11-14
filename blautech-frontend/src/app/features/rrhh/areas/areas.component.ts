import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AreaService } from '../../../core/services/area.service';
import { Area } from '../../../core/models/area.model';

type FiltroEstado = 'ACTIVAS' | 'TODAS';

@Component({
  selector: 'app-areas-rrhh',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './areas.component.html',
  styleUrl: './areas.component.css',
})
export class AreasRrhhComponent implements OnInit {
  areas: Area[] = [];
  cargando = true;
  filtro: FiltroEstado = 'ACTIVAS';

  // Alta de un área nueva.
  nombreNuevaArea = '';
  creando = false;
  errorMsg = '';

  // Edición inline: solo un área a la vez tiene su input abierto.
  idEnEdicion: number | null = null;
  nombreEdicion = '';
  guardandoId: number | null = null;

  constructor(private areaService: AreaService) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.areaService.listarTodas().subscribe({
      next: (a) => {
        this.areas = a;
        this.cargando = false;
      },
      error: () => (this.cargando = false),
    });
  }

  get filtradas(): Area[] {
    if (this.filtro === 'TODAS') return this.areas;
    return this.areas.filter((a) => (a.estado ?? 'ACTIVO') === 'ACTIVO');
  }

  setFiltro(f: FiltroEstado): void {
    this.filtro = f;
  }

  crear(): void {
    const nombre = this.nombreNuevaArea.trim();
    if (!nombre) return;
    this.creando = true;
    this.errorMsg = '';
    this.areaService.crear(nombre).subscribe({
      next: () => {
        this.creando = false;
        this.nombreNuevaArea = '';
        this.cargar();
      },
      error: (err) => {
        this.creando = false;
        this.errorMsg = err?.error?.mensaje || 'No se pudo crear el área.';
      },
    });
  }

  editar(area: Area): void {
    this.idEnEdicion = area.idArea;
    this.nombreEdicion = area.nombre;
    this.errorMsg = '';
  }

  cancelarEdicion(): void {
    this.idEnEdicion = null;
    this.nombreEdicion = '';
  }

  guardarEdicion(area: Area): void {
    const nombre = this.nombreEdicion.trim();
    if (!nombre) return;
    this.guardandoId = area.idArea;
    this.errorMsg = '';
    this.areaService.actualizar(area.idArea, nombre).subscribe({
      next: () => {
        this.guardandoId = null;
        this.idEnEdicion = null;
        this.cargar();
      },
      error: (err) => {
        this.guardandoId = null;
        this.errorMsg = err?.error?.mensaje || 'No se pudo guardar el cambio.';
      },
    });
  }

  desactivar(area: Area): void {
    if (!confirm(`¿Desactivar el área "${area.nombre}"? Las vacantes que ya la usan no se ven afectadas, solo deja de ofrecerse para vacantes nuevas.`)) {
      return;
    }
    this.guardandoId = area.idArea;
    this.areaService.desactivar(area.idArea).subscribe({
      next: () => {
        this.guardandoId = null;
        this.cargar();
      },
      error: () => (this.guardandoId = null),
    });
  }

  activar(area: Area): void {
    this.guardandoId = area.idArea;
    this.areaService.activar(area.idArea).subscribe({
      next: () => {
        this.guardandoId = null;
        this.cargar();
      },
      error: () => (this.guardandoId = null),
    });
  }

  claseBadge(estado: string | undefined): string {
    return 'badge badge-' + (estado ?? 'ACTIVO').toLowerCase();
  }
}
