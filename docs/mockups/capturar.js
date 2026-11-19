/**
 * Genera las capturas PNG de los mockups con Playwright (Chromium).
 *
 *   npm run capturar            → todas las capturas
 *   npm run capturar -- HU-07   → solo las que contienen "HU-07" en el nombre
 *
 * Sirve la carpeta html/ por HTTP local para que los mosaicos del mapa
 * (OpenStreetMap y Esri) carguen igual que en un navegador real.
 */
const http = require('http');
const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');

const RAIZ = path.join(__dirname, 'html');
const SALIDA = path.join(__dirname, 'img');
const PUERTO = 4173;

// [archivo de salida(s), página, estado, opciones]
// Si una pantalla cubre varias HU, se lista un nombre por HU (una copia por HU).
const CAPTURAS = [
  { nombres: ['00-guia-de-estilo'], pagina: '00-guia-de-estilo.html', paginaCompleta: true },

  { nombres: ['HU-01-inicio-sesion'], pagina: '01-acceso.html' },
  { nombres: ['HU-01-inicio-sesion-error'], pagina: '01-acceso.html', estado: 'error' },
  { nombres: ['HU-01-registro'], pagina: '01-acceso.html', estado: 'registro' },
  { nombres: ['HU-01-registro-exitoso'], pagina: '01-acceso.html', estado: 'registro-exitoso' },

  { nombres: ['HU-02-menu-por-rol'], pagina: '02-menu-por-rol.html' },
  { nombres: ['HU-02-usuarios-y-roles'], pagina: '02-menu-por-rol.html', estado: 'superadmin' },
  { nombres: ['HU-02-permisos-insuficientes'], pagina: '02-menu-por-rol.html', estado: 'permisos-insuficientes' },

  { nombres: ['HU-03-mi-perfil'], pagina: '03-mi-perfil.html' },
  { nombres: ['HU-03-mi-perfil-foto'], pagina: '03-mi-perfil.html', estado: 'foto' },
  { nombres: ['HU-03-mi-perfil-error'], pagina: '03-mi-perfil.html', estado: 'error' },

  { nombres: ['HU-04-enlazar-qr'], pagina: '04-enlazar-dispositivo.html', estado: 'qr' },
  { nombres: ['HU-04-enlazar-serial'], pagina: '04-enlazar-dispositivo.html', estado: 'serial' },
  { nombres: ['HU-04-enlazar-exito'], pagina: '04-enlazar-dispositivo.html', estado: 'exito' },
  { nombres: ['HU-04-enlazar-error'], pagina: '04-enlazar-dispositivo.html', estado: 'error' },

  { nombres: ['HU-05-registrar-vehiculo'], pagina: '05-registrar-activo.html', estado: 'vehiculo' },
  { nombres: ['HU-05-registrar-familiar'], pagina: '05-registrar-activo.html', estado: 'familiar' },
  { nombres: ['HU-05-registrar-mascota'], pagina: '05-registrar-activo.html', estado: 'mascota' },
  { nombres: ['HU-05-registrar-exito'], pagina: '05-registrar-activo.html', estado: 'exito' },
  { nombres: ['HU-05-registrar-error'], pagina: '05-registrar-activo.html', estado: 'error' },

  { nombres: ['HU-06-mis-dispositivos'], pagina: '06-mis-dispositivos.html' },

  { nombres: ['HU-07-monitoreo', 'HU-08-motor-kilometraje', 'HU-09-vista-estandar'], pagina: '07-monitoreo.html' },
  { nombres: ['HU-07-ubicacion-no-disponible'], pagina: '07-monitoreo.html', estado: 'sin-ubicacion' },
  { nombres: ['HU-08-motor-no-disponible'], pagina: '07-monitoreo.html', estado: 'motor-no-disponible' },
  { nombres: ['HU-09-vista-satelital'], pagina: '07-monitoreo.html', estado: 'satelite' },
  { nombres: ['HU-09-opcion-no-disponible'], pagina: '07-monitoreo.html', estado: 'capa-no-disponible' },

  { nombres: ['HU-10-geocercas', 'HU-11-geocercas-estados'], pagina: '08-geocercas.html' },
  { nombres: ['HU-10-crear-geocerca'], pagina: '08-geocercas.html', estado: 'crear' },
  { nombres: ['HU-10-crear-geocerca-error'], pagina: '08-geocercas.html', estado: 'error' },
  { nombres: ['HU-11-silenciar-geocerca'], pagina: '08-geocercas.html', estado: 'silenciar' },

  { nombres: ['HU-12-centro-alertas', 'HU-13-alerta-sos'], pagina: '09-centro-alertas.html' },
  { nombres: ['HU-12-alertas-geocerca'], pagina: '09-centro-alertas.html', estado: 'geocerca' },
  { nombres: ['HU-13-detalle-sos'], pagina: '09-centro-alertas.html', estado: 'sos' },
  { nombres: ['HU-13-sos-inconsistente'], pagina: '09-centro-alertas.html', estado: 'superadmin' },
  { nombres: ['HU-15-exceso-velocidad'], pagina: '09-centro-alertas.html', estado: 'velocidad' },
  { nombres: ['HU-15-limite-no-configurado'], pagina: '09-centro-alertas.html', estado: 'limite-no-configurado' },

  { nombres: ['HU-14-control-motor'], pagina: '10-control-motor.html' },
  { nombres: ['HU-14-confirmar-orden'], pagina: '10-control-motor.html', estado: 'confirmar' },
  { nombres: ['HU-14-orden-enviada'], pagina: '10-control-motor.html', estado: 'enviada' },
  { nombres: ['HU-14-orden-fallida'], pagina: '10-control-motor.html', estado: 'fallida' },
  { nombres: ['HU-14-permisos-insuficientes'], pagina: '10-control-motor.html', estado: 'permisos-insuficientes' },

  { nombres: ['HU-16-recorrido'], pagina: '11-recorrido.html' },
  { nombres: ['HU-16-recorrido-sin-datos'], pagina: '11-recorrido.html', estado: 'sin-datos' },
  { nombres: ['HU-16-recorrido-error'], pagina: '11-recorrido.html', estado: 'error' },

  { nombres: ['HU-17-informe-pdf'], pagina: '12-informes-pdf.html' },
  { nombres: ['HU-17-informe-sin-datos'], pagina: '12-informes-pdf.html', estado: 'sin-datos' },
  { nombres: ['HU-17-permisos-insuficientes'], pagina: '12-informes-pdf.html', estado: 'permisos-insuficientes' },

  { nombres: ['HU-18-subflotas'], pagina: '13-subflotas.html' },
  { nombres: ['HU-18-crear-subflota'], pagina: '13-subflotas.html', estado: 'crear' },
  { nombres: ['HU-18-crear-subflota-error'], pagina: '13-subflotas.html', estado: 'error' },
];

const TIPOS = { '.html': 'text/html; charset=utf-8', '.css': 'text/css', '.js': 'text/javascript', '.png': 'image/png', '.svg': 'image/svg+xml', '.jpg': 'image/jpeg' };

function servidor() {
  return http.createServer((req, res) => {
    const ruta = path.join(RAIZ, decodeURIComponent(req.url.split('?')[0]));
    if (!ruta.startsWith(RAIZ) || !fs.existsSync(ruta) || fs.statSync(ruta).isDirectory()) {
      res.writeHead(404); return res.end();
    }
    res.writeHead(200, { 'Content-Type': TIPOS[path.extname(ruta)] || 'application/octet-stream' });
    fs.createReadStream(ruta).pipe(res);
  }).listen(PUERTO);
}

(async () => {
  const filtro = process.argv[2];
  const lista = CAPTURAS.filter((c) => !filtro || c.nombres.some((n) => n.includes(filtro)));
  fs.mkdirSync(SALIDA, { recursive: true });

  const srv = servidor();
  const navegador = await chromium.launch();
  const contexto = await navegador.newContext({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 2, locale: 'es-CO' });
  const pagina = await contexto.newPage();
  pagina.on('pageerror', (e) => console.error('  Error JS:', e.message));

  for (const c of lista) {
    const url = `http://localhost:${PUERTO}/${c.pagina}${c.estado ? `?estado=${c.estado}` : ''}`;
    await pagina.goto(url, { waitUntil: 'networkidle' });
    await pagina.evaluate(() => document.fonts.ready);
    await pagina.waitForTimeout(1500);
    const primero = path.join(SALIDA, `${c.nombres[0]}.png`);
    await pagina.screenshot({ path: primero, fullPage: !!c.paginaCompleta });
    for (const otro of c.nombres.slice(1)) fs.copyFileSync(primero, path.join(SALIDA, `${otro}.png`));
    console.log(`✓ ${c.nombres.join(', ')}`);
  }

  await navegador.close();
  srv.close();
})();
