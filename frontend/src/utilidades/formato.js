const LOCALE = 'es-CO';
const ZONA = 'America/Bogota';

export const ROLES = {
  SUPERADMIN: { nombre: 'SuperAdmin', clave: 'superadmin' },
  ADMIN_FLOTA_B2B: { nombre: 'Admin de Flota B2B', clave: 'admin' },
  USUARIO_HOGAR_B2C: { nombre: 'Usuario Hogar B2C', clave: 'hogar' },
};

export const TIPOS_ALERTA = {
  ENTRADA_GEOCERCA: { texto: 'Entrada a geocerca', clase: 'badge--info', icono: 'LogIn' },
  SALIDA_GEOCERCA: { texto: 'Salida de geocerca', clase: 'badge--info', icono: 'LogOut' },
  SOS: { texto: 'SOS', clase: 'badge--solid-danger', icono: 'Siren' },
  EXCESO_VELOCIDAD: { texto: 'Exceso de velocidad', clase: 'badge--warning', icono: 'Gauge' },
};

export const TIPOS_DISPOSITIVO = { GPS: 'GPS', PULSERA: 'Pulsera', COLLAR: 'Collar' };

export function iniciales(nombre = '') {
  return nombre.split(/\s+/).filter(Boolean).slice(0, 2).map((p) => p[0].toUpperCase()).join('');
}

export function numero(valor, decimales = 0) {
  if (valor === null || valor === undefined) return '—';
  return Number(valor).toLocaleString(LOCALE, { minimumFractionDigits: decimales, maximumFractionDigits: decimales });
}

export function coordenada(valor) {
  return valor === null || valor === undefined ? '—' : Number(valor).toFixed(5).replace('-', '−');
}

export function fechaHora(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleString(LOCALE, { timeZone: ZONA, day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', hour12: false });
}

export function hora(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleTimeString(LOCALE, { timeZone: ZONA, hour: '2-digit', minute: '2-digit', hour12: false });
}

export function diaCorto(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  const hoy = new Date();
  const mismoDia = (a, b) => a.toLocaleDateString(LOCALE, { timeZone: ZONA }) === b.toLocaleDateString(LOCALE, { timeZone: ZONA });
  if (mismoDia(d, hoy)) return 'Hoy';
  const ayer = new Date(hoy.getTime() - 86400000);
  if (mismoDia(d, ayer)) return 'Ayer';
  return d.toLocaleDateString(LOCALE, { timeZone: ZONA, day: 'numeric', month: 'short' });
}

export function hace(iso) {
  if (!iso) return 'sin datos';
  const s = Math.max(0, Math.round((Date.now() - new Date(iso).getTime()) / 1000));
  if (s < 60) return `hace ${s} s`;
  const m = Math.floor(s / 60);
  if (m < 60) return `hace ${m} min`;
  const h = Math.floor(m / 60);
  if (h < 24) return `hace ${h} h ${m % 60} min`;
  const d = Math.floor(h / 24);
  return `hace ${d} ${d === 1 ? 'día' : 'días'}`;
}

/** Fecha local (Bogotá) para inputs datetime-local. */
export function aInputLocal(fecha) {
  const d = new Date(fecha.getTime() - 5 * 3600000);
  return d.toISOString().slice(0, 16);
}

/** Convierte el valor de un input datetime-local (hora de Bogotá) a ISO UTC. */
export function deInputLocal(valor) {
  if (!valor) return null;
  return new Date(`${valor}:00-05:00`).toISOString();
}

export const tipoActivoClave = (tipo) => ({ VEHICULO: 'vehiculo', FAMILIAR: 'familiar', MASCOTA: 'mascota' }[tipo] || 'vehiculo');
