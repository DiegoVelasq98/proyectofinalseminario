import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { VacanteService } from '../../../core/services/vacante.service';
import { PostulacionService } from '../../../core/services/postulacion.service';
import { VacanteResponse } from '../../../core/models/vacante.model';

@Component({
  selector: 'app-vacantes-candidato',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './vacantes.component.html',
  styleUrl: './vacantes.component.css',
})
export class VacantesCandidatoComponent implements OnInit {
  vacantes: VacanteResponse[] = [];
  cargando = true;
  postulandoId: number | null = null;
  mensaje = '';
  errorMsg = '';

  // Vacante que se está mostrando en el modal de "Ver requisitos". No
  // hace falta pedirla al backend otra vez: listarActivas() ya trae
  // todo lo que necesita el modal (descripción, requisitos, habilidades).
  vacanteSeleccionada: VacanteResponse | null = null;

  constructor(private vacanteService: VacanteService, private postulacionService: PostulacionService) {}

  ngOnInit(): void {
    this.vacanteService.listarActivas().subscribe({
      next: (v) => {
        this.vacantes = v;
        this.cargando = false;
      },
      error: () => (this.cargando = false),
    });
  }

  postularse(idVacante: number): void {
    this.postulandoId = idVacante;
    this.mensaje = '';
    this.errorMsg = '';
    this.postulacionService.postularse({ idVacante }).subscribe({
      next: () => {
        this.postulandoId = null;
        this.mensaje = 'Postulación enviada correctamente.';
        this.vacanteSeleccionada = null; // si se postuló desde el modal, lo cerramos
      },
      error: (err) => {
        this.postulandoId = null;
        this.errorMsg = err?.error?.mensaje || 'No se pudo enviar la postulación.';
      },
    });
  }

  verRequisitos(v: VacanteResponse): void {
    this.mensaje = '';
    this.errorMsg = '';
    this.vacanteSeleccionada = v;
  }

  cerrarRequisitos(): void {
    this.vacanteSeleccionada = null;
  }
}
