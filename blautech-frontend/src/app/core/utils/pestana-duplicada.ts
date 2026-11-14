// ============================================================================
// Seguridad: una pestaña DUPLICADA no debe heredar la sesión.
//
// La sesión vive en sessionStorage (una por pestaña), pero el navegador, al
// usar "Duplicar pestaña", COPIA el sessionStorage de la original. Resultado:
// la copia aparecía ya iniciada con el mismo usuario.
//
// Cómo se detecta: cada pestaña tiene un id guardado en su sessionStorage y,
// mientras está abierta, deja un "latido" en localStorage (compartido entre
// pestañas). Si al cargar, el id que traigo ya tiene un latido reciente de
// OTRA pestaña viva, es que soy una copia: descarto la sesión copiada y me
// asigno un id nuevo, así que el router me manda al login.
//
// Recargar (F5) la misma pestaña NO se confunde: al descargar la página se
// borra su latido (pagehide), así que al volver a cargar no hay latido vivo.
// Debe llamarse en main.ts ANTES de arrancar Angular.
// ============================================================================
const CLAVE_ID = 'blautech_tab_id';
const PREFIJO_LATIDO = 'blautech_tab_viva_';
// Todo lo que vive en sessionStorage y pertenece a la sesión del usuario.
const CLAVES_DE_SESION = ['blautech_session', 'blautech_nav_ctx'];

const LATIDO_CADA_MS = 1500;
const VIGENCIA_LATIDO_MS = 6000;
const LIMPIEZA_LATIDOS_VIEJOS_MS = 60_000;

function nuevoId(): string {
  return typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : Date.now() + '-' + Math.random().toString(36).slice(2);
}

export function protegerContraPestanaDuplicada(): void {
  try {
    const ahora = Date.now();

    // Limpieza de latidos que quedaron huérfanos (pestañas cerradas a la
    // fuerza, caídas del navegador, etc.).
    for (let i = localStorage.length - 1; i >= 0; i--) {
      const k = localStorage.key(i);
      if (k && k.startsWith(PREFIJO_LATIDO)) {
        const ultimo = Number(localStorage.getItem(k) || 0);
        if (ahora - ultimo > LIMPIEZA_LATIDOS_VIEJOS_MS) localStorage.removeItem(k);
      }
    }

    let id = sessionStorage.getItem(CLAVE_ID);
    if (id) {
      const ultimo = Number(localStorage.getItem(PREFIJO_LATIDO + id) || 0);
      if (ahora - ultimo < VIGENCIA_LATIDO_MS) {
        // Otra pestaña viva ya usa este id: soy una copia. Sin sesión heredada.
        CLAVES_DE_SESION.forEach((k) => sessionStorage.removeItem(k));
        id = null;
      }
    }
    if (!id) {
      id = nuevoId();
      sessionStorage.setItem(CLAVE_ID, id);
    }

    const miClave = PREFIJO_LATIDO + id;
    const latir = () => localStorage.setItem(miClave, String(Date.now()));
    latir();
    setInterval(latir, LATIDO_CADA_MS);
    window.addEventListener('pagehide', () => localStorage.removeItem(miClave));
    window.addEventListener('pageshow', latir);
  } catch {
    /* sin storage disponible: no hay sesión que proteger */
  }
}
