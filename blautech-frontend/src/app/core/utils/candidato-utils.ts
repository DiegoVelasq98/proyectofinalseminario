// Utilidades pequeñas y reutilizables para las pantallas de RRHH que
// necesitan resumir el perfil de un candidato (tarjetas, tablas,
// análisis de compatibilidad), calculadas a partir de datos reales del
// perfil -- nada de cifras inventadas.
import { CandidatoPerfilResponse } from '../models/candidato.model';

// Suma la duración de cada experiencia laboral registrada (fechaFin, o
// hoy si "actualmenteTrabaja"/sin fecha fin) y la redondea a años.
export function aniosExperienciaTotal(candidato: CandidatoPerfilResponse | null | undefined): number {
  if (!candidato || !candidato.experiencias?.length) return 0;
  const msPorAnio = 1000 * 60 * 60 * 24 * 365.25;
  let totalMs = 0;
  for (const exp of candidato.experiencias) {
    const inicio = new Date(exp.fechaInicio).getTime();
    const fin = exp.fechaFin ? new Date(exp.fechaFin).getTime() : Date.now();
    if (!isNaN(inicio) && !isNaN(fin) && fin > inicio) {
      totalMs += fin - inicio;
    }
  }
  return Math.round(totalMs / msPorAnio);
}

// La formación más reciente registrada (por fecha de inicio), usada
// como "profesión" del candidato en tarjetas y listados.
export function formacionPrincipal(candidato: CandidatoPerfilResponse | null | undefined): string {
  if (!candidato || !candidato.educaciones?.length) return 'Sin formación registrada';
  const ordenadas = [...candidato.educaciones].sort((a, b) =>
    (b.fechaInicio || '').localeCompare(a.fechaInicio || '')
  );
  return ordenadas[0].titulo;
}

export function iniciales(nombre: string, apellido: string): string {
  return `${nombre?.[0] || ''}${apellido?.[0] || ''}`.toUpperCase();
}

// Estado del perfil (mismas 4 secciones que muestra el prototipo),
// calculado en base a lo que el candidato YA tiene guardado -- se
// reutiliza tanto en "Mi perfil" como en "Inicio".
export interface SeccionEstadoPerfil {
  label: string;
  completo: boolean;
  textoEstado: string;
}

export function seccionesEstadoPerfil(candidato: CandidatoPerfilResponse | null | undefined): SeccionEstadoPerfil[] {
  const datosPersonalesCompletos = !!(
    candidato?.telefono &&
    candidato?.direccion &&
    candidato?.ciudad &&
    candidato?.pais &&
    candidato?.descripcionPerfil
  );
  const experienciaYEducacionCompletas = !!(candidato?.experiencias?.length && candidato?.educaciones?.length);
  const habilidadesCompletas = !!candidato?.habilidades?.length;
  const curriculumCargado = !!candidato?.curriculumNombreArchivo;

  return [
    { label: 'Datos personales', completo: datosPersonalesCompletos, textoEstado: datosPersonalesCompletos ? 'Completo' : 'Pendiente' },
    { label: 'Experiencia y educación', completo: experienciaYEducacionCompletas, textoEstado: experienciaYEducacionCompletas ? 'Completo' : 'Pendiente' },
    { label: 'Habilidades', completo: habilidadesCompletas, textoEstado: habilidadesCompletas ? 'Completo' : 'Pendiente' },
    { label: 'Currículum', completo: curriculumCargado, textoEstado: curriculumCargado ? 'Cargado' : 'Pendiente' },
  ];
}

export function porcentajePerfil(candidato: CandidatoPerfilResponse | null | undefined): number {
  if (!candidato) return 0;
  const secciones = seccionesEstadoPerfil(candidato);
  const completas = secciones.filter((s) => s.completo).length;
  return Math.round((completas / secciones.length) * 100);
}

// Dispara la descarga de un blob de archivo con el nombre indicado --
// se usa tanto para el candidato descargando su propio currículum como
// para RRHH revisando el de un candidato en particular.
export function descargarBlob(blob: Blob, nombreArchivo: string): void {
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = nombreArchivo || 'curriculum';
  a.click();
  window.URL.revokeObjectURL(url);
}

// Si es un PDF, lo abre embebido en una pestaña nueva del navegador (RRHH
// revisa decenas de CV al día -- descargar cada uno aparte es fricción
// innecesaria). Para otros formatos (.docx, que el navegador no puede
// renderizar) cae de vuelta a la descarga de siempre.
export function verOdescargarBlob(blob: Blob, nombreArchivo: string): void {
  if (blob.type === 'application/pdf') {
    const url = window.URL.createObjectURL(blob);
    window.open(url, '_blank');
    // No se revoca la URL de inmediato: la pestaña nueva la sigue usando
    // para mostrar el PDF. El navegador la libera solo al cerrarse.
    return;
  }
  descargarBlob(blob, nombreArchivo);
}


// Genera y descarga un .csv a partir de encabezados + filas, 100% en el
// cliente (sin pedirle nada al backend). Se usa para "Exportar CSV" en
// Postulantes y en Candidatos de RRHH.
function celdaCsv(valor: string | number | null | undefined): string {
  const texto = valor === null || valor === undefined ? '' : String(valor);
  // Si el valor tiene coma, comilla o salto de línea, hay que envolverlo
  // en comillas y duplicar las comillas internas -- si no, Excel/Sheets
  // rompe las columnas donde no debe.
  if (/[",\n]/.test(texto)) {
    return '"' + texto.replace(/"/g, '""') + '"';
  }
  return texto;
}

export function exportarCsv(nombreArchivo: string, encabezados: string[], filas: (string | number | null | undefined)[][]): void {
  const lineas = [encabezados, ...filas].map((fila) => fila.map(celdaCsv).join(','));
  // \uFEFF (BOM) al inicio: sin esto, Excel abre los acentos del español
  // como caracteres corruptos en vez de UTF-8.
  const contenido = '\uFEFF' + lineas.join('\r\n');
  const blob = new Blob([contenido], { type: 'text/csv;charset=utf-8;' });
  descargarBlob(blob, nombreArchivo.endsWith('.csv') ? nombreArchivo : `${nombreArchivo}.csv`);
}
