import { useCallback, useEffect, useState } from 'react';
import { useMapEvents } from 'react-leaflet';
import { ArrowLeft, BellOff, Check, ChevronDown, CircleCheck, CircleDashed, CirclePause, CirclePlay, Clock, Crosshair, Info, MousePointerClick, Plus, Timer, Trash2 } from 'lucide-react';
import { api } from '../api/cliente';
import { Pagina } from '../componentes/Layout';
import { AvisoError, Campo, Cargando, IconoActivo, Vacio } from '../componentes/Ui';
import { BOGOTA, CirculoGeocerca, Encuadre, Mapa, Marcador, VolarA } from '../componentes/Mapa';
import { hora, TIPOS_DISPOSITIVO } from '../utilidades/formato';

const VACIA = { dispositivoId: '', nombre: '', latitudCentro: '', longitudCentro: '', radioMetros: '300' };
const SILENCIOS = [[30, '30 min'], [60, '1 h'], [240, '4 h'], [1440, '24 h']];

/** Zonas de seguridad (HU-10) y su activación, desactivación y silencio temporal (HU-11). */
export default function Geocercas() {
  const [geocercas, setGeocercas] = useState(null);
  const [dispositivos, setDispositivos] = useState([]);
  const [ubicaciones, setUbicaciones] = useState([]);
  const [seleccion, setSeleccion] = useState(null);
  const [creando, setCreando] = useState(false);
  const [form, setForm] = useState(VACIA);
  const [error, setError] = useState(null);
  const [menu, setMenu] = useState(null);

  const cargar = useCallback(() => {
    api.get('/api/geocercas').then((l) => { setGeocercas(l); setSeleccion((s) => s || l[0]?.id || null); }).catch(setError);
  }, []);

  useEffect(() => {
    cargar();
    api.get('/api/dispositivos').then(setDispositivos).catch(() => {});
    api.get('/api/monitoreo/ubicaciones').then(setUbicaciones).catch(() => {});
    const t = setInterval(cargar, 30000);
    return () => clearInterval(t);
  }, [cargar]);

  const accion = async (g, ruta) => {
    setError(null);
    setMenu(null);
    try {
      await api.put(`/api/geocercas/${g.id}/${ruta}`);
      cargar();
    } catch (err) { setError(err); }
  };

  const eliminar = async (g) => {
    if (!window.confirm(`¿Eliminar la geocerca ${g.nombre}?`)) return;
    try { await api.del(`/api/geocercas/${g.id}`); setSeleccion(null); cargar(); } catch (err) { setError(err); }
  };

  const crear = async (e) => {
    e.preventDefault();
    setError(null);
    const numeroO = (v) => (v === '' || v === null ? null : Number(String(v).replace('−', '-').replace(',', '.')));
    try {
      const g = await api.post('/api/geocercas', {
        dispositivoId: form.dispositivoId || null,
        nombre: form.nombre,
        latitudCentro: numeroO(form.latitudCentro),
        longitudCentro: numeroO(form.longitudCentro),
        radioMetros: numeroO(form.radioMetros),
      });
      setCreando(false);
      setForm(VACIA);
      setSeleccion(g.id);
      cargar();
    } catch (err) { setError(err); }
  };

  const sel = geocercas?.find((g) => g.id === seleccion);
  const centroForm = form.latitudCentro !== '' && form.longitudCentro !== '' ? [Number(form.latitudCentro), Number(form.longitudCentro)] : null;
  const previaValida = centroForm && Math.abs(centroForm[0]) <= 90 && Math.abs(centroForm[1]) <= 180 && Number(form.radioMetros) > 0;
  const ubicacionSel = ubicaciones.find((u) => u.dispositivoId === (creando ? form.dispositivoId : sel?.dispositivoId));
  const errorCampo = (texto) => error?.detalles?.find((d) => d.toLowerCase().includes(texto));

  return (
    <Pagina titulo="Geocercas" subtitulo="Zonas de seguridad circulares asignadas a cada dispositivo" mapa
      acciones={!creando && <button className="btn btn--primary" onClick={() => { setCreando(true); setError(null); }}><Plus />Nueva geocerca</button>}>
      <div className="pg-08" style={{ height: '100%' }}>
        <div className="geo">
          <aside className="geo__lado">
            {creando ? (
              <>
                <div className="geo__lado-head">
                  <button className="icon-btn" onClick={() => { setCreando(false); setError(null); }} aria-label="Volver" style={{ width: 32, height: 32 }}><ArrowLeft /></button>
                  <h3>Nueva geocerca</h3>
                </div>
                <div className="geo__scroll">
                  <form className="form-geo" onSubmit={crear}>
                    <AvisoError error={error} />
                    <Campo etiqueta="Dispositivo" requerido>
                      <select className="select" value={form.dispositivoId} onChange={(e) => setForm({ ...form, dispositivoId: e.target.value })}>
                        <option value="">Selecciona…</option>
                        {dispositivos.map((d) => <option key={d.id} value={d.id}>{TIPOS_DISPOSITIVO[d.tipo]} {d.serial}{d.activoEtiqueta ? ` · ${d.activoEtiqueta}` : ''}</option>)}
                      </select>
                    </Campo>
                    <Campo etiqueta="Nombre" requerido error={errorCampo('nombre')}>
                      <input className="input" value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })} placeholder="Casa, colegio, oficina…" />
                    </Campo>
                    <div className="ayuda"><MousePointerClick />Haz clic en el mapa para fijar el centro o escribe sus coordenadas.</div>
                    <div className="form-grid">
                      <Campo etiqueta="Latitud del centro" requerido>
                        <input className={`input mono${errorCampo('centro') ? ' is-error' : ''}`} value={form.latitudCentro} onChange={(e) => setForm({ ...form, latitudCentro: e.target.value })} placeholder="4.6487" />
                      </Campo>
                      <Campo etiqueta="Longitud del centro" requerido>
                        <input className={`input mono${errorCampo('centro') ? ' is-error' : ''}`} value={form.longitudCentro} onChange={(e) => setForm({ ...form, longitudCentro: e.target.value })} placeholder="-74.0642" />
                      </Campo>
                    </div>
                    <Campo etiqueta="Radio" requerido error={errorCampo('radio')} ayuda="Distancia en metros desde el centro hasta el borde (20 m a 50 km).">
                      <div className="input-wrap" style={{ maxWidth: 200 }}><CircleDashed />
                        <input className={`input mono${errorCampo('radio') ? ' is-error' : ''}`} value={form.radioMetros} onChange={(e) => setForm({ ...form, radioMetros: e.target.value })} />
                        <span className="suffix">m</span>
                      </div>
                    </Campo>
                    <div className="row" style={{ marginTop: 'var(--sp-2)' }}>
                      <button className="btn btn--primary"><Check />Crear geocerca</button>
                      <button type="button" className="btn btn--ghost" onClick={() => { setCreando(false); setError(null); }}>Cancelar</button>
                    </div>
                  </form>
                </div>
              </>
            ) : (
              <>
                <div className="geo__lado-head"><h3>Tus geocercas</h3><span className="muted small">{geocercas?.length ?? 0} zonas</span></div>
                {error && <div style={{ padding: 'var(--sp-3) var(--sp-5) 0' }}><AvisoError error={error} /></div>}
                <div className="geo__scroll">
                  {!geocercas && <Cargando />}
                  {geocercas?.length === 0 && <Vacio titulo="Aún no hay geocercas">Crea una zona circular para recibir alertas de entrada y salida.</Vacio>}
                  {geocercas?.map((g) => (
                    <div key={g.id} className={`gc clicable${g.id === seleccion ? ' is-selected' : ''}${g.estado === 'INACTIVA' ? ' is-inactiva' : ''}`} onClick={() => setSeleccion(g.id)}>
                      <div className="gc__top">
                        <IconoActivo tipo={g.activoTipo} />
                        <div className="meta"><b>{g.nombre}</b><span>{g.activoEtiqueta || 'Sin activo asociado'}</span><span className="mono">{g.serialDispositivo}</span></div>
                        <EstadoGeocerca g={g} />
                      </div>
                      <div className="gc__datos">
                        <span><Crosshair />{g.latitudCentro.toFixed(4)}, {g.longitudCentro.toFixed(4).replace('-', '−')}</span>
                        <span><CircleDashed />{Math.round(g.radioMetros).toLocaleString('es-CO')} m</span>
                      </div>
                      <div className="gc__acciones" onClick={(e) => e.stopPropagation()}>
                        {g.estado === 'ACTIVA' && (
                          <>
                            <button className="btn btn--secondary btn--sm" onClick={() => setMenu(menu === g.id ? null : g.id)}><BellOff />Silenciar<ChevronDown /></button>
                            <button className="btn btn--ghost btn--sm" onClick={() => accion(g, 'desactivar')}><CirclePause />Desactivar</button>
                          </>
                        )}
                        {g.estado === 'SILENCIADA' && (<>
                          <button className="btn btn--secondary btn--sm" onClick={() => accion(g, 'activar')}><CirclePlay />Reanudar ahora</button>
                          <button className="btn btn--ghost btn--sm" onClick={() => accion(g, 'desactivar')}><CirclePause />Desactivar</button>
                        </>)}
                        {g.estado === 'INACTIVA' && <button className="btn btn--secondary btn--sm" onClick={() => accion(g, 'activar')}><CirclePlay />Activar</button>}
                        <button className="btn btn--ghost btn--sm" style={{ marginLeft: 'auto' }} onClick={() => eliminar(g)} aria-label="Eliminar"><Trash2 /></button>
                        {menu === g.id && (
                          <div className="menu">
                            <div className="menu__titulo">Silenciar alertas de {g.nombre} durante</div>
                            {SILENCIOS.map(([min, txt]) => (
                              <button key={min} className="menu__op" onClick={() => accion(g, `silenciar?minutos=${min}`)}>
                                <Timer />{txt}<small>hasta {hora(new Date(Date.now() + min * 60000).toISOString())}</small>
                              </button>
                            ))}
                          </div>
                        )}
                      </div>
                      {g.estado === 'SILENCIADA' && <div className="gc__nota"><Clock />Sin alertas hasta las {hora(g.silenciadaHasta)}. Luego la vigilancia se reanuda sola.</div>}
                      {g.estado === 'INACTIVA' && <div className="gc__nota"><Info />No genera alertas de entrada ni de salida mientras esté inactiva.</div>}
                      {g.estado === 'ACTIVA' && <div className="gc__nota"><CircleCheck />Evaluando entradas y salidas de la zona.</div>}
                    </div>
                  ))}
                </div>
              </>
            )}
          </aside>
          <div className="geo__mapa">
            <Mapa centro={BOGOTA} zoom={14} zoomPos="topright">
              {creando && <SelectorCentro onClick={(lat, lng) => setForm((f) => ({ ...f, latitudCentro: lat.toFixed(5), longitudCentro: lng.toFixed(5) }))} />}
              {!creando && geocercas?.map((g) => (
                <CirculoGeocerca key={g.id} centro={[g.latitudCentro, g.longitudCentro]} radio={g.radioMetros} estado={g.estado}
                  nombre={g.id === seleccion ? g.nombre : null} detalle={g.id === seleccion} />
              ))}
              {!creando && sel && <VolarA posicion={[sel.latitudCentro, sel.longitudCentro]} zoom={sel.radioMetros > 800 ? 13 : 16} />}
              {!creando && !sel && geocercas?.length > 0 && <Encuadre puntos={geocercas.map((g) => [g.latitudCentro, g.longitudCentro])} dependencia={geocercas.length} />}
              {creando && previaValida && <CirculoGeocerca centro={centroForm} radio={Number(form.radioMetros)} nombre={form.nombre || 'Nueva geocerca'} />}
              {creando && previaValida && <VolarA posicion={centroForm} />}
              {ubicacionSel?.ubicacionDisponible && (
                <Marcador posicion={[ubicacionSel.latitud, ubicacionSel.longitud]} tipo={ubicacionSel.activoTipo} etiqueta={ubicacionSel.activoEtiqueta.split(' ').slice(0, 2).join(' ')} desconectado={!ubicacionSel.conectado} />
              )}
            </Mapa>
            <div className="map-panel leyenda">
              <div><i />Perímetro de la geocerca</div>
              <div><i className="c" />Centro</div>
            </div>
          </div>
        </div>
      </div>
    </Pagina>
  );
}

function EstadoGeocerca({ g }) {
  if (g.estado === 'ACTIVA') return <span className="badge badge--info"><CircleCheck />Activa</span>;
  if (g.estado === 'SILENCIADA') return <span className="badge badge--warning"><BellOff />Silenciada hasta {hora(g.silenciadaHasta)}</span>;
  return <span className="badge"><CirclePause />Inactiva</span>;
}

function SelectorCentro({ onClick }) {
  useMapEvents({ click: (e) => onClick(e.latlng.lat, e.latlng.lng) });
  return null;
}
