import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { NavContextService } from '../../../core/services/nav-context.service';
import { VacanteService } from '../../../core/services/vacante.service';
import { PostulacionService } from '../../../core/services/postulacion.service';
import { HabilidadService } from '../../../core/services/habilidad.service';
import { FormsModule } from '@angular/forms';
import { VacanteResponse, ResponsableOpcion } from '../../../core/models/vacante.model';
import { PostulacionResponse } from '../../../core/models/postulacion.model';
import { Habilidad } from '../../../core/models/candidato.model';

type Tab = 'info' | 'requisitos' | 'candidatos';

@Component({
  selector: 'app-vacante-detalle',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './vacante-detalle.component.html',
  styleUrl: './vacante-detalle.component.css',
})
export class VacanteDetalleComponent implements OnInit {
  vacante: VacanteResponse | null = null;
  postulaciones: PostulacionResponse[] = [];
  habilidadesCatalogo: Habilidad[] = [];
  cargando = true;
  tab: Tab = 'info';
  idVacante!: number;
  publicando = false;

  responsables: ResponsableOpcion[] = [];
  idResponsableSeleccionado: number | null = null;
  guardandoResponsable = false;

  constructor(
    private router: Router,
    public nav: NavContextService,
    private vacanteService: VacanteService,
    private postulacionService: PostulacionService,
    private habilidadService: HabilidadService
  ) {}

  ngOnInit(): void {
    this.idVacante = Number(this.nav.leer('/vacantes/detalle')?.id);
    if (!this.idVacante) {
      this.router.navigateByUrl('/vacantes');
      return;
    }
    this.habilidadService.listar().subscribe((h) => (this.habilidadesCatalogo = h));
    this.vacanteService.listarResponsables().subscribe((r) => (this.responsables = r));
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.vacanteService.obtener(this.idVacante).subscribe({
      next: (v) => {
        this.vacante = v;
        this.idResponsableSeleccionado = v.idUsuarioCreador ?? null;
        this.cargando = false;
      },
      error: () => (this.cargando = false),
    });
    this.postulacionService.listarPorVacante(this.idVacante).subscribe((p) => (this.postulaciones = p));
  }

  cambiarResponsableSeleccionado(id: string | number): void {
    this.idResponsableSeleccionado = Number(id);
  }

  guardarResponsable(): void {
    if (!this.vacante || !this.idResponsableSeleccionado) return;
    this.guardandoResponsable = true;
    this.vacanteService.asignarResponsable(this.vacante.idVacante, this.idResponsableSeleccionado).subscribe({
      next: (v) => {
        this.vacante = v;
        this.guardandoResponsable = false;
      },
      error: () => (this.guardandoResponsable = false),
    });
  }

  setTab(t: Tab): void {
    this.tab = t;
  }

  nombreHabilidad(id: number): string {
    return this.habilidadesCatalogo.find((h) => h.idHabilidad === id)?.nombre ?? `#${id}`;
  }

  claseBadge(estado: string): string {
    return 'badge badge-' + estado.toLowerCase();
  }

  irAnalisis(): void {
    this.nav.ir('/analisis', { vacante: this.idVacante });
  }

  cerrar(): void {
    if (!this.vacante || !confirm('¿Cerrar esta vacante?')) return;
    this.vacanteService.cerrar(this.vacante.idVacante).subscribe(() => this.cargar());
  }

  publicar(): void {
    if (!this.vacante) return;
    this.publicando = true;
    this.vacanteService.publicar(this.vacante.idVacante).subscribe({
      next: () => {
        this.publicando = false;
        this.cargar();
      },
      error: () => (this.publicando = false),
    });
  }
}
