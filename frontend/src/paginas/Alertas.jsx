import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Car, CheckCheck, CircleCheck, Gauge, LogIn, LogOut, MapPin, OctagonAlert, RadioTower, Siren, TriangleAlert } from 'lucide-react';
import { api } from '../api/cliente';
import { useSesion } from '../contexto/Sesion';
import { Pagina, useAlertas } from '../componentes/Layout';
import { AvisoError, Cargando, Vacio } from '../componentes/Ui';
import { Mapa, Marcador } from '../componentes/Mapa';
import { coordenada, diaCorto, fechaHora, hace, hora, TIPOS_ALERTA, TIPOS_DISPOSITIVO } from '../utilidades/formato';

const ICONOS = { LogIn, LogOut, Siren, Gauge };
const FILTROS = [['', 'Todas'], ['ENTRADA_GEOCERCA', 'Entrada a geocerca'], ['SALIDA_GEOCERCA', 'Salida de geocerca'], ['SOS', 'SOS'], ['EXCESO_VELOCIDAD', 'Exceso de velocidad']];

export function BadgeTipo({ tipo }) {
  const t = TIPOS_ALERTA[tipo];
  const Icono = ICONOS[t.icono];
  return <span className={`badge ${t.clase}`}><Icono />{t.texto}</span>;
}

/** Centro de alertas: geocercas (HU-12), SOS (HU-13) y exceso de velocidad (HU-15). */
export default function Alertas() {
  const { usuario } = useSesion();
  const { refrescar } = useAlertas();
  const [params, setParams] = useSearchParams();
  const [alertas, setAlertas] = useState(null);
  const [error, setError] = useState(null);
  const filtro = params.get('tipo') || '';
  const idSel = params.get('id');

  const cargar = useCallback(() => {
    api.get(`/api/alertas${filtro ? `?tipo=${filtro}` : ''}`).then(setAlertas).catch(setError);
  }, [filtro]);

  useEffect(() => {
    cargar();
    const t = setInterval(cargar, 15000);
    return () => clearInterval(t);
  }, [cargar]);

  const marcar = async (a) => {
    await api.put(`/api/alertas/${a.id}/leida`).catch(setError);
    cargar();
    refrescar();
  };
  const marcarTodas = async () => {
    await api.put('/api/alertas/leidas').catch(setError);
    cargar();
    refrescar();
  };

  const seleccionada = alertas?.find((a) => a.id === idSel);
  const inconsistente = usuario.rol === 'SUPERADMIN' ? alertas?.find((a) => a.inconsistente && !a.leida) : null;
  const hoy = useMemo(() => {
    const conteo = {};
    alertas?.filter((a) => diaCorto(a.fechaHora) === 'Hoy').forEach((a) => { conteo[a.tipo] = (conteo[a.tipo] || 0) + 1; });
    return conteo;
  }, [alertas]);
  const subtitulo = usuario.rol === 'SUPERADMIN' ? 'Alertas de todas las empresas y usuarios'
    : usuario.empresaNombre ? `Alertas de la flota de ${usuario.empresaNombre}` : 'Alertas de geocerca, SOS y exceso de velocidad de tus activos';

  return (
    <Pagina titulo="Centro de alertas" subtitulo={subtitulo}
      acciones={<button className="btn btn--secondary" onClick={marcarTodas}><CheckCheck />Marcar todas como leídas</button>}>
      <div className="pg-09">
        <div className="stack-6">
          <AvisoError error={error} />
          {inconsistente && (
            <div className="banner banner--danger"><OctagonAlert />
              <div className="banner__body">
                <b>Alerta SOS inconsistente: dispositivo no asociado.</b>
                <p>{inconsistente.mensaje} a las {hora(inconsistente.fechaHora)}. El evento quedó marcado como inconsistente para revisión del equipo de soporte.</p>
              </div>
              <div className="banner__actions"><button className="btn btn--secondary btn--sm" onClick={() => setParams({ id: inconsistente.id })}>Revisar</button></div>
            </div>
          )}
          <div className="alertas">
            <div className="stack">
              <div className="filtros">
                {FILTROS.map(([valor, texto]) => {
                  const Icono = valor ? ICONOS[TIPOS_ALERTA[valor].icono] : null;
                  return (
                    <button key={valor} className={`chip${filtro === valor ? ' is-active' : ''}`} onClick={() => setParams(valor ? { tipo: valor } : {})}>
                      {Icono && <Icono />}{texto}
                    </button>
                  );
                })}
              </div>
              <div className="card">
                {!alertas && <Cargando />}
                {alertas?.length === 0 && <Vacio titulo="Sin alertas">No hay alertas {filtro ? 'de este tipo ' : ''}registradas.</Vacio>}
                {alertas?.map((a) => (
                  <div key={a.id} className={`al clicable${a.leida ? ' is-leida' : ''}${a.inconsistente || (a.tipo === 'SOS' && !a.leida) ? ' is-danger' : ''}${a.id === idSel ? ' is-selected' : ''}`}
                    onClick={() => setParams({ ...(filtro ? { tipo: filtro } : {}), id: a.id })}>
                    <span>{!a.leida && <span className="unread-dot" title="No leída" />}</span>
                    <div className="al__tipo"><BadgeTipo tipo={a.tipo} />{a.inconsistente && <span className="badge badge--danger"><OctagonAlert />Inconsistente</span>}</div>
                    <div className="al__main">
                      <b>{a.inconsistente ? 'Dispositivo no asociado. ' : ''}{a.mensaje}</b>
                      <div className="al__meta">
                        <span>{a.tipo === 'EXCESO_VELOCIDAD' ? <Car /> : <RadioTower />}<span><span className={a.activoTipo === 'VEHICULO' ? 'mono' : ''}>{a.activoEtiqueta || 'Sin activo'}</span>, {TIPOS_DISPOSITIVO[a.tipoDispositivo] || 'Dispositivo'} <span className="mono">{a.serialDispositivo}</span>{usuario.rol === 'SUPERADMIN' && a.usuarioNombre ? ` · ${a.usuarioNombre}` : ''}</span></span>
                        {a.latitud != null && <span><MapPin /><span className="mono">{coordenada(a.latitud)}, {coordenada(a.longitud)}</span></span>}
                      </div>
                    </div>
                    <div className="al__lado">
                      <span className="al__hora">{hora(a.fechaHora)}<small>{diaCorto(a.fechaHora)}</small></span>
                      {a.leida ? <span className="leida-txt"><CheckCheck />Leída</span>
                        : <button className="btn btn--ghost btn--sm" onClick={(e) => { e.stopPropagation(); marcar(a); }}><CheckCheck />Marcar como leída</button>}
                    </div>
                  </div>
                ))}
              </div>
            </div>
            <aside className="stack-6">
              {seleccionada ? <DetalleAlerta a={seleccionada} onLeida={() => marcar(seleccionada)} /> : (
                <>
                  <PanelLimites />
                  <section className="card"><div className="card__head"><h3>Hoy</h3><span className="muted small">{Object.values(hoy).reduce((s, n) => s + n, 0)} alertas</span></div>
                    <div className="card__body resumen-dia">
                      {Object.keys(TIPOS_ALERTA).map((t) => <div key={t}><BadgeTipo tipo={t} /><b>{hoy[t] || 0}</b></div>)}
                    </div>
                  </section>
                </>
              )}
            </aside>
          </div>
        </div>
      </div>
    </Pagina>
  );
}

function DetalleAlerta({ a, onLeida }) {
  return (
    <section className="card">
      <div className="card__head"><BadgeTipo tipo={a.tipo} /><h3>Detalle de la alerta</h3></div>
      <div className="card__body stack">
        {a.latitud != null && (
          <div className="mini-mapa">
            <Mapa key={a.id} centro={[a.latitud, a.longitud]} zoom={16}>
              <Marcador posicion={[a.latitud, a.longitud]} tipo={a.activoTipo || 'FAMILIAR'} sos={a.tipo === 'SOS'} etiqueta={a.activoEtiqueta} />
            </Mapa>
          </div>
        )}
        <dl className="dl">
          <dt>Activo monitoreado</dt><dd>{a.activoEtiqueta || 'Sin activo asociado'}</dd>
          <dt>Dispositivo</dt><dd>{TIPOS_DISPOSITIVO[a.tipoDispositivo] || ''} <span className="mono">{a.serialDispositivo}</span></dd>
          <dt>Momento</dt><dd className="mono">{fechaHora(a.fechaHora)}</dd>
          <dt>Hace</dt><dd>{hace(a.fechaHora)}</dd>
          <dt>Ubicación</dt><dd className="mono">{a.latitud != null ? `${coordenada(a.latitud)}, ${coordenada(a.longitud)}` : 'Sin coordenadas'}</dd>
          <dt>Estado</dt><dd>{a.inconsistente ? <span className="badge badge--danger"><OctagonAlert />Inconsistente, notificado a soporte</span> : a.leida ? <span className="badge badge--success"><CircleCheck />Atendida</span> : <span className="badge badge--warning"><TriangleAlert />Pendiente</span>}</dd>
        </dl>
        <div className="row">
          {a.dispositivoId && !a.inconsistente && <Link className="btn btn--secondary btn--sm" to={`/monitoreo?dispositivo=${a.dispositivoId}`}><MapPin />Ver en el mapa</Link>}
          {!a.leida && <button className="btn btn--ghost btn--sm" onClick={onLeida}><CheckCheck />Marcar como leída</button>}
        </div>
      </div>
    </section>
  );
}

/** Límites de velocidad por dispositivo (HU-15), con aviso de configuración pendiente. */
function PanelLimites() {
  const [dispositivos, setDispositivos] = useState([]);
  const [valores, setValores] = useState({});
  const [error, setError] = useState(null);
  const [guardado, setGuardado] = useState(false);

  const cargar = () => api.get('/api/dispositivos').then((l) => {
    const gps = l.filter((d) => d.tipo === 'GPS');
    setDispositivos(gps);
    setValores(Object.fromEntries(gps.map((d) => [d.id, d.limiteVelocidad ?? ''])));
  }).catch(setError);
  useEffect(() => { cargar(); }, []);

  const guardar = async () => {
    setError(null);
    setGuardado(false);
    try {
      for (const d of dispositivos) {
        const v = valores[d.id] === '' ? null : Number(valores[d.id]);
        if (v !== (d.limiteVelocidad ?? null)) await api.put(`/api/dispositivos/${d.id}/limite-velocidad`, { limiteVelocidad: v });
      }
      setGuardado(true);
      cargar();
    } catch (err) { setError(err); }
  };

  if (!dispositivos.length) return null;
  return (
    <section className="card">
      <div className="card__head"><Gauge /><h3>Límite de velocidad</h3></div>
      <div className="card__body limites-scroll" style={{ paddingTop: 'var(--sp-2)', paddingBottom: 'var(--sp-2)' }}>
        <p className="small muted" style={{ padding: 'var(--sp-2) 0' }}>Se genera una alerta cuando la velocidad reportada supera el límite configurado.</p>
        <AvisoError error={error} />
        {dispositivos.map((d) => d.limiteVelocidad ? (
          <div key={d.id} className="limite">
            <span className="type-icon type-icon--vehiculo"><Car /></span>
            <div className="meta"><b className="mono">{d.activoEtiqueta || d.serial}</b><span>GPS {d.serial}</span></div>
            <div className="input-wrap"><input className="input mono" value={valores[d.id]} onChange={(e) => setValores({ ...valores, [d.id]: e.target.value })} /><span className="suffix">km/h</span></div>
          </div>
        ) : (
          <div key={d.id} className="limite is-pendiente">
            <span className="type-icon" style={{ background: '#fff', color: 'var(--color-warning)' }}><TriangleAlert /></span>
            <div className="meta"><b className="mono">{d.activoEtiqueta || d.serial}</b><span style={{ color: 'var(--color-warning-text)', fontWeight: 600 }}>Límite no configurado</span></div>
            <div className="input-wrap"><input className="input mono" placeholder="—" value={valores[d.id]} onChange={(e) => setValores({ ...valores, [d.id]: e.target.value })} /><span className="suffix">km/h</span></div>
            <p className="xs" style={{ flexBasis: '100%', color: 'var(--color-warning-text)', marginTop: -4 }}>Configuración pendiente: mientras no tenga límite, {d.activoEtiqueta || d.serial} no genera alertas de exceso de velocidad.</p>
          </div>
        ))}
      </div>
      <div className="card__foot">
        {guardado && <span className="small" style={{ color: 'var(--color-success)', marginRight: 'auto' }}>Límites guardados</span>}
        <button className="btn btn--primary btn--sm" onClick={guardar}>Guardar límites</button>
      </div>
    </section>
  );
}
