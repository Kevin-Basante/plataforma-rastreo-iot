import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import {
  Bell, BellRing, Building2, CircleDot, FileText, House, Layers, LogOut, Map, RadioTower, Route, Shapes, ShieldCheck, Siren, UserRound, Users,
} from 'lucide-react';
import { useSesion } from '../contexto/Sesion';
import { api } from '../api/cliente';
import { Logo } from './Ui';
import { hace, iniciales, ROLES } from '../utilidades/formato';

// Menú por rol (HU-02): cada rol ve solo las secciones que puede usar.
const ITEMS = {
  monitoreo: { icono: Map, texto: 'Monitoreo', ruta: '/monitoreo' },
  alertas: { icono: BellRing, texto: 'Centro de alertas', ruta: '/alertas', contador: true },
  geocercas: { icono: CircleDot, texto: 'Geocercas', ruta: '/geocercas' },
  recorrido: { icono: Route, texto: 'Recorrido', ruta: '/recorrido' },
  dispositivos: { icono: RadioTower, texto: 'Dispositivos', ruta: '/dispositivos' },
  misDispositivos: { icono: RadioTower, texto: 'Mis dispositivos', ruta: '/dispositivos' },
  activos: { icono: Shapes, texto: 'Activos monitoreados', ruta: '/activos' },
  subflotas: { icono: Layers, texto: 'Sub-flotas', ruta: '/subflotas' },
  informes: { icono: FileText, texto: 'Informes PDF', ruta: '/informes' },
  usuarios: { icono: Users, texto: 'Usuarios y roles', ruta: '/usuarios' },
  perfil: { icono: UserRound, texto: 'Mi perfil', ruta: '/perfil' },
};

export const MENU = {
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

const ICONO_ROL = { superadmin: ShieldCheck, admin: Building2, hogar: House };

const ContextoAlertas = createContext({ noLeidas: 0, refrescar: () => {} });
export const useAlertas = () => useContext(ContextoAlertas);

export function Avatar({ usuario, oscuro }) {
  return (
    <div className={`avatar${oscuro ? ' avatar--dark' : ''}`} title={usuario.nombre}>
      {usuario.fotoPerfil ? <img src={usuario.fotoPerfil} alt="" /> : iniciales(usuario.nombre)}
    </div>
  );
}

export default function Layout() {
  const { usuario, cerrarSesion } = useSesion();
  const navegar = useNavigate();
  const [noLeidas, setNoLeidas] = useState(0);
  const [sos, setSos] = useState(null);
  const rol = ROLES[usuario.rol];
  const IconoRol = ICONO_ROL[rol.clave];

  const refrescar = useCallback(async () => {
    try {
      const [{ cantidad }, pendientesSos] = await Promise.all([
        api.get('/api/alertas/no-leidas'),
        api.get('/api/alertas?tipo=SOS&soloNoLeidas=true'),
      ]);
      setNoLeidas(cantidad);
      setSos(pendientesSos.find((a) => !a.inconsistente) || pendientesSos[0] || null);
    } catch { /* se reintenta en el siguiente ciclo */ }
  }, []);

  useEffect(() => {
    refrescar();
    const t = setInterval(refrescar, 15000);
    return () => clearInterval(t);
  }, [refrescar]);

  const atenderSos = async () => {
    if (!sos) return;
    await api.put(`/api/alertas/${sos.id}/leida`).catch(() => {});
    refrescar();
  };

  return (
    <ContextoAlertas.Provider value={{ noLeidas, refrescar }}>
      <div className="app">
        <aside className="sidebar">
          <div className="brand"><Logo /><div className="brand__name">Plataforma de Rastreo IoT<span>Consola de monitoreo</span></div></div>
          <nav className="nav" aria-label="Menú principal">
            {MENU[rol.clave].map(([titulo, ids]) => (
              <div key={titulo} className="nav__bloque">
                <div className="nav__group">{titulo}</div>
                {ids.map((id) => {
                  const it = ITEMS[id];
                  const Icono = it.icono;
                  return (
                    <NavLink key={id} to={it.ruta} className={({ isActive }) => `nav__item${isActive ? ' is-active' : ''}`}>
                      <Icono /><span>{it.texto}</span>
                      {it.contador && noLeidas > 0 && <span className="nav__count">{noLeidas}</span>}
                    </NavLink>
                  );
                })}
              </div>
            ))}
          </nav>
          <div className="sidebar__user">
            <Avatar usuario={usuario} oscuro />
            <div className="who">
              <b>{usuario.nombre}</b>
              <span className="role-tag"><IconoRol />{usuario.empresaNombre || rol.nombre}</span>
            </div>
            <button className="salir" onClick={() => { cerrarSesion(); navegar('/acceso'); }} title="Cerrar sesión" aria-label="Cerrar sesión">
              <LogOut />
            </button>
          </div>
        </aside>
        <div className="app__main">
          {sos && (
            <div className="banner banner--sos banner--global" role="alert">
              <span className="sos-ring"><Siren /></span>
              <div className="banner__body">
                <b>{sos.inconsistente ? 'SOS inconsistente para soporte' : `SOS: ${sos.activoEtiqueta || sos.serialDispositivo}`}</b>
                <p>{sos.mensaje} · {hace(sos.fechaHora)}</p>
              </div>
              <div className="banner__actions">
                <Link className="btn btn--secondary btn--sm" to={`/alertas?id=${sos.id}`}>Ver detalle</Link>
                <button className="btn btn--ghost btn--sm" onClick={atenderSos}>Marcar como atendida</button>
              </div>
            </div>
          )}
          <Outlet />
        </div>
      </div>
    </ContextoAlertas.Provider>
  );
}

/** Barra superior + contenido de cada pantalla. */
export function Pagina({ titulo, subtitulo, acciones, mapa, children }) {
  const { usuario } = useSesion();
  const { noLeidas } = useAlertas();
  useEffect(() => { document.title = `${titulo} · Plataforma de Rastreo IoT`; }, [titulo]);
  return (
    <>
      <header className="topbar">
        <div className="topbar__title">
          <h1>{titulo}</h1>
          {subtitulo && <p>{subtitulo}</p>}
        </div>
        <div className="topbar__actions">
          {acciones}
          <Link className="icon-btn" to="/alertas" aria-label={`Notificaciones: ${noLeidas} sin leer`}>
            <Bell />{noLeidas > 0 && <span className="dot">{noLeidas > 99 ? '99+' : noLeidas}</span>}
          </Link>
          <Link to="/perfil"><Avatar usuario={usuario} /></Link>
        </div>
      </header>
      <main className={`content${mapa ? ' content--map' : ''}`}>{children}</main>
    </>
  );
}
