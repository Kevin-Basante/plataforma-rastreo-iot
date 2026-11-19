// Cliente HTTP de la API REST (Spring Boot). La URL base se configura con VITE_API_URL.
export const API_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/$/, '');

const CLAVE_TOKEN = 'rastreo.token';

export function obtenerToken() {
  try { return localStorage.getItem(CLAVE_TOKEN); } catch { return null; }
}

export function guardarToken(token) {
  try {
    if (token) localStorage.setItem(CLAVE_TOKEN, token);
    else localStorage.removeItem(CLAVE_TOKEN);
  } catch { /* almacenamiento no disponible */ }
}

/** Error con el mensaje, el estado HTTP y los detalles que devuelve la API. */
export class ErrorApi extends Error {
  constructor(mensaje, estado, detalles = []) {
    super(mensaje);
    this.estado = estado;
    this.detalles = detalles;
  }
}

let alExpirar = () => {};
export function alExpirarSesion(fn) { alExpirar = fn; }

async function solicitar(metodo, ruta, cuerpo, opciones = {}) {
  const cabeceras = {};
  const token = obtenerToken();
  if (token) cabeceras.Authorization = `Bearer ${token}`;
  let body;
  if (cuerpo instanceof FormData) body = cuerpo;
  else if (cuerpo !== undefined) {
    cabeceras['Content-Type'] = 'application/json';
    body = JSON.stringify(cuerpo);
  }
  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}${ruta}`, { method: metodo, headers: cabeceras, body });
  } catch {
    throw new ErrorApi('No se pudo conectar con el servidor. Si acaba de despertar, espera unos segundos y vuelve a intentarlo.', 0);
  }
  if (respuesta.status === 401 && token && !ruta.startsWith('/api/auth')) alExpirar();
  if (!respuesta.ok) {
    let datos = {};
    try { datos = await respuesta.json(); } catch { /* sin cuerpo */ }
    throw new ErrorApi(datos.mensaje || 'Ocurrió un error inesperado.', respuesta.status, datos.detalles || []);
  }
  if (opciones.blob) return respuesta.blob();
  if (respuesta.status === 204) return null;
  return respuesta.json();
}

export const api = {
  get: (ruta) => solicitar('GET', ruta),
  post: (ruta, cuerpo) => solicitar('POST', ruta, cuerpo ?? {}),
  put: (ruta, cuerpo) => solicitar('PUT', ruta, cuerpo ?? {}),
  del: (ruta) => solicitar('DELETE', ruta),
  blob: (ruta) => solicitar('GET', ruta, undefined, { blob: true }),
};

export const qs = (params) => {
  const p = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') p.set(k, v); });
  const s = p.toString();
  return s ? `?${s}` : '';
};
