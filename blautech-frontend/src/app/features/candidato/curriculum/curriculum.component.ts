import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CandidatoService } from '../../../core/services/candidato.service';
import { CandidatoPerfilResponse, CurriculumProcesadoResponse } from '../../../core/models/candidato.model';
import { descargarBlob } from '../../../core/utils/candidato-utils';

type Paso = 'inicial' | 'seleccionado' | 'procesando' | 'completado' | 'error';

@Component({
  selector: 'app-curriculum',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './curriculum.component.html',
  styleUrl: './curriculum.component.css',
})
export class CurriculumComponent implements OnInit {
  perfil: CandidatoPerfilResponse | null = null;
  cargandoPerfil = true;

  archivoSeleccionado: File | null = null;
  paso: Paso = 'inicial';
  errorMsg = '';
  resultado: CurriculumProcesadoResponse | null = null;
  arrastrando = false;

  constructor(private candidatoService: CandidatoService) {}

  ngOnInit(): void {
    this.candidatoService.obtenerPerfil().subscribe({
      next: (p) => {
        this.perfil = p;
        this.cargandoPerfil = false;
      },
      error: () => (this.cargandoPerfil = false),
    });
  }

  // La subida y el procesamiento pasan en UNA sola llamada real al
  // backend (no hay eventos intermedios de verdad), así que mientras
  // esa llamada está en curso los pasos 2 y 3 se muestran juntos como
  // "En proceso" -- no se simula un avance que no está pasando.
  get pasos(): { label: string; estado: string }[] {
    const enProceso = this.paso === 'procesando' ? 'En proceso' : 'Pendiente';
    const completado = this.paso === 'completado';
    const error = this.paso === 'error';
    return [
      { label: 'Seleccionando archivo', estado: this.paso === 'inicial' ? 'Pendiente' : 'Completado' },
      { label: 'Cargando currículum', estado: completado ? 'Completado' : error ? 'Error' : enProceso },
      { label: 'Procesando información', estado: completado ? 'Completado' : error ? 'Error' : enProceso },
      { label: 'Procesamiento completado', estado: completado ? 'Completado' : error ? 'Error' : 'Pendiente' },
    ];
  }

  onArchivoElegido(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files[0];
    this.tomarArchivo(file || null);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.arrastrando = false;
    const file = event.dataTransfer?.files?.[0] || null;
    this.tomarArchivo(file);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.arrastrando = true;
  }

  onDragLeave(): void {
    this.arrastrando = false;
  }

  private tomarArchivo(file: File | null): void {
    if (!file) return;
    const extension = file.name.split('.').pop()?.toLowerCase();
    if (extension !== 'pdf' && extension !== 'docx') {
      this.errorMsg = 'Solo se permiten archivos PDF o DOCX.';
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.errorMsg = 'El archivo supera el máximo permitido de 5 MB.';
      return;
    }
    this.errorMsg = '';
    this.archivoSeleccionado = file;
    this.paso = 'seleccionado';
    this.resultado = null;
  }

  cargar(): void {
    if (!this.archivoSeleccionado) return;
    this.paso = 'procesando';
    this.errorMsg = '';
    this.candidatoService.agregarCurriculum(this.archivoSeleccionado).subscribe({
      next: (r) => {
        this.resultado = r;
        this.paso = 'completado';
        if (this.perfil) {
          this.perfil.curriculumNombreArchivo = r.nombreArchivo;
          this.perfil.curriculumFechaCarga = r.fechaCarga;
        }
      },
      error: (err) => {
        this.paso = 'error';
        this.errorMsg = err?.error?.mensaje || 'No se pudo procesar el currículum.';
      },
    });
  }

  descargarActual(): void {
    this.candidatoService.descargarMiCurriculum().subscribe((blob) =>
      descargarBlob(blob, this.perfil?.curriculumNombreArchivo || 'curriculum')
    );
  }
}
