/* ==========================================================================
   Plataforma de Rastreo IoT — componentes compartidos de los mockups
   Estructura (barra lateral + barra superior), menú por rol, mapa y marcadores.
   ========================================================================== */
(function () {
  // Nombre del producto: cámbialo aquí y se actualiza en todas las pantallas.
  const PRODUCTO = 'Plataforma de Rastreo IoT';

  const BOGOTA = [4.6510, -74.0817];

  const ROLES = {
    superadmin: { nombre: 'Equipo de Soporte', rol: 'SuperAdmin', iniciales: 'ES', icono: 'shield-check', alertas: 2 },
    admin: { nombre: 'Andrés Gómez', rol: 'Admin de Flota B2B', empresa: 'Transportes Andinos S.A.S.', iniciales: 'AG', icono: 'building-2', alertas: 5 },
    hogar: { nombre: 'Laura Martínez', rol: 'Usuario Hogar B2C', iniciales: 'LM', icono: 'house', alertas: 3 },
  };

  // Menú por rol (HU-02). Cada grupo lista los ítems que el rol puede ver.
  const ITEMS = {
    monitoreo: { icono: 'map', texto: 'Monitoreo', href: '07-monitoreo.html' },
    alertas: { icono: 'bell-ring', texto: 'Centro de alertas', href: '09-centro-alertas.html', contador: true },
    geocercas: { icono: 'circle-dot', texto: 'Geocercas', href: '08-geocercas.html' },
    recorrido: { icono: 'route', texto: 'Recorrido', href: '11-recorrido.html' },
    dispositivos: { icono: 'radio-tower', texto: 'Dispositivos', href: '06-mis-dispositivos.html' },
    misDispositivos: { icono: 'radio-tower', texto: 'Mis dispositivos', href: '06-mis-dispositivos.html' },
    activos: { icono: 'shapes', texto: 'Activos monitoreados', href: '05-registrar-activo.html' },
    subflotas: { icono: 'layers', texto: 'Sub-flotas', href: '13-subflotas.html' },
    informes: { icono: 'file-text', texto: 'Informes PDF', href: '12-informes-pdf.html' },
    usuarios: { icono: 'users', texto: 'Usuarios y roles', href: '02-menu-por-rol.html' },
    perfil: { icono: 'user-round', texto: 'Mi perfil', href: '03-mi-perfil.html' },
  };
  const MENU = {
    superadmin: [
      ['Operación', ['monitoreo', 'alertas', 'geocercas', 'recorrido']],
      ['Gestión', ['dispositivos', 'activos', 'subflotas', 'informes']],
      ['Administración', ['usuarios']],
      ['Cuenta', ['perfil']],
    ],
    admin: [
      ['Operación', ['monitoreo', 'alertas', 'geocercas', 'recorrido']],
      ['Mi flota', ['dispositivos', 'activos', 'subflotas', 'informes']],
      ['Cuenta', ['perfil']],
    ],
    hogar: [
      ['Operación', ['monitoreo', 'alertas', 'geocercas', 'recorrido']],
      ['Lo mío', ['misDispositivos', 'activos', 'informes']],
      ['Cuenta', ['perfil']],
    ],
  };

  const params = new URLSearchParams(location.search);
  const estado = params.get('estado') || 'normal';

  const esc = (s) => String(s).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
  const ico = (nombre, clase) => `<i data-lucide="${nombre}"${clase ? ` class="${clase}"` : ''}></i>`;

  // Logotipo: retícula de geocerca (zona circular con centro).
  const logo = (clase = 'brand__mark') => `
    <svg class="${clase}" viewBox="0 0 32 32" aria-hidden="true">
      <rect width="32" height="32" rx="8" fill="#0E7C66"/>
      <circle cx="16" cy="16" r="10" fill="none" stroke="#fff" stroke-opacity=".45" stroke-width="1.5" stroke-dasharray="2.5 2.5"/>
      <circle cx="16" cy="16" r="5" fill="none" stroke="#fff" stroke-width="2"/>
      <circle cx="16" cy="16" r="1.75" fill="#fff"/>
      <path d="M16 3v4M16 25v4M3 16h4M25 16h4" stroke="#fff" stroke-width="1.5" stroke-linecap="round"/>
    </svg>`;

  // Motivo para estados vacíos: la misma retícula, en trazo.
  const marcaVacia = `
    <svg class="empty__mark" viewBox="0 0 56 56" fill="none" stroke="currentColor" aria-hidden="true">
      <circle cx="28" cy="28" r="22" stroke-opacity=".35" stroke-width="1.5" stroke-dasharray="3 4"/>
      <circle cx="28" cy="28" r="11" stroke-width="2"/>
      <circle cx="28" cy="28" r="2.5" fill="currentColor" stroke="none"/>
      <path d="M28 2v8M28 46v8M2 28h8M46 28h8" stroke-width="1.75" stroke-linecap="round"/>
    </svg>`;

  function sidebar(rolId, activo) {
    const r = ROLES[rolId];
    const grupos = MENU[rolId].map(([titulo, ids]) => {
      const items = ids.map((id) => {
        const it = ITEMS[id];
        const act = id === activo || (activo === 'dispositivos' && id === 'misDispositivos');
        const cont = it.contador && r.alertas ? `<span class="nav__count">${r.alertas}</span>` : '';
        return `<a class="nav__item${act ? ' is-active' : ''}" href="${it.href}">${ico(it.icono)}<span>${it.texto}</span>${cont}</a>`;
      }).join('');
      return `<div class="nav__group">${titulo}</div>${items}`;
    }).join('');
    const sub = r.empresa ? r.empresa : r.rol;
    return `
      <aside class="sidebar">
        <div class="brand">${logo()}<div class="brand__name">${esc(PRODUCTO)}<span>Consola de monitoreo</span></div></div>
        <nav class="nav" aria-label="Menú principal">${grupos}</nav>
        <div class="sidebar__user">
          <div class="avatar avatar--dark">${r.iniciales}</div>
          <div class="who"><b>${esc(r.nombre)}</b><span class="role-tag">${ico(r.icono)}${esc(r.rol)}</span></div>
        </div>
      </aside>`;
  }

  function topbar(rolId, titulo, subtitulo) {
    const r = ROLES[rolId];
    return `
      <header class="topbar">
        <div class="topbar__title">
          <h1>${esc(titulo)}</h1>
          ${subtitulo ? `<p>${esc(subtitulo)}</p>` : ''}
        </div>
        <div class="topbar__actions" id="topbar-acciones">
          <button class="icon-btn" aria-label="Notificaciones: ${r.alertas} sin leer">${ico('bell')}${r.alertas ? `<span class="dot">${r.alertas}</span>` : ''}</button>
          <div class="avatar" title="${esc(r.nombre)}">${r.iniciales}</div>
        </div>
      </header>`;
  }

  /**
   * Envuelve el <main class="content"> de la página con la barra lateral y la superior.
   * Lee data-rol, data-nav, data-titulo y data-subtitulo del <body>.
   * Si existe <template id="acciones">, su contenido se inserta antes de la campana.
   */
  function shell(opc = {}) {
    const b = document.body;
    const rol = opc.rol || b.dataset.rol || 'hogar';
    const main = document.querySelector('main');
    const app = document.createElement('div');
    app.className = 'app';
    app.innerHTML = sidebar(rol, opc.nav || b.dataset.nav) +
      `<div class="app__main">${topbar(rol, opc.titulo || b.dataset.titulo || PRODUCTO, opc.subtitulo || b.dataset.subtitulo)}</div>`;
    b.prepend(app);
    app.querySelector('.app__main').appendChild(main);
    const tpl = document.getElementById('acciones');
    if (tpl) document.getElementById('topbar-acciones').prepend(tpl.content.cloneNode(true));
    document.title = `${opc.titulo || b.dataset.titulo || ''} · ${PRODUCTO}`.replace(/^ · /, '');
  }

  // ---------- Mapa ----------
  const CAPAS = {
    estandar: () => L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19, attribution: '&copy; OpenStreetMap',
    }),
    satelite: () => L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
      maxZoom: 19, attribution: 'Imágenes &copy; Esri',
    }),
  };

  function mapa(id, centro = BOGOTA, zoom = 14, capa = 'estandar', opc = {}) {
    const m = L.map(id, { zoomControl: false, attributionControl: true, ...opc }).setView(centro, zoom);
    if (opc.zoomControl !== false) L.control.zoom({ position: opc.zoomPos || 'bottomright' }).addTo(m);
    if (m.attributionControl) m.attributionControl.setPrefix(false);
    CAPAS[capa]().addTo(m);
    return m;
  }

  const ICONO_TIPO = { vehiculo: 'car', familiar: 'user-round', mascota: 'paw-print' };

  /** Marcador personalizado según el tipo de activo monitoreado. */
  function marcador(latlng, o = {}) {
    const tipo = o.tipo || 'vehiculo';
    const clases = ['mk', `mk--${tipo}`, o.seleccionado && 'is-selected', o.desconectado && 'mk--off', o.sos && 'mk--sos'].filter(Boolean).join(' ');
    const html = `<div class="${clases}">${o.sos ? '<span class="mk__ring"></span>' : ''}<span class="mk__pin"></span>${ico(o.icono || ICONO_TIPO[tipo])}${o.etiqueta ? `<span class="mk__label">${esc(o.etiqueta)}</span>` : ''}</div>`;
    return L.marker(latlng, { icon: L.divIcon({ className: '', html, iconSize: [36, 44], iconAnchor: [18, 43] }) });
  }

  /** Marcador de punto simple (inicio/fin de recorrido, centro de geocerca). */
  function punto(latlng, clase) {
    return L.marker(latlng, { icon: L.divIcon({ className: '', html: `<div class="${clase}"></div>`, iconSize: [14, 14], iconAnchor: [7, 7] }) });
  }

  /** Geocerca circular (HU-10): perímetro, centro y radio visibles. */
  function geocerca(m, centro, radio, o = {}) {
    const color = o.color || '#1F6FB2';
    const g = L.layerGroup().addTo(m);
    L.circle(centro, { radius: radio, color, weight: 2, fillColor: color, fillOpacity: o.opacidad ?? .12, dashArray: o.punteada === false ? null : '6 6', opacity: o.tenue ? .55 : 1 }).addTo(g);
    if (!o.soloPerimetro) {
      const borde = [centro[0], centro[1] + radio / (111320 * Math.cos(centro[0] * Math.PI / 180))];
      L.polyline([centro, borde], { color, weight: 1.5 }).addTo(g);
      punto(centro, 'mk-center').addTo(g);
      const medio = [centro[0], (centro[1] + borde[1]) / 2];
      L.marker(medio, { icon: L.divIcon({ className: '', html: `<span class="radius-label">${radio.toLocaleString('es-CO')} m</span>`, iconSize: [60, 18], iconAnchor: [30, 22] }) }).addTo(g);
    }
    if (o.nombre) {
      const norte = [centro[0] + radio / 111320, centro[1]];
      L.marker(norte, { icon: L.divIcon({ className: '', html: `<span class="geo-name">${esc(o.nombre)}</span>`, iconSize: [120, 22], iconAnchor: [60, 30] }) }).addTo(g);
    }
    return g;
  }

  /** Bloque de "Permisos insuficientes" (HU-02, HU-14, HU-17). */
  function denegado(o) {
    return `
      <div class="denied">
        <div class="denied__icon">${ico('shield-x')}</div>
        <span class="badge badge--danger">${ico('lock')}Permisos insuficientes</span>
        <h2>${esc(o.titulo)}</h2>
        <p>${o.detalle}</p>
        <dl class="denied__meta">
          <dt>Tu rol</dt><dd>${esc(o.rol)}</dd>
          <dt>Intentaste</dt><dd>${o.intento}</dd>
          <dt>Requiere</dt><dd>${o.requiere}</dd>
        </dl>
        <div class="row"><a class="btn btn--primary" href="${o.volverHref || '07-monitoreo.html'}">${ico('arrow-left')}${esc(o.volver || 'Volver al monitoreo')}</a></div>
      </div>`;
  }

  function iconos() { if (window.lucide) lucide.createIcons({ attrs: { 'stroke-width': 1.75 } }); }

  window.APP = { PRODUCTO, BOGOTA, ROLES, MENU, ITEMS, estado, params, esc, ico, logo, marcaVacia, sidebar, shell, mapa, marcador, punto, geocerca, denegado, iconos };
})();
