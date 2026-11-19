import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Calendar, Car, CircleCheck, Link2, Link2Off, PawPrint, Save, UserRound } from 'lucide-react';
import { api } from '../api/cliente';
import { Pagina } from '../componentes/Layout';
import { AvisoError, Campo, IconoActivo, Vacio } from '../componentes/Ui';
import { TIPOS_DISPOSITIVO } from '../utilidades/formato';

const TIPOS = {
  VEHICULO: [Car, 'Vehículo', 'Placa y modelo', 'vehiculo'],
  FAMILIAR: [UserRound, 'Familiar', 'Persona a cuidar', 'familiar'],
  MASCOTA: [PawPrint, 'Mascota', 'Perro, gato u otra', 'mascota'],
};
const VACIO = { placa: '', modelo: '', nombre: '', parentesco: '', fechaNacimiento: '', especie: '', raza: '' };
const NOMBRE_CAMPO = { placa: 'la placa', modelo: 'el modelo', nombre: 'el nombre', parentesco: 'el parentesco', especie: 'la especie', tipo: 'el tipo' };

/** Registro del activo o sujeto monitoreado (HU-05). */
export default function Activos() {
  const [params] = useSearchParams();
  const [tipo, setTipo] = useState('VEHICULO');
  const [datos, setDatos] = useState(VACIO);
  const [dispositivoId, setDispositivoId] = useState(params.get('dispositivo') || '');
  const [activos, setActivos] = useState([]);
  const [dispositivos, setDispositivos] = useState([]);
  const [error, setError] = useState(null);
  const [exito, setExito] = useState(null);
  const [guardando, setGuardando] = useState(false);

  const cargar = () => {
    api.get('/api/activos').then(setActivos).catch(setError);
    api.get('/api/dispositivos').then(setDispositivos).catch(() => {});
  };
  useEffect(cargar, []);

  const faltantes = error?.detalles || [];
  const campo = (clave, extra = '') => ({
    value: datos[clave],
    onChange: (e) => setDatos({ ...datos, [clave]: e.target.value }),
    className: `input ${extra}${faltantes.includes(clave) ? ' is-error' : ''}`,
  });
  const errorDe = (clave) => (faltantes.includes(clave) ? `Escribe ${NOMBRE_CAMPO[clave] || clave}.` : null);

  const guardar = async () => {
    setGuardando(true);
    setError(null);
    setExito(null);
    try {
      const activo = await api.post('/api/activos', { tipo, ...datos, fechaNacimiento: datos.fechaNacimiento || null, dispositivoId: dispositivoId || null });
      setExito(activo);
      setDatos(VACIO);
      setDispositivoId('');
      cargar();
    } catch (err) {
      setError(err);
    } finally {
      setGuardando(false);
    }
  };

  const libres = dispositivos.filter((d) => !d.activoId);

  return (
    <Pagina titulo="Activos monitoreados" subtitulo="Registra el vehículo, familiar o mascota que vas a monitorear">
      <div className="pg-05">
        {error && <div style={{ marginBottom: 'var(--sp-6)' }}><AvisoError error={error} /></div>}
        {exito && (
          <div className="banner banner--success" style={{ marginBottom: 'var(--sp-6)' }}>
            <CircleCheck />
            <div className="banner__body"><b>{TIPOS[exito.tipo][1]} registrado.</b> {exito.etiqueta} ya aparece en tus activos monitoreados{exito.dispositivoId ? ' y quedó asociado a su dispositivo.' : ' y está disponible para asociarlo a un dispositivo.'}</div>
            {!exito.dispositivoId && <div className="banner__actions"><Link className="btn btn--secondary btn--sm" to="/dispositivos"><Link2 />Asociar dispositivo</Link></div>}
          </div>
        )}
        <div className="registro">
          <section className="card">
            <div className="card__body">
              <div className="bloque">
                <h4>Tipo de activo</h4>
                <div className="tipo-selector">
                  {Object.entries(TIPOS).map(([k, [Icono, t, d, clase]]) => (
                    <button key={k} type="button" className={`tipo-op${k === tipo ? ' is-active' : ''}`} onClick={() => { setTipo(k); setError(null); }}>
                      <span className={`type-icon type-icon--${clase}`}><Icono /></span>
                      <div><b>{t}</b><span>{d}</span></div><span className="radio" />
                    </button>
                  ))}
                </div>
              </div>
              <div className="bloque">
                {tipo === 'VEHICULO' && (<>
                  <h4>Datos del vehículo</h4>
                  <div className="form-grid">
                    <Campo etiqueta="Placa" requerido error={errorDe('placa')} ayuda="Tres letras y tres números (o dos números y una letra)."><input {...campo('placa', 'mono')} placeholder="ABC-123" onChange={(e) => setDatos({ ...datos, placa: e.target.value.toUpperCase() })} /></Campo>
                    <Campo etiqueta="Modelo" requerido error={errorDe('modelo')} ayuda="Marca, referencia y año."><input {...campo('modelo')} placeholder="Marca, referencia y año" /></Campo>
                  </div>
                </>)}
                {tipo === 'FAMILIAR' && (<>
                  <h4>Datos del familiar</h4>
                  <div className="form-grid">
                    <div className="span-2"><Campo etiqueta="Nombre completo" requerido error={errorDe('nombre')}><input {...campo('nombre')} /></Campo></div>
                    <Campo etiqueta="Parentesco" requerido error={errorDe('parentesco')}>
                      <select {...campo('parentesco')} className={`select${faltantes.includes('parentesco') ? ' is-error' : ''}`}>
                        <option value="">Selecciona…</option>
                        {['Padre', 'Madre', 'Hijo', 'Hija', 'Abuelo', 'Abuela', 'Hermano', 'Hermana', 'Pareja', 'Otro'].map((p) => <option key={p}>{p}</option>)}
                      </select>
                    </Campo>
                    <Campo etiqueta="Fecha de nacimiento"><div className="input-wrap"><Calendar /><input {...campo('fechaNacimiento', 'mono')} type="date" /></div></Campo>
                  </div>
                </>)}
                {tipo === 'MASCOTA' && (<>
                  <h4>Datos de la mascota</h4>
                  <div className="form-grid">
                    <div className="span-2"><Campo etiqueta="Nombre" requerido error={errorDe('nombre')}><input {...campo('nombre')} /></Campo></div>
                    <Campo etiqueta="Especie" requerido error={errorDe('especie')}>
                      <select {...campo('especie')} className={`select${faltantes.includes('especie') ? ' is-error' : ''}`}>
                        <option value="">Selecciona…</option>
                        {['Perro', 'Gato', 'Otra'].map((p) => <option key={p}>{p}</option>)}
                      </select>
                    </Campo>
                    <Campo etiqueta="Raza"><input {...campo('raza')} /></Campo>
                  </div>
                </>)}
              </div>
              <div className="bloque">
                <h4>Dispositivo asociado</h4>
                <p>Opcional. Puedes asociarlo ahora o más tarde desde Mis dispositivos.</p>
                <div className="field" style={{ maxWidth: 420 }}>
                  <label>Dispositivo</label>
                  <select className="select" value={dispositivoId} onChange={(e) => setDispositivoId(e.target.value)}>
                    <option value="">Sin dispositivo por ahora</option>
                    {libres.map((d) => <option key={d.id} value={d.id}>{d.serial} · {TIPOS_DISPOSITIVO[d.tipo]} · sin asignar</option>)}
                  </select>
                </div>
              </div>
            </div>
            <div className="card__foot">
              <button className="btn btn--secondary" onClick={() => { setDatos(VACIO); setError(null); }}>Limpiar</button>
              <button className="btn btn--primary" onClick={guardar} disabled={guardando}><Save />Guardar activo</button>
            </div>
          </section>

          <aside className="card">
            <div className="card__head"><h3>Tus activos monitoreados</h3><span className="muted small">{activos.length} registrados</span></div>
            <div className="list">
              {activos.length === 0 && <Vacio titulo="Aún no hay activos">Registra el primero con el formulario.</Vacio>}
              {activos.map((a) => {
                const d = dispositivos.find((x) => x.id === a.dispositivoId);
                return (
                  <div key={a.id} className={`list-item${exito?.id === a.id ? ' is-selected' : ''}`}>
                    <IconoActivo tipo={a.tipo} />
                    <div className="meta"><b className={a.tipo === 'VEHICULO' ? 'mono' : ''}>{a.etiqueta}</b><span>{a.descripcion.split(' · ').slice(1).join(' · ')}</span></div>
                    {d ? <span className="mono xs muted">{d.serial}</span> : <span className="badge badge--warning"><Link2Off />Sin dispositivo</span>}
                  </div>
                );
              })}
            </div>
          </aside>
        </div>
      </div>
    </Pagina>
  );
}
