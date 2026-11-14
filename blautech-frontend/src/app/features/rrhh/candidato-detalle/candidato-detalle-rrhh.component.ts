import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { NavContextService } from '../../../core/services/nav-context.service';
import { CandidatoService } from '../../../core/services/candidato.service';
import { HabilidadService } from '../../../core/services/habilidad.service';
import { CandidatoPerfilResponse, Habilidad } from '../../../core/models/candidato.model';
import { aniosExperienciaTotal, iniciales, verOdescargarBlob } from '../../../core/utils/candidato-utils';

@Component({
  selector: 'app-candidato-detalle-rrhh',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './candidato-detalle-rrhh.component.html',
  styleUrl: './candidato-detalle-rrhh.component.css',
})
export class CandidatoDetalleRrhhComponent implements OnInit {
  candidato: CandidatoPerfilResponse | null = null;
  habilidadesCatalogo: Habilidad[] = [];
  cargando = true;

  constructor(
    private router: Router,
    private nav: NavContextService,
    private candidatoService: CandidatoService,
    private habilidadService: HabilidadService
  ) {}

  ngOnInit(): void {
    const id = Number(this.nav.leer('/candidatos/detalle')?.id);
    if (!id) {
      this.router.navigateByUrl('/candidatos');
      return;
    }
    this.habilidadService.listar().subscribe((h) => (this.habilidadesCatalogo = h));
    this.candidatoService.obtenerPorId(id).subscribe({
      next: (c) => {
        this.candidato = c;
        this.cargando = false;
      },
      error: () => (this.cargando = false),
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

  descargarCv(): void {
    if (!this.candidato) return;
    this.candidatoService
      .descargarCurriculumDeCandidato(this.candidato.idCandidato)
      .subscribe((blob) => verOdescargarBlob(blob, this.candidato!.curriculumNombreArchivo || 'curriculum'));
  }
}
