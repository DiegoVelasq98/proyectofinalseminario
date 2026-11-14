import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { rolGuard, rolMatch } from './core/guards/rol.guard';
import { LayoutComponent } from './shared/layout/layout.component';
import { LoginComponent } from './features/auth/login/login.component';
import { RegistroComponent } from './features/auth/registro/registro.component';
import { CambiarContrasenaComponent } from './features/auth/cambiar-contrasena/cambiar-contrasena.component';
import { NoAutorizadoComponent } from './shared/no-autorizado/no-autorizado.component';
import { InicioCandidatoComponent } from './features/candidato/inicio/inicio.component';
import { PerfilComponent } from './features/candidato/perfil/perfil.component';
import { CurriculumComponent } from './features/candidato/curriculum/curriculum.component';
import { VacantesCandidatoComponent } from './features/candidato/vacantes/vacantes.component';
import { MisPostulacionesComponent } from './features/candidato/postulaciones/postulaciones.component';
import { VacantesRrhhComponent } from './features/rrhh/vacantes/vacantes-rrhh.component';
import { VacanteFormComponent } from './features/rrhh/vacante-form/vacante-form.component';
import { VacanteDetalleComponent } from './features/rrhh/vacante-detalle/vacante-detalle.component';
import { PostulantesComponent } from './features/rrhh/postulantes/postulantes.component';
import { DashboardRrhhComponent } from './features/rrhh/dashboard/dashboard-rrhh.component';
import { CandidatosRrhhComponent } from './features/rrhh/candidatos/candidatos-rrhh.component';
import { CandidatoDetalleRrhhComponent } from './features/rrhh/candidato-detalle/candidato-detalle-rrhh.component';
import { AnalisisCandidatosComponent } from './features/rrhh/analisis/analisis-candidatos.component';
import { PostulacionDetalleComponent } from './features/rrhh/postulacion-detalle/postulacion-detalle.component';
import { UsuariosAdminComponent } from './features/admin/usuarios/usuarios.component';
import { AreasRrhhComponent } from './features/rrhh/areas/areas.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'registro', component: RegistroComponent },
  { path: 'no-autorizado', component: NoAutorizadoComponent },
  // Fuera del layout a propósito: no tiene menú ni navegación lateral,
  // es un paso obligatorio antes de ver cualquier otra pantalla cuando
  // el Admin reinició la contraseña (ver authGuard).
  { path: 'cambiar-contrasena', component: CambiarContrasenaComponent, canActivate: [authGuard] },

  {
    path: '',
    component: LayoutComponent,
    canActivate: [authGuard],
    children: [
      // Rutas "compartidas": la misma URL sirve un componente distinto
      // según el rol logueado (canMatch), para que la dirección nunca
      // revele si quien navega es candidato o personal de RRHH.
      { path: 'inicio', canMatch: [rolMatch(['CANDIDATO'])], component: InicioCandidatoComponent },
      { path: 'inicio', canMatch: [rolMatch(['RRHH'])], component: DashboardRrhhComponent },

      { path: 'vacantes', canMatch: [rolMatch(['CANDIDATO'])], component: VacantesCandidatoComponent },
      { path: 'vacantes', canMatch: [rolMatch(['RRHH'])], component: VacantesRrhhComponent },

      { path: 'postulaciones', canMatch: [rolMatch(['CANDIDATO'])], component: MisPostulacionesComponent },
      { path: 'postulaciones', canMatch: [rolMatch(['RRHH'])], component: PostulantesComponent },

      // Exclusivas de CANDIDATO
      { path: 'perfil', component: PerfilComponent, canActivate: [rolGuard(['CANDIDATO'])] },
      { path: 'curriculum', component: CurriculumComponent, canActivate: [rolGuard(['CANDIDATO'])] },

      // Exclusivas de RRHH (más específicas primero, igual que antes)
      { path: 'vacantes/nueva', component: VacanteFormComponent, canActivate: [rolGuard(['RRHH'])] },
      // Sin ids en la URL: el id viaja en NavContextService (ver ese servicio).
      { path: 'vacantes/editar', component: VacanteFormComponent, canActivate: [rolGuard(['RRHH'])] },
      { path: 'vacantes/postulantes', component: PostulantesComponent, canActivate: [rolGuard(['RRHH'])] },
      { path: 'vacantes/detalle', component: VacanteDetalleComponent, canActivate: [rolGuard(['RRHH'])] },
      { path: 'candidatos', component: CandidatosRrhhComponent, canActivate: [rolGuard(['RRHH'])] },
      { path: 'candidatos/detalle', component: CandidatoDetalleRrhhComponent, canActivate: [rolGuard(['RRHH'])] },
      { path: 'postulaciones/detalle', component: PostulacionDetalleComponent, canActivate: [rolGuard(['RRHH'])] },
      { path: 'analisis', component: AnalisisCandidatosComponent, canActivate: [rolGuard(['RRHH'])] },
      { path: 'areas', component: AreasRrhhComponent, canActivate: [rolGuard(['RRHH'])] },

      // Exclusiva de ADMIN
      { path: 'usuarios', component: UsuariosAdminComponent, canActivate: [rolGuard(['ADMIN'])] },
    ],
  },

  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: '**', redirectTo: 'login' },
];
