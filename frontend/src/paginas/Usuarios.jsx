import { useEffect, useMemo, useState } from 'react';
import { Building2, Check, House, Minus, Search, ShieldCheck, UserPlus } from 'lucide-react';
import { api } from '../api/cliente';
import { useSesion } from '../contexto/Sesion';
import { Pagina } from '../componentes/Layout';
import { AvisoError, Campo, Cargando, Denegado, Modal } from '../componentes/Ui';
import { fechaHora, iniciales, ROLES } from '../utilidades/formato';

const ICONO_ROL = { SUPERADMIN: ShieldCheck, ADMIN_FLOTA_B2B: Building2, USUARIO_HOGAR_B2C: House };
const CLASE_ROL = { SUPERADMIN: 'badge--primary', ADMIN_FLOTA_B2B: 'badge--info', USUARIO_HOGAR_B2C: '' };
const SECCIONES = [
  ['Monitoreo, alertas, geocercas y recorrido', 'todo', 'su flota', 'lo suyo'],
  ['Dispositivos y activos monitoreados', 'todo', 'su flota', 'lo suyo'],
  ['Control remoto del motor', 'todo', 'su flota', 'lo suyo'],
  ['Informes PDF', 'todo', 'su flota', 'lo suyo'],
  ['Sub-flotas', 'todo', 'su flota', ''],
  ['Usuarios y roles', 'todo', '', ''],
];

function Celda({ v }) {
  if (!v) return <span className="x"><Minus />Sin acceso</span>;
  return <span className="ok"><Check />{v === 'todo' ? 'Todo' : v === 'su flota' ? 'Su flota' : 'Lo suyo'}</span>;
}

/** Gestión de permisos por rol (HU-02): solo el SuperAdmin administra usuarios. */
export default function Usuarios() {
  const { usuario } = useSesion();
  const [usuarios, setUsuarios] = useState(null);
  const [empresas, setEmpresas] = useState([]);
  const [filtro, setFiltro] = useState('');
  const [rol, setRol] = useState('');
  const [error, setError] = useState(null);
  const [creando, setCreando] = useState(false);
  const esSuper = usuario.rol === 'SUPERADMIN';

  const cargar = () => api.get('/api/usuarios').then(setUsuarios).catch(setError);
  useEffect(() => {
    if (!esSuper) return;
    cargar();
    api.get('/api/usuarios/empresas').then(setEmpresas).catch(() => {});
  }, [esSuper]);

  const visibles = useMemo(() => (usuarios || []).filter((u) =>
    (!rol || u.rol === rol)
    && (!filtro || `${u.nombre} ${u.correo}`.toLowerCase().includes(filtro.toLowerCase()))), [usuarios, rol, filtro]);

  if (!esSuper) {
    return (
      <Pagina titulo="Usuarios y roles" subtitulo="Cada usuario ve solo las secciones y los datos de su rol">
        <Denegado titulo="No puedes abrir Usuarios y roles"
          detalle={usuario.rol === 'USUARIO_HOGAR_B2C'
            ? 'Esta sección es solo para el SuperAdmin. Con tu cuenta puedes gestionar tu perfil y tus propios dispositivos.'
            : 'Esta sección es solo para el SuperAdmin. Con tu cuenta puedes gestionar la flota de tu empresa.'}
          rol={ROLES[usuario.rol].nombre} intento="Abrir Usuarios y roles desde un enlace directo" requiere="Rol SuperAdmin" />
      </Pagina>
    );
  }

  const alcance = (u) => (u.rol === 'SUPERADMIN' ? 'Todo el sistema' : u.rol === 'ADMIN_FLOTA_B2B' ? (u.empresaNombre || 'Su empresa') : 'Su perfil y dispositivos');

  return (
    <Pagina titulo="Usuarios y roles" subtitulo="Cada usuario ve solo las secciones y los datos de su rol"
      acciones={<button className="btn btn--primary" onClick={() => setCreando(true)}><UserPlus />Nuevo usuario</button>}>
      <div className="pg-02">
        <div className="layout-usuarios">
          <div className="stack">
            <div className="row" style={{ flexWrap: 'wrap' }}>
              <div className="input-wrap" style={{ width: 320 }}><Search /><input className="input" placeholder="Buscar por nombre o correo" value={filtro} onChange={(e) => setFiltro(e.target.value)} /></div>
              <button className={`chip${rol === '' ? ' is-active' : ''}`} onClick={() => setRol('')}>Todos los roles</button>
              {Object.entries(ROLES).map(([clave, r]) => <button key={clave} className={`chip${rol === clave ? ' is-active' : ''}`} onClick={() => setRol(clave)}>{r.nombre}</button>)}
            </div>
            <AvisoError error={error} />
            {!usuarios ? <Cargando /> : (
              <div className="table-wrap">
                <table className="table">
                  <thead><tr><th>Usuario</th><th>Rol</th><th>Alcance</th><th>Teléfono</th><th>Registro</th></tr></thead>
                  <tbody>{visibles.map((u) => {
                    const Icono = ICONO_ROL[u.rol];
                    return (
                      <tr key={u.id}>
                        <td><div className="cell-main"><div className="avatar">{u.fotoPerfil ? <img src={u.fotoPerfil} alt="" /> : iniciales(u.nombre)}</div><div><b>{u.nombre}</b><span>{u.correo}</span></div></div></td>
                        <td><span className={`badge ${CLASE_ROL[u.rol]}`}><Icono />{ROLES[u.rol].nombre}</span></td>
                        <td style={{ whiteSpace: 'nowrap' }}>{alcance(u)}</td>
                        <td className="mono small">{u.telefono}</td>
                        <td className="muted small" style={{ whiteSpace: 'nowrap' }}>{fechaHora(u.fechaRegistro)}</td>
                      </tr>
                    );
                  })}
                  {visibles.length === 0 && <tr><td colSpan={5} className="muted small">Ningún usuario coincide con la búsqueda.</td></tr>}
                  </tbody>
                </table>
              </div>
            )}
          </div>
          <div className="card">
            <div className="card__head"><h3>Permisos por rol</h3><span className="muted small">Cada rol solo ve los datos de su alcance</span></div>
            <table className="table matriz">
              <thead><tr><th>Sección</th><th>SuperAdmin</th><th>Admin de Flota B2B</th><th>Usuario Hogar B2C</th></tr></thead>
              <tbody>{SECCIONES.map(([s, a, b, c]) => <tr key={s}><td>{s}</td><td><Celda v={a} /></td><td><Celda v={b} /></td><td><Celda v={c} /></td></tr>)}</tbody>
            </table>
          </div>
        </div>
        {creando && <NuevoUsuario empresas={empresas} onCerrar={() => setCreando(false)} onCreado={() => { setCreando(false); cargar(); }} />}
      </div>
    </Pagina>
  );
}

function NuevoUsuario({ empresas, onCerrar, onCreado }) {
  const [f, setF] = useState({ nombre: '', correo: '', telefono: '', contrasena: '', rol: 'ADMIN_FLOTA_B2B', empresaId: empresas[0]?.id || '' });
  const [error, setError] = useState(null);
  const cambiar = (c) => (e) => setF({ ...f, [c]: e.target.value });
  const crear = async () => {
    setError(null);
    try {
      await api.post('/api/usuarios', { ...f, empresaId: f.rol === 'ADMIN_FLOTA_B2B' ? f.empresaId : null });
      onCreado();
    } catch (err) { setError(err); }
  };
  return (
    <Modal titulo="Nuevo usuario" subtitulo="Crea cuentas de administradores de flota u otros usuarios" icono={UserPlus} ancho onCerrar={onCerrar}
      pie={<><button className="btn btn--secondary" onClick={onCerrar}>Cancelar</button><button className="btn btn--primary" onClick={crear}><Check />Crear usuario</button></>}>
      <AvisoError error={error} />
      <div className="form-grid">
        <Campo etiqueta="Nombre completo" requerido><input className="input" value={f.nombre} onChange={cambiar('nombre')} /></Campo>
        <Campo etiqueta="Correo electrónico" requerido><input className="input" type="email" value={f.correo} onChange={cambiar('correo')} /></Campo>
        <Campo etiqueta="Teléfono celular" requerido><input className="input mono" value={f.telefono} onChange={cambiar('telefono')} /></Campo>
        <Campo etiqueta="Contraseña temporal" requerido ayuda="Mínimo 8 caracteres, una mayúscula y un número."><input className="input" type="password" value={f.contrasena} onChange={cambiar('contrasena')} autoComplete="new-password" /></Campo>
        <Campo etiqueta="Rol" requerido>
          <select className="select" value={f.rol} onChange={cambiar('rol')}>
            {Object.entries(ROLES).map(([clave, r]) => <option key={clave} value={clave}>{r.nombre}</option>)}
          </select>
        </Campo>
        {f.rol === 'ADMIN_FLOTA_B2B' && (
          <Campo etiqueta="Empresa" requerido>
            <select className="select" value={f.empresaId} onChange={cambiar('empresaId')}>
              {empresas.map((e) => <option key={e.id} value={e.id}>{e.nombre}</option>)}
            </select>
          </Campo>
        )}
      </div>
    </Modal>
  );
}
