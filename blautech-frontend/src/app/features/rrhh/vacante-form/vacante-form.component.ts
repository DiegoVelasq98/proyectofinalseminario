import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { NavContextService } from '../../../core/services/nav-context.service';
import { VacanteService } from '../../../core/services/vacante.service';
import { HabilidadService } from '../../../core/services/habilidad.service';
import { AreaService } from '../../../core/services/area.service';
import { VacanteHabilidad } from '../../../core/models/vacante.model';
import { Habilidad } from '../../../core/models/candidato.model';
import { Area } from '../../../core/models/area.model';

interface HabilidadPendiente {
  idHabilidad: number;
  nombre: string;
  nivelRequerido: string;
  esObligatoria: boolean;
}

@Component({
  selector: 'app-vacante-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterLink],
  templateUrl: './vacante-form.component.html',
  styleUrl: './vacante-form.component.css',
})
export class VacanteFormComponent implements OnInit {
  guardando = false;
  cargando = false;
  errorMsg = '';
  idVacante: number | null = null;

  areas: Area[] = [];
  habilidadesCatalogo: Habilidad[] = [];
  habilidadesGuardadas: VacanteHabilidad[] = []; // ya existen en el backend (modo edición)
  habilidadesPendientes: HabilidadPendiente[] = []; // se agregarán al guardar (modo creación)
  guardandoHabilidad = false;
  errorHabilidad = '';

  tagTexto = '';
  tagNivel = '';
  tagObligatoria = true;

  private fb = inject(FormBuilder);

  form = this.fb.group({
    titulo: ['', Validators.required],
    idArea: [null as number | null],
    descripcion: ['', Validators.required],
    conocimientosRequeridos: [''],
    experienciaRequerida: [''],
    nivelAcademico: [''],
    modalidad: ['Presencial'],
    ubicacion: [''],
    fechaCierre: [''],
    estadoInicial: ['ACTIVA'],
  });

  constructor(
    private vacanteService: VacanteService,
    private habilidadService: HabilidadService,
    private areaService: AreaService,
    private router: Router,
    private nav: NavContextService
  ) {}

  get modoEdicion(): boolean {
    return this.idVacante !== null;
  }

  ngOnInit(): void {
    this.areaService.listar().subscribe((a) => (this.areas = a));
    this.habilidadService.listar().subscribe((h) => (this.habilidadesCatalogo = h));

    const esEdicion = this.router.url.split('?')[0] === '/vacantes/editar';
    const idParam = esEdicion ? this.nav.leer('/vacantes/editar')?.id : null;
    if (esEdicion && !idParam) {
      this.router.navigateByUrl('/vacantes');
      return;
    }
    if (idParam) {
      this.idVacante = Number(idParam);
      this.cargando = true;
      this.vacanteService.obtener(this.idVacante).subscribe({
        next: (v) => {
          this.form.patchValue({
            titulo: v.titulo,
            idArea: v.idArea ?? null,
            descripcion: v.descripcion,
            conocimientosRequeridos: v.conocimientosRequeridos,
            ubicacion: v.ubicacion,
            modalidad: v.modalidad,
            experienciaRequerida: v.experienciaRequerida,
            nivelAcademico: v.nivelAcademico,
            fechaCierre: v.fechaCierre ? v.fechaCierre.substring(0, 10) : '',
          });
          this.habilidadesGuardadas = v.habilidadesRequeridas || [];
          this.cargando = false;
        },
        error: () => {
          this.errorMsg = 'No se pudo cargar la vacante a editar.';
          this.cargando = false;
        },
      });
    }
  }

  nombreHabilidad(id: number): string {
    return this.habilidadesCatalogo.find((h) => h.idHabilidad === id)?.nombre ?? `#${id}`;
  }

  // Se usa en el <input list="..."> como sugerencias de autocompletar.
  get nombresCatalogo(): string[] {
    return this.habilidadesCatalogo.map((h) => h.nombre);
  }

  get nombreAreaSeleccionada(): string {
    const id = this.form.get('idArea')?.value;
    return this.areas.find((a) => a.idArea === id)?.nombre || '';
  }

  // Escribe el nombre y presiona Enter (o el botón +Agregar) para
  // sumarla como etiqueta -- si el nombre no existe todavía en el
  // catálogo, se crea de una vez contra el backend real.
  agregarPorTexto(): void {
    const nombre = this.tagTexto.trim();
    if (!nombre) return;

    const existente = this.habilidadesCatalogo.find((h) => h.nombre.toLowerCase() === nombre.toLowerCase());
    if (existente) {
      this.agregarHabilidad(existente.idHabilidad, existente.nombre);
      return;
    }

    this.errorHabilidad = '';
    this.guardandoHabilidad = true;
    this.habilidadService.crear(nombre).subscribe({
      next: (creada) => {
        this.habilidadesCatalogo = [...this.habilidadesCatalogo, creada];
        this.agregarHabilidad(creada.idHabilidad, creada.nombre);
      },
      error: (err) => {
        this.guardandoHabilidad = false;
        this.errorHabilidad = err?.error?.mensaje || 'No se pudo crear esa habilidad.';
      },
    });
  }

  private agregarHabilidad(idHabilidad: number, nombre: string): void {
    const yaExiste =
      this.habilidadesGuardadas.some((h) => h.idHabilidad === idHabilidad) ||
      this.habilidadesPendientes.some((h) => h.idHabilidad === idHabilidad);
    if (yaExiste) {
      this.guardandoHabilidad = false;
      this.errorHabilidad = 'Esa habilidad ya está agregada.';
      return;
    }

    if (this.modoEdicion) {
      this.vacanteService
        .agregarHabilidad(this.idVacante!, {
          idHabilidad,
          nivelRequerido: this.tagNivel || undefined,
          esObligatoria: this.tagObligatoria,
        })
        .subscribe({
          next: (h) => {
            this.habilidadesGuardadas = [...this.habilidadesGuardadas, h];
            this.finalizarAgregado();
          },
          error: (err) => {
            this.guardandoHabilidad = false;
            this.errorHabilidad = err?.error?.mensaje || 'No se pudo agregar la habilidad.';
          },
        });
    } else {
      this.habilidadesPendientes = [
        ...this.habilidadesPendientes,
        { idHabilidad, nombre, nivelRequerido: this.tagNivel, esObligatoria: this.tagObligatoria },
      ];
      this.finalizarAgregado();
    }
  }

  private finalizarAgregado(): void {
    this.guardandoHabilidad = false;
    this.tagTexto = '';
    this.errorHabilidad = '';
  }

  quitarPendiente(idHabilidad: number): void {
    this.habilidadesPendientes = this.habilidadesPendientes.filter((h) => h.idHabilidad !== idHabilidad);
  }

  enviar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.guardando = true;
    this.errorMsg = '';
    const { estadoInicial, ...resto } = this.form.getRawValue() as any;

    if (this.modoEdicion) {
      this.vacanteService.actualizar(this.idVacante!, resto).subscribe({
        next: () => this.nav.ir('/vacantes/detalle', { id: this.idVacante! }),
        error: (err) => {
          this.guardando = false;
          this.errorMsg = err?.error?.mensaje || 'No se pudo guardar la vacante.';
        },
      });
    } else {
      this.vacanteService.crear({ ...resto, estadoInicial }).subscribe({
        next: (creada) => this.guardarHabilidadesPendientes(creada.idVacante),
        error: (err) => {
          this.guardando = false;
          this.errorMsg = err?.error?.mensaje || 'No se pudo crear la vacante.';
        },
      });
    }
  }

  // Después de crear la vacante, registra una por una las habilidades
  // que quedaron pendientes (si no hay ninguna, navega directo).
  private guardarHabilidadesPendientes(idVacante: number, indice = 0): void {
    if (indice >= this.habilidadesPendientes.length) {
      this.nav.ir('/vacantes/detalle', { id: idVacante });
      return;
    }
    const h = this.habilidadesPendientes[indice];
    this.vacanteService
      .agregarHabilidad(idVacante, {
        idHabilidad: h.idHabilidad,
        nivelRequerido: h.nivelRequerido || undefined,
        esObligatoria: h.esObligatoria,
      })
      .subscribe({
        next: () => this.guardarHabilidadesPendientes(idVacante, indice + 1),
        error: () => this.guardarHabilidadesPendientes(idVacante, indice + 1),
      });
  }
}
