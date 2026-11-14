import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CandidatoService } from '../../../core/services/candidato.service';
import { HabilidadService } from '../../../core/services/habilidad.service';
import { CandidatoPerfilResponse, Habilidad, ExperienciaLaboral, Educacion } from '../../../core/models/candidato.model';
import { seccionesEstadoPerfil, porcentajePerfil, SeccionEstadoPerfil } from '../../../core/utils/candidato-utils';

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterLink],
  templateUrl: './perfil.component.html',
  styleUrl: './perfil.component.css',
})
export class PerfilComponent implements OnInit {
  perfil: CandidatoPerfilResponse | null = null;
  existePerfil = false;
  cargando = true;
  guardando = false;
  errorMsg = '';
  okMsg = '';
  habilidadesCatalogo: Habilidad[] = [];

  // Tag-input de habilidades (mismo patrón que usa RRHH al crear una
  // vacante): se escribe el nombre y se presiona Enter; si no existe
  // todavía en el catálogo, se crea de una vez contra el backend real.
  tagTexto = '';
  nivelSeleccionado: string = 'BASICO';
  guardandoHabilidad = false;
  errorHabilidad = '';

  // id de la habilidad cuyo nivel se está guardando en este momento
  // (para deshabilitar solo ese select mientras llega la respuesta).
  actualizandoNivelId: number | null = null;

  // Edición inline de experiencia/educación: cuando no es null, el
  // formulario de arriba deja de ser "agregar nueva" y pasa a editar
  // ESE registro en particular (mismo formulario, reutilizado).
  editandoExperienciaId: number | null = null;
  editandoEducacionId: number | null = null;
  errorExperiencia = '';
  errorEducacion = '';

  private fb = inject(FormBuilder);

  formPerfil = this.fb.group({
    telefono: [''],
    fechaNacimiento: [''],
    direccion: [''],
    ciudad: [''],
    pais: ['Guatemala'],
    descripcionPerfil: [''],
    disponibilidad: ['Inmediata'],
  });

  formExperiencia = this.fb.group({
    empresa: ['', Validators.required],
    puesto: ['', Validators.required],
    descripcion: [''],
    fechaInicio: ['', Validators.required],
    fechaFin: [''],
    actualmenteTrabaja: [false],
  });

  formEducacion = this.fb.group({
    institucion: ['', Validators.required],
    titulo: ['', Validators.required],
    nivelAcademico: [''],
    fechaInicio: ['', Validators.required],
    fechaFin: [''],
    estado: ['FINALIZADO'],
  });

  constructor(
    private candidatoService: CandidatoService,
    private habilidadService: HabilidadService
  ) {}

  ngOnInit(): void {
    this.cargarPerfil(true);
    this.habilidadService.listar().subscribe((h) => (this.habilidadesCatalogo = h));
  }

  // La pantalla completa solo se oculta ("Cargando...") en la PRIMERA carga.
  // Las recargas posteriores (agregar/quitar habilidad, experiencia,
  // educación) refrescan los datos en silencio, sin el parpadeo de antes.
  cargarPerfil(inicial = false): void {
    if (inicial) this.cargando = true;
    this.candidatoService.obtenerPerfil().subscribe({
      next: (p) => {
        this.perfil = p;
        this.existePerfil = true;
        this.formPerfil.patchValue(p as any);
        this.cargando = false;
      },
      error: () => {
        // Aún no tiene perfil creado (404) -> se muestra el formulario vacío
        if (inicial) this.existePerfil = false;
        this.cargando = false;
      },
    });
  }

  guardarPerfil(): void {
    this.guardando = true;
    this.errorMsg = '';
    this.okMsg = '';
    const req = this.formPerfil.getRawValue() as any;
    const obs = this.existePerfil ? this.candidatoService.actualizarPerfil(req) : this.candidatoService.crearPerfil(req);
    obs.subscribe({
      next: (p) => {
        this.perfil = p;
        this.existePerfil = true;
        this.guardando = false;
        this.okMsg = 'Perfil guardado correctamente.';
      },
      error: (err) => {
        this.guardando = false;
        this.errorMsg = err?.error?.mensaje || 'No se pudo guardar el perfil.';
      },
    });
  }

  guardarExperiencia(): void {
    if (this.formExperiencia.invalid) {
      this.formExperiencia.markAllAsTouched();
      return;
    }
    const valorExperiencia = { ...this.formExperiencia.getRawValue() };
    // Si sigue trabajando ahí, no mandamos fecha fin aunque el campo
    // haya quedado con algo escrito antes de marcar la casilla.
    if (valorExperiencia.actualmenteTrabaja) {
      valorExperiencia.fechaFin = '';
    }
    this.errorExperiencia = '';
    const obs = this.editandoExperienciaId
      ? this.candidatoService.actualizarExperiencia(this.editandoExperienciaId, valorExperiencia as any)
      : this.candidatoService.agregarExperiencia(valorExperiencia as any);
    obs.subscribe({
      next: () => {
        this.formExperiencia.reset({ actualmenteTrabaja: false });
        this.editandoExperienciaId = null;
        this.cargarPerfil();
      },
      error: (err) => (this.errorExperiencia = err?.error?.mensaje || 'No se pudo guardar la experiencia.'),
    });
  }

  editarExperiencia(e: ExperienciaLaboral): void {
    this.editandoExperienciaId = e.idExperiencia ?? null;
    this.errorExperiencia = '';
    this.formExperiencia.patchValue({
      empresa: e.empresa,
      puesto: e.puesto,
      descripcion: e.descripcion || '',
      fechaInicio: e.fechaInicio,
      fechaFin: e.fechaFin || '',
      actualmenteTrabaja: !!e.actualmenteTrabaja,
    });
  }

  cancelarEdicionExperiencia(): void {
    this.editandoExperienciaId = null;
    this.errorExperiencia = '';
    this.formExperiencia.reset({ actualmenteTrabaja: false });
  }

  eliminarExperiencia(e: ExperienciaLaboral): void {
    if (!e.idExperiencia) return;
    if (!confirm(`¿Eliminar la experiencia en "${e.empresa}"?`)) return;
    this.candidatoService.eliminarExperiencia(e.idExperiencia).subscribe(() => {
      if (this.editandoExperienciaId === e.idExperiencia) this.cancelarEdicionExperiencia();
      this.cargarPerfil();
    });
  }

  guardarEducacion(): void {
    if (this.formEducacion.invalid) {
      this.formEducacion.markAllAsTouched();
      return;
    }
    const valorEducacion = { ...this.formEducacion.getRawValue() };
    // Si sigue en curso, no mandamos fecha fin aunque el campo haya
    // quedado con algo escrito (se oculta en el formulario, pero por si
    // acaso -- más vale ser explícitos que confiar en que no se ve).
    if (valorEducacion.estado === 'EN_CURSO') {
      valorEducacion.fechaFin = '';
    }
    this.errorEducacion = '';
    const obs = this.editandoEducacionId
      ? this.candidatoService.actualizarEducacion(this.editandoEducacionId, valorEducacion as any)
      : this.candidatoService.agregarEducacion(valorEducacion as any);
    obs.subscribe({
      next: () => {
        this.formEducacion.reset({ estado: 'FINALIZADO' });
        this.editandoEducacionId = null;
        this.cargarPerfil();
      },
      error: (err) => (this.errorEducacion = err?.error?.mensaje || 'No se pudo guardar la educación.'),
    });
  }

  editarEducacion(e: Educacion): void {
    this.editandoEducacionId = e.idEducacion ?? null;
    this.errorEducacion = '';
    this.formEducacion.patchValue({
      institucion: e.institucion,
      titulo: e.titulo,
      nivelAcademico: e.nivelAcademico || '',
      fechaInicio: e.fechaInicio || '',
      fechaFin: e.fechaFin || '',
      estado: e.estado || 'FINALIZADO',
    });
  }

  cancelarEdicionEducacion(): void {
    this.editandoEducacionId = null;
    this.errorEducacion = '';
    this.formEducacion.reset({ estado: 'FINALIZADO' });
  }

  eliminarEducacion(e: Educacion): void {
    if (!e.idEducacion) return;
    if (!confirm(`¿Eliminar "${e.titulo}" de tu perfil?`)) return;
    this.candidatoService.eliminarEducacion(e.idEducacion).subscribe(() => {
      if (this.editandoEducacionId === e.idEducacion) this.cancelarEdicionEducacion();
      this.cargarPerfil();
    });
  }

  nombreHabilidad(id: number): string {
    return this.habilidadesCatalogo.find((h) => h.idHabilidad === id)?.nombre ?? `#${id}`;
  }

  // El candidato escribe libremente lo que quiera -- una sola habilidad
  // o varias separadas por coma -- y se agregan todas de una vez. Si
  // alguna no existe todavía en el catálogo, se crea contra el backend
  // real antes de asociarla a su perfil.
  agregarPorTexto(): void {
    // Set con lo que ya tenía + un Set aparte para lo que se va agregando
    // DENTRO de este mismo lote (si escribe "Java, Java" no truena, solo
    // ignora el repetido).
    const nombres = this.tagTexto
      .split(',')
      .map((n) => n.trim())
      .filter((n) => n.length > 0);
    if (!nombres.length) return;

    this.errorHabilidad = '';
    this.guardandoHabilidad = true;
    this.procesarNombres(nombres, 0, new Set<string>());
  }

  private procesarNombres(nombres: string[], indice: number, agregadosEnEsteLote: Set<string>): void {
    if (indice >= nombres.length) {
      this.guardandoHabilidad = false;
      this.tagTexto = '';
      this.cargarPerfil();
      return;
    }

    const nombre = nombres[indice];
    const clave = nombre.toLowerCase();
    const siguiente = () => this.procesarNombres(nombres, indice + 1, agregadosEnEsteLote);

    const yaLaTiene =
      agregadosEnEsteLote.has(clave) ||
      this.perfil?.habilidades?.some((h) => this.nombreHabilidad(h.idHabilidad).toLowerCase() === clave);
    if (yaLaTiene) {
      siguiente();
      return;
    }
    agregadosEnEsteLote.add(clave);

    const existente = this.habilidadesCatalogo.find((h) => h.nombre.toLowerCase() === nombre.toLowerCase());
    if (existente) {
      this.registrarHabilidad(existente.idHabilidad, siguiente);
    } else {
      this.habilidadService.crear(nombre).subscribe({
        next: (creada) => {
          this.habilidadesCatalogo = [...this.habilidadesCatalogo, creada];
          this.registrarHabilidad(creada.idHabilidad, siguiente);
        },
        error: (err) => {
          this.guardandoHabilidad = false;
          this.errorHabilidad = err?.error?.mensaje || `No se pudo crear "${nombre}".`;
        },
      });
    }
  }

  private registrarHabilidad(idHabilidad: number, siguiente: () => void): void {
    this.candidatoService.agregarHabilidad({ idHabilidad, nivel: this.nivelSeleccionado }).subscribe({
      next: () => siguiente(),
      error: (err) => {
        this.guardandoHabilidad = false;
        this.errorHabilidad = err?.error?.mensaje || 'No se pudo agregar la habilidad.';
      },
    });
  }

  quitarHabilidad(idCandidatoHabilidad: number | undefined): void {
    if (!idCandidatoHabilidad) return;
    // Se quita de la lista local de inmediato (sin recargar toda la pantalla).
    this.candidatoService.eliminarHabilidad(idCandidatoHabilidad).subscribe(() => {
      if (this.perfil?.habilidades) {
        this.perfil = {
          ...this.perfil,
          habilidades: this.perfil.habilidades.filter((h) => h.idCandidatoHabilidad !== idCandidatoHabilidad),
        };
      }
    });
  }

  // Le pone/cambia el nivel a una habilidad que ya está en el perfil --
  // ya no hace falta borrarla y agregarla de nuevo para esto (sobre
  // todo útil para las que detectó el currículum, que llegan sin nivel).
  cambiarNivel(idCandidatoHabilidad: number | undefined, nivel: string): void {
    if (!idCandidatoHabilidad) return;
    this.errorHabilidad = '';
    this.actualizandoNivelId = idCandidatoHabilidad;
    this.candidatoService.actualizarNivelHabilidad(idCandidatoHabilidad, { nivel }).subscribe({
      next: () => {
        this.actualizandoNivelId = null;
        if (this.perfil?.habilidades) {
          this.perfil = {
            ...this.perfil,
            habilidades: this.perfil.habilidades.map((h) =>
              h.idCandidatoHabilidad === idCandidatoHabilidad ? { ...h, nivel } : h
            ),
          };
        }
      },
      error: (err) => {
        this.actualizandoNivelId = null;
        this.errorHabilidad = err?.error?.mensaje || 'No se pudo actualizar el nivel.';
      },
    });
  }

  // ------------------------------------------------------------------
  // Estado del perfil: misma lógica que usa "Inicio" (compartida en
  // core/utils/candidato-utils.ts) para que nunca queden desalineados.
  // ------------------------------------------------------------------
  get seccionesEstado(): SeccionEstadoPerfil[] {
    return seccionesEstadoPerfil(this.perfil);
  }

  get porcentajePerfil(): number {
    return this.existePerfil ? porcentajePerfil(this.perfil) : 0;
  }
}
