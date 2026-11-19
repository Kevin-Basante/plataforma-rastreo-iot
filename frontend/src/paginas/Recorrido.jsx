import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import L from 'leaflet';
import { Marker, Polyline } from 'react-leaflet';
import { CalendarRange, Route, Search, Timer } from 'lucide-react';
import { api } from '../api/cliente';
import { Pagina } from '../componentes/Layout';
import { AvisoError, MarcaVacia } from '../componentes/Ui';
import { BOGOTA, Encuadre, Mapa, Punto } from '../componentes/Mapa';
import { aInputLocal, deInputLocal, fechaHora, hora, numero, TIPOS_ALERTA, TIPOS_DISPOSITIVO } from '../utilidades/formato';

const COLOR_EVENTO = { EXCESO_VELOCIDAD: 'var(--color-warning)', SOS: 'var(--color-danger)', ENTRADA_GEOCERCA: 'var(--color-info)', SALIDA_GEOCERCA: 'var(--color-info)' };

/** Consulta de recorrido en un rango de fechas (HU-16). */
export default function Recorrido() {
  const [params, setParams] = useSearchParams();
  const [dispositivos, setDispositivos] = useState([]);
  const ahora = new Date();
  const [form, setForm] = useState({
    dispositivoId: params.get('dispositivo') || '',
    desde: aInputLocal(new Date(ahora.getTime() - 24 * 3600000)),
    hasta: aInputLocal(ahora),
  });
  const [recorrido, setRecorrido] = useState(null);
  const [error, setError] = useState(null);
  const [cargando, setCargando] = useState(false);

  useEffect(() => {
    api.get('/api/dispositivos').then((l) => {
      setDispositivos(l);
      setForm((f) => (f.dispositivoId ? f : { ...f, dispositivoId: (l.find((d) => d.tipo === 'GPS' && d.activoId) || l[0])?.id || '' }));
    }).catch(setError);
  }, []);

  const consultar = async (e) => {
    e?.preventDefault();
    setError(null);
    setRecorrido(null);
    if (!form.dispositivoId) return;
    setCargando(true);
    try {
      const r = await api.get(`/api/monitoreo/dispositivos/${form.dispositivoId}/recorrido?desde=${deInputLocal(form.desde)}&hasta=${deInputLocal(form.hasta)}`);
      setRecorrido(r);
      setParams({ dispositivo: form.dispositivoId });
    } catch (err) {
      setError(err);
    } finally {
      setCargando(false);
    }
  };

  // Consulta automática al abrir con un dispositivo.
  useEffect(() => { if (form.dispositivoId) consultar(); /* eslint-disable-next-line */ }, [form.dispositivoId]);

  const linea = useMemo(() => recorrido?.puntos.map((p) => [p.latitud, p.longitud]) || [], [recorrido]);
  const duracionMin = recorrido?.hayDatos ? Math.round((new Date(recorrido.finReal) - new Date(recorrido.inicioReal)) / 60000) : 0;
  const disp = dispositivos.find((d) => d.id === form.dispositivoId);
  const rangoInvalido = error?.message?.startsWith('Rango inválido');

  return (
    <Pagina titulo="Recorrido" subtitulo="Trayectoria de un dispositivo en un rango de fechas" mapa>
      <div className="pg-11" style={{ height: '100%' }}>
        <div className="rec" style={{ height: '100%' }}>
          <aside className="rec__lado">
            <form className="rec__sec" onSubmit={consultar}>
              <h3>Consulta</h3>
              {error && <AvisoError error={error} />}
              <div className="field"><label>Dispositivo</label>
                <select className="select" value={form.dispositivoId} onChange={(e) => setForm({ ...form, dispositivoId: e.target.value })}>
                  {dispositivos.map((d) => <option key={d.id} value={d.id}>{TIPOS_DISPOSITIVO[d.tipo]} {d.serial}{d.activoEtiqueta ? ` · ${d.activoEtiqueta}` : ''}</option>)}
                </select>
              </div>
              <div className="field"><label>Desde</label><input type="datetime-local" className={`input mono${rangoInvalido ? ' is-error' : ''}`} value={form.desde} onChange={(e) => setForm({ ...form, desde: e.target.value })} /></div>
              <div className="field"><label>Hasta</label><input type="datetime-local" className={`input mono${rangoInvalido ? ' is-error' : ''}`} value={form.hasta} onChange={(e) => setForm({ ...form, hasta: e.target.value })} /></div>
              <button className="btn btn--primary" disabled={cargando}><Search />{cargando ? 'Consultando…' : 'Consultar recorrido'}</button>
            </form>
            {recorrido?.hayDatos && (<>
              <div className="rec__sec">
                <h3>Resumen del recorrido</h3>
                <div className="readout" style={{ '--cols': 2 }}>
                  <div className="readout__cell"><div className="readout__label"><Route />Distancia</div><div className="readout__value">{numero(recorrido.distanciaKm, 1)}<span className="unit">km</span></div></div>
                  <div className="readout__cell"><div className="readout__label"><Timer />Duración</div><div className="readout__value">{duracionMin >= 60 ? `${Math.floor(duracionMin / 60)} h ${duracionMin % 60}` : duracionMin}<span className="unit">min</span></div></div>
                </div>
                <dl className="dl">
                  <dt>Velocidad promedio</dt><dd className="mono">{recorrido.velocidadPromedio != null ? `${numero(recorrido.velocidadPromedio)} km/h` : '—'}</dd>
                  <dt>Velocidad máxima</dt><dd className="mono">{recorrido.velocidadMaxima != null ? `${numero(recorrido.velocidadMaxima)} km/h` : '—'}</dd>
                  <dt>Posiciones registradas</dt><dd className="mono">{recorrido.puntos.length}</dd>
                </dl>
              </div>
              <div className="rec__sec">
                <h3>Eventos en el recorrido</h3>
                <ul className="eventos">
                  <li><span className="h">{hora(recorrido.inicioReal)}</span><span className="p" style={{ background: 'var(--color-success)' }} /><div><b>Inicio</b><span>{fechaHora(recorrido.inicioReal)}</span></div></li>
                  {recorrido.eventos.map((a) => (
                    <li key={a.id}><span className="h">{hora(a.fechaHora)}</span><span className="p" style={{ background: COLOR_EVENTO[a.tipo] }} /><div><b>{TIPOS_ALERTA[a.tipo].texto}</b><span>{a.mensaje}</span></div></li>
                  ))}
                  <li><span className="h">{hora(recorrido.finReal)}</span><span className="p" style={{ background: 'var(--color-danger)' }} /><div><b>Fin</b><span>{fechaHora(recorrido.finReal)}</span></div></li>
                </ul>
              </div>
            </>)}
          </aside>
          <div className="rec__mapa">
            <Mapa centro={BOGOTA} zoom={13}>
              {linea.length > 0 && (<>
                <Encuadre puntos={linea} dependencia={recorrido} />
                <Polyline positions={linea} pathOptions={{ color: '#fff', weight: 9, opacity: 0.95, lineJoin: 'round', lineCap: 'round' }} />
                <Polyline positions={linea} pathOptions={{ color: '#0E7C66', weight: 5, lineJoin: 'round', lineCap: 'round' }} />
                <Punto posicion={linea[0]} clase="mk-dot mk-dot--start" />
                <Punto posicion={linea[linea.length - 1]} clase="mk-dot mk-dot--end" />
                <Etiqueta posicion={linea[0]} html={`Inicio<b>${hora(recorrido.inicioReal)}</b>`} />
                <Etiqueta posicion={linea[linea.length - 1]} html={`Fin<b>${hora(recorrido.finReal)}</b>`} />
                {recorrido.eventos.filter((a) => a.latitud != null).map((a) => (
                  <Marker key={a.id} position={[a.latitud, a.longitud]} icon={L.divIcon({ className: '', html: `<div class="mk-evento" title="${TIPOS_ALERTA[a.tipo].texto}"></div>`, iconSize: [26, 26], iconAnchor: [13, 13] })} />
                ))}
              </>)}
            </Mapa>
            {recorrido && !recorrido.hayDatos && (
              <div className="vacio-mapa"><div className="empty">
                <MarcaVacia />
                <h3>No hay datos en este rango</h3>
                <p>{disp ? `${TIPOS_DISPOSITIVO[disp.tipo]} ${disp.serial}` : 'El dispositivo'} no registró posiciones entre el {fechaHora(recorrido.desde)} y el {fechaHora(recorrido.hasta)}, así que no hay ruta que mostrar. Prueba con otras fechas.</p>
                <span className="badge"><CalendarRange />Cambia el rango en el panel izquierdo</span>
              </div></div>
            )}
          </div>
        </div>
      </div>
    </Pagina>
  );
}

function Etiqueta({ posicion, html }) {
  return <Marker position={posicion} interactive={false} icon={L.divIcon({ className: '', html: `<span class="etq">${html}</span>`, iconSize: [110, 22], iconAnchor: [-12, 11] })} />;
}

